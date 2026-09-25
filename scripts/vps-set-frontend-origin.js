const { Client } = require('ssh2')
const { loadEnv } = require('./load-env')

loadEnv()
const host = process.env.VPS_HOST
const username = process.env.VPS_USER
const password = process.env.VPS_PASS
const origin = process.env.NEW_FRONTEND_ORIGIN || 'https://tendersprouts.com.ng'

if (!host || !username || !password) {
  console.error('VPS_HOST, VPS_USER, and VPS_PASS are required')
  process.exit(1)
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
        process.stdout.write(text)
        if (/\[sudo\] password/i.test(text) || /password for/i.test(text)) {
          stream.write(`${password}\n`)
        }
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

const conn = new Client()
conn.on('ready', async () => {
  try {
    await exec(
      conn,
      `
set -e
cd $HOME/supersoft/tender
touch .env
if grep -q "^FRONTEND_ORIGIN=" .env; then
  sed -i "s|^FRONTEND_ORIGIN=.*|FRONTEND_ORIGIN=${origin}|" .env
else
  echo "FRONTEND_ORIGIN=${origin}" >> .env
fi
echo ENV_KEYS
grep -E "^FRONTEND_ORIGIN=" .env
sudo -S docker compose up -d backend
`.trim(),
      180000,
    )
    await exec(
      conn,
      `
set -e
cd $HOME/supersoft/tender
for i in $(seq 1 40); do
  if sudo -S docker compose exec -T backend wget -qO- http://127.0.0.1:8080/actuator/health | grep -q UP; then
    echo HEALTH_OK
    exit 0
  fi
  sleep 3
done
exit 1
`.trim(),
      180000,
    )
    conn.end()
  } catch (err) {
    console.error(err)
    conn.end()
    process.exit(1)
  }
})
conn.on('error', (err) => {
  console.error(err)
  process.exit(1)
})
conn.connect({
  host,
  username,
  password,
  readyTimeout: 60000,
  keepaliveInterval: 5000,
  hostVerifier: () => true,
})
