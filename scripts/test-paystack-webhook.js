const { Client } = require('ssh2')
const crypto = require('crypto')
const fs = require('fs')
const path = require('path')
const https = require('https')
const http = require('http')

const { loadEnv, requireEnv } = require('./load-env')

loadEnv()
requireEnv(['DB_PASSWORD', 'DB_USERNAME'])
const dbPassword = process.env.DB_PASSWORD
const dbUsername = process.env.DB_USERNAME
const host = process.env.VPS_HOST
const username = process.env.VPS_USER
const password = process.env.VPS_PASS
const site = process.env.SITE_URL || 'https://68.169.55.92.sslip.io'
const root = path.resolve(__dirname, '..')

function loadLocalEnv() {
  return loadEnv(path.join(root, '.env'))
}

function exec(conn, command, timeoutMs = 180000) {
  return new Promise((resolve, reject) => {
    conn.exec(command, { pty: true }, (err, stream) => {
      if (err) return reject(err)
      let stdout = ''
      const timer = setTimeout(() => {
        stream.close()
        reject(new Error(`Timeout after ${timeoutMs}ms`))
      }, timeoutMs)
      stream.on('data', (data) => {
        const text = data.toString()
        stdout += text
        if (/\[sudo\] password/i.test(text) || /password for/i.test(text)) {
          stream.write(`${password}\n`)
        }
      })
      stream.stderr.on('data', (data) => {
        stdout += data.toString()
      })
      stream.on('close', (code) => {
        clearTimeout(timer)
        if (code === 0) resolve(stdout)
        else reject(new Error(`Exit ${code}\n${stdout}`))
      })
    })
  })
}

function writeRemote(conn, remotePath, body) {
  return new Promise((resolve, reject) => {
    conn.exec(`cat > ${remotePath}`, (err, stream) => {
      if (err) return reject(err)
      stream.on('close', (code) => {
        if (code === 0) resolve()
        else reject(new Error(`write failed ${remotePath} exit ${code}`))
      })
      stream.stderr.on('data', () => {})
      stream.end(Buffer.from(body))
    })
  })
}

function sign(secret, rawBody) {
  return crypto.createHmac('sha512', secret).update(rawBody).digest('hex')
}

function request(url, { method = 'GET', headers = {}, body } = {}) {
  return new Promise((resolve, reject) => {
    const client = url.startsWith('https:') ? https : http
    const req = client.request(url, { method, headers }, (res) => {
      const chunks = []
      res.on('data', (c) => chunks.push(c))
      res.on('end', () => {
        resolve({
          status: res.statusCode,
          headers: res.headers,
          body: Buffer.concat(chunks).toString('utf8'),
        })
      })
    })
    req.on('error', reject)
    req.setTimeout(20000, () => {
      req.destroy(new Error('request timeout'))
    })
    if (body) req.write(body)
    req.end()
  })
}

async function mysql(conn, sql) {
  const remote = '/tmp/tender-webhook-q.sql'
  await writeRemote(conn, remote, sql.endsWith(';') ? sql : `${sql};`)
  const out = await exec(
    conn,
    `sudo -S docker exec -i tender-mysql-1 mysql -u${dbUsername} -p${dbPassword} --table school_portal < ${remote}`,
  )
  return out.replace(/\[sudo\] password for .+\r?\n/, '')
}

async function mysqlValue(conn, sql) {
  const out = await mysql(conn, sql)
  const lines = out.split(/\r?\n/).map((l) => l.trim()).filter(Boolean)
  const data = lines.find((l) => l.startsWith('|') && !l.includes('---') && !/^\| [a-z_]+/i.test(l.split('|')[1] || ''))
  // fallback: last numeric-looking cell
  const cells = lines
    .filter((l) => l.startsWith('|') && !l.includes('+---') && !l.includes('---'))
    .map((l) => l.split('|').map((c) => c.trim()).filter(Boolean))
  if (cells.length >= 2) return cells[1][0]
  return null
}

const results = []
function check(name, ok, detail) {
  results.push({ name, ok: Boolean(ok), detail: detail || '' })
  console.log(`${ok ? 'PASS' : 'FAIL'}  ${name}${detail ? ` — ${detail}` : ''}`)
}

