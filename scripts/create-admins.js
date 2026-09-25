const crypto = require('crypto')
const bcrypt = require('bcryptjs')
const { Client } = require('ssh2')
const { loadEnv, requireEnv } = require('./load-env')

loadEnv()
requireEnv(['MYSQL_ROOT_PASSWORD'])
const mysqlRootPassword = process.env.MYSQL_ROOT_PASSWORD

const host = process.env.VPS_HOST
const username = process.env.VPS_USER
const password = process.env.VPS_PASS
const apiBase = (process.env.API_BASE || 'https://68.169.55.92.sslip.io').replace(/\/$/, '')
const remoteDir = process.env.VPS_DIR || '$HOME/supersoft/tender'

const BOOTSTRAP_EMAIL = 'bootstrap@tender.local'
const ACCOUNTS = [
  { email: 'aminatmohammed273@gmail.com', fullName: 'Aminat Mohammed', role: 'ADMIN' },
  { email: 'tendersproutsa@gmail.com', fullName: 'Tender Sprouts', role: 'ADMIN' },
]

if (!host || !username || !password) {
  console.error('VPS_HOST, VPS_USER, and VPS_PASS are required')
  process.exit(2)
}

function shellSingleQuote(value) {
  return `'${String(value).replace(/'/g, `'\\''`)}'`
}

function sshExec(conn, command, timeoutMs = 120000) {
  return new Promise((resolve, reject) => {
    conn.exec(command, (err, stream) => {
      if (err) return reject(err)
      let stdout = ''
      const timer = setTimeout(() => {
        stream.close()
        reject(new Error(`Timeout after ${timeoutMs}ms`))
      }, timeoutMs)
      stream.on('data', (data) => {
        const text = data.toString()
        stdout += text
        process.stdout.write(text)
      })
      stream.stderr.on('data', (data) => process.stderr.write(data))
      stream.on('close', (code) => {
        clearTimeout(timer)
        if (code === 0) resolve(stdout)
        else reject(new Error(`Exit ${code}\n${stdout}`))
      })
    })
  })
}

function runMysql(conn, sql) {
  const b64 = Buffer.from(sql, 'utf8').toString('base64')
  const remoteSql = '/tmp/tender-create-admins.sql'
  return sshExec(
    conn,
    [
      `cd ${remoteDir}`,
      `printf '%s' '${b64}' | base64 -d > ${remoteSql}`,
      `printf '%s\\n' ${shellSingleQuote(password)} | sudo -S sh -c ${shellSingleQuote(
        `docker compose exec -T mysql mysql -uroot -p${mysqlRootPassword} school_portal < ${remoteSql}`
      )}`,
      `rm -f ${remoteSql}`,
    ].join(' && ')
  )
}

async function api(path, { method = 'GET', token, body } = {}) {
  const headers = { Accept: 'application/json' }
  if (body !== undefined) headers['Content-Type'] = 'application/json'
  if (token) headers.Authorization = `Bearer ${token}`
  const response = await fetch(`${apiBase}${path}`, {
    method,
    headers,
    body: body !== undefined ? JSON.stringify(body) : undefined,
  })
  const text = await response.text()
  let json = null
  if (text) {
    try {
      json = JSON.parse(text)
    } catch {
      json = { raw: text }
    }
  }
  return { status: response.status, json }
}

async function main() {
  const bootstrapPassword = crypto.randomBytes(18).toString('base64url')
  const hash = bcrypt.hashSync(bootstrapPassword, 10)

  const conn = new Client()
  await new Promise((resolve, reject) => {
    conn.on('ready', resolve)
    conn.on('error', reject)
    conn.connect({
      host,
      username,
      password,
      readyTimeout: 30000,
      keepaliveInterval: 15000,
      hostVerifier: () => true,
    })
  })

  let cleaned = false
  const cleanup = async () => {
    if (cleaned) return
    cleaned = true
    console.log('Removing bootstrap admin...')
    await runMysql(
      conn,
      `
DELETE a FROM admins a
JOIN users u ON a.user_id = u.id
WHERE u.email = '${BOOTSTRAP_EMAIL}';
DELETE FROM users WHERE email = '${BOOTSTRAP_EMAIL}';
`
    )
  }

  try {
    console.log('Inserting bootstrap admin...')
    await runMysql(
      conn,
      `
INSERT INTO users (email, password, role, is_active, must_change_password, access_token_version)
SELECT '${BOOTSTRAP_EMAIL}', '${hash}', 'ADMIN', 1, 0, 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = '${BOOTSTRAP_EMAIL}');

UPDATE users
SET password = '${hash}', role = 'ADMIN', is_active = 1, must_change_password = 0
WHERE email = '${BOOTSTRAP_EMAIL}';

INSERT INTO admins (user_id, full_name)
SELECT u.id, 'Bootstrap'
FROM users u
WHERE u.email = '${BOOTSTRAP_EMAIL}'
  AND NOT EXISTS (SELECT 1 FROM admins a WHERE a.user_id = u.id);
`
    )

    console.log(`Logging in at ${apiBase}/api/auth/login ...`)
    const login = await api('/api/auth/login', {
      method: 'POST',
      body: { email: BOOTSTRAP_EMAIL, password: bootstrapPassword },
    })
    if (login.status !== 200 || !login.json?.accessToken) {
      throw new Error(`Login failed (${login.status}): ${JSON.stringify(login.json)}`)
    }
    const token = login.json.accessToken

    for (const account of ACCOUNTS) {
      console.log(`Creating ${account.fullName} <${account.email}> ...`)
      const created = await api('/api/users', {
        method: 'POST',
        token,
        body: account,
      })
      if (created.status === 201) {
        console.log(
          `created id=${created.json.id} email=${created.json.email} role=${created.json.role} mustChangePassword=${created.json.mustChangePassword}`
        )
      } else if (created.status === 409) {
        console.log(`already exists, skipped: ${account.email}`)
      } else {
        throw new Error(`Create failed for ${account.email} (${created.status}): ${JSON.stringify(created.json)}`)
      }
    }

    const list = await api('/api/users?size=50', { token })
    if (list.status === 200) {
      const rows = list.json.content || list.json
      const emails = Array.isArray(rows) ? rows.map((row) => row.email) : []
      console.log(`users now: ${emails.join(', ')}`)
    }

    await cleanup()
    conn.end()
    console.log('Done. Welcome emails were sent through the normal create-user path.')
    console.log(`Sign in at ${apiBase}/login with the temporary password from each inbox.`)
  } catch (err) {
    console.error(err)
    try {
      await cleanup()
    } catch (cleanupErr) {
      console.error(cleanupErr)
    }
    conn.end()
    process.exit(1)
  }
}

main()
