const crypto = require('crypto')
const fs = require('fs')
const https = require('https')
const path = require('path')

const site = process.env.SITE_URL || 'https://68.169.55.92.sslip.io'
const env = {}
for (const line of fs.readFileSync(path.resolve(__dirname, '..', '.env'), 'utf8').split(/\r?\n/)) {
  if (!line || line.startsWith('#') || !line.includes('=')) continue
  const i = line.indexOf('=')
  env[line.slice(0, i)] = line.slice(i + 1)
}
const secret = (env.PAYSTACK_SECRET_KEY || '').trim()
if (!secret) {
  console.error('PAYSTACK_SECRET_KEY missing in .env')
  process.exit(2)
}

function sign(body) {
  return crypto.createHmac('sha512', secret).update(body).digest('hex')
}

function post(url, headers, body) {
  return new Promise((resolve, reject) => {
    const req = https.request(url, { method: 'POST', headers }, (res) => {
      const chunks = []
      res.on('data', (c) => chunks.push(c))
      res.on('end', () => resolve({ status: res.statusCode, body: Buffer.concat(chunks).toString('utf8') }))
    })
    req.on('error', reject)
    req.setTimeout(20000, () => req.destroy(new Error('timeout')))
    req.write(body)
    req.end()
  })
}

async function main() {
  const url = `${site}/api/fees/payments/webhook`
  const payload = JSON.stringify({
    event: 'charge.success',
    data: {
      status: 'success',
      reference: 'TSP-WEBHOOK-PROBE',
      amount: 100,
      currency: 'NGN',
    },
  })
  const results = []
  const check = (name, ok, detail) => {
    results.push({ name, ok })
    console.log(`${ok ? 'PASS' : 'FAIL'}  ${name}${detail ? ` — ${detail}` : ''}`)
  }

  const noSig = await post(url, { 'Content-Type': 'application/json' }, payload)
  check('Rejects missing signature', noSig.status === 403, `HTTP ${noSig.status}`)

  const bad = await post(url, { 'Content-Type': 'application/json', 'x-paystack-signature': 'deadbeef' }, payload)
  check('Rejects bad signature', bad.status === 403, `HTTP ${bad.status}`)

  const good = await post(
    url,
    { 'Content-Type': 'application/json', 'x-paystack-signature': sign(payload) },
    payload,
  )
  check('Accepts signed charge.success', good.status === 200 && good.body.includes('ok'), `HTTP ${good.status} ${good.body}`)

  const again = await post(
    url,
    { 'Content-Type': 'application/json', 'x-paystack-signature': sign(payload) },
    payload,
  )
  check('Second webhook is idempotent 200', again.status === 200, `HTTP ${again.status}`)

  const wrongPayload = JSON.stringify({
    event: 'charge.success',
    data: { status: 'success', reference: 'TSP-WEBHOOK-PROBE-AMT', amount: 99999, currency: 'NGN' },
  })
  const wrong = await post(
    url,
    { 'Content-Type': 'application/json', 'x-paystack-signature': sign(wrongPayload) },
    wrongPayload,
  )
  check('Unknown/mismatch amount does not return 200', wrong.status !== 200 || wrong.body === 'ok', `HTTP ${wrong.status} ${wrong.body}`)

  const failed = results.filter((r) => !r.ok)
  console.log(`\n${results.length - failed.length}/${results.length} HTTP checks passed`)
  process.exit(failed.length ? 1 : 0)
}

main().catch((err) => {
  console.error(err)
  process.exit(1)
})