const conn = new Client()
conn.on('ready', async () => {
  try {
    const localEnv = loadLocalEnv()
    const secret = (localEnv.PAYSTACK_SECRET_KEY || '').trim()
    const publicKey = (localEnv.PAYSTACK_PUBLIC_KEY || '').trim()
    if (!secret || !publicKey) {
      throw new Error('Local .env is missing PAYSTACK_SECRET_KEY / PAYSTACK_PUBLIC_KEY')
    }
    console.log(`Signing with local test key prefix ${secret.slice(0, 10)}… (len ${secret.length})`)

    await writeRemote(
      conn,
      '/tmp/patch-paystack-env.py',
      `
from pathlib import Path
p = Path("/home/solaolu/supersoft/tender/.env")
text = p.read_text(encoding="utf-8", errors="replace") if p.exists() else ""
vals = {
    "PAYSTACK_ENABLED": "true",
    "PAYSTACK_SECRET_KEY": ${JSON.stringify(secret)},
    "PAYSTACK_PUBLIC_KEY": ${JSON.stringify(publicKey)},
}
lines = text.splitlines()
seen = set()
out = []
for line in lines:
    if not line or line.startswith("#") or "=" not in line:
        out.append(line)
        continue
    k = line.split("=", 1)[0]
    if k in vals:
        out.append(f"{k}={vals[k]}")
        seen.add(k)
    else:
        out.append(line)
for k, v in vals.items():
    if k not in seen:
        out.append(f"{k}={v}")
p.write_text("\\n".join(out) + "\\n", encoding="utf-8")
print("PAYSTACK_ENABLED=true")
print("PAYSTACK_SECRET_KEY set")
print("PAYSTACK_PUBLIC_KEY set")
`,
    )
    await exec(conn, 'python3 /tmp/patch-paystack-env.py; rm -f /tmp/patch-paystack-env.py')
    await writeRemote(conn, '/home/solaolu/supersoft/tender/docker-compose.yml', fs.readFileSync(path.join(root, 'docker-compose.yml')))

    console.log('\nRecreating backend with Paystack enabled…')
    await exec(conn, 'cd /home/solaolu/supersoft/tender && sudo -S docker compose up -d --force-recreate backend', 180000)
    await exec(conn, 'cd /home/solaolu/supersoft/tender && sudo -S docker compose exec -T backend sh -c \'echo ENABLED=$PAYSTACK_ENABLED; echo SECRET_SET=$( [ -n "$PAYSTACK_SECRET_KEY" ] && echo yes || echo no )\'')

    let healthy = false
    for (let i = 0; i < 24; i += 1) {
      const probe = await request(`${site}/actuator/health`).catch(() => null)
      if (probe && probe.body.includes('"UP"')) {
        healthy = true
        break
      }
      await new Promise((r) => setTimeout(r, 5000))
    }
    check('Backend healthy over HTTPS', healthy, site)

    const studentId = await mysqlValue(conn, 'SELECT id FROM students ORDER BY id LIMIT 1')
    const classId = await mysqlValue(conn, 'SELECT id FROM classes ORDER BY id LIMIT 1')
    const sessionId = await mysqlValue(conn, 'SELECT id FROM academic_sessions ORDER BY id LIMIT 1')
    const termId = await mysqlValue(conn, 'SELECT id FROM terms ORDER BY id LIMIT 1')
    check('Found student/class/session for probe invoice', studentId && classId && sessionId, `student=${studentId} class=${classId}`)

    const feeName = `WEBHOOK_PROBE_${Date.now()}`
    await mysql(
      conn,
      `INSERT INTO fee_structures (name, class_id, session_id, term_id, amount, description, is_active, created_at)
       VALUES ('${feeName}', ${classId}, ${sessionId}, ${termId || 'NULL'}, 1.00, 'webhook probe', 1, NOW())`,
    )
    const feeId = await mysqlValue(conn, `SELECT id FROM fee_structures WHERE name='${feeName}'`)
    await mysql(
      conn,
      `INSERT INTO invoices (student_id, fee_structure_id, amount, due_date, status, created_at)
       VALUES (${studentId}, ${feeId}, 1.00, CURDATE(), 'PENDING', NOW())`,
    )
    const invoiceId = await mysqlValue(conn, `SELECT id FROM invoices WHERE fee_structure_id=${feeId}`)
    const reference = `TSP-WEBHOOK-${Date.now()}`
    await mysql(
      conn,
      `INSERT INTO payments (invoice_id, amount, status, gateway_reference, created_at)
       VALUES (${invoiceId}, 1.00, 'PENDING', '${reference}', NOW())`,
    )
    const paymentId = await mysqlValue(conn, `SELECT id FROM payments WHERE gateway_reference='${reference}'`)
    await mysql(conn, `INSERT INTO payment_invoices (payment_id, invoice_id) VALUES (${paymentId}, ${invoiceId})`)
    check('Created isolated PENDING payment', paymentId && invoiceId, `payment=${paymentId} invoice=${invoiceId} ref=${reference}`)

    const payload = JSON.stringify({
      event: 'charge.success',
      data: {
        status: 'success',
        reference,
        amount: 100,
        currency: 'NGN',
      },
    })
    const webhookUrl = `${site}/api/fees/payments/webhook`

    const noSig = await request(webhookUrl, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: payload,
    })
    check('Rejects webhook without signature', noSig.status === 403, `HTTP ${noSig.status}`)

    const badSig = await request(webhookUrl, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'x-paystack-signature': 'deadbeef',
      },
      body: payload,
    })
    check('Rejects webhook with bad signature', badSig.status === 403, `HTTP ${badSig.status}`)

    const good = await request(webhookUrl, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'x-paystack-signature': sign(secret, payload),
      },
      body: payload,
    })
    check('Accepts signed charge.success', good.status === 200 && good.body.includes('ok'), `HTTP ${good.status} body=${good.body}`)

    const after = await mysql(
      conn,
      `SELECT p.status AS payment_status, p.paid_at IS NOT NULL AS has_paid_at, i.status AS invoice_status
       FROM payments p JOIN invoices i ON i.id=${invoiceId}
       WHERE p.id=${paymentId}`,
    )
    console.log(after)
    const paid = /SUCCESS/i.test(after) && /PAID/i.test(after)
    check('Payment marked SUCCESS and invoice PAID', paid, after.replace(/\s+/g, ' ').trim())

    const again = await request(webhookUrl, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'x-paystack-signature': sign(secret, payload),
      },
      body: payload,
    })
    check('Idempotent second webhook stays 200', again.status === 200, `HTTP ${again.status}`)

    const wrongRef = `${reference}-AMT`
    await mysql(
      conn,
      `INSERT INTO payments (invoice_id, amount, status, gateway_reference, created_at)
       VALUES (${invoiceId}, 1.00, 'PENDING', '${wrongRef}', NOW())`,
    )
    const wrongId = await mysqlValue(conn, `SELECT id FROM payments WHERE gateway_reference='${wrongRef}'`)
    await mysql(conn, `INSERT INTO payment_invoices (payment_id, invoice_id) VALUES (${wrongId}, ${invoiceId})`)
    const wrongPayload = JSON.stringify({
      event: 'charge.success',
      data: { status: 'success', reference: wrongRef, amount: 99999, currency: 'NGN' },
    })
    const wrong = await request(webhookUrl, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'x-paystack-signature': sign(secret, wrongPayload),
      },
      body: wrongPayload,
    })
    check('Mismatched amount is rejected', wrong.status >= 400, `HTTP ${wrong.status}`)

    console.log('\nCleaning up probe rows…')
    await mysql(conn, `DELETE FROM payment_invoices WHERE payment_id IN (${paymentId}, ${wrongId})`)
    await mysql(conn, `DELETE FROM payments WHERE id IN (${paymentId}, ${wrongId})`)
    await mysql(conn, `DELETE FROM invoices WHERE id=${invoiceId}`)
    await mysql(conn, `DELETE FROM fee_structures WHERE id=${feeId}`)

    const failed = results.filter((r) => !r.ok)
    console.log(`\n${results.length - failed.length}/${results.length} checks passed`)
    if (failed.length) {
      conn.end()
      process.exit(1)
    }
    conn.end()
  } catch (err) {
    console.error(err)
    conn.end()
    process.exit(1)
  }
})
conn.connect({
  host,
  username,
  password,
  readyTimeout: 30000,
  keepaliveInterval: 15000,
  hostVerifier: () => true,
})
