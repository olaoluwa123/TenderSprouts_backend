const { Client } = require('ssh2')
const { loadEnv } = require('./load-env')

loadEnv()
const host = process.env.VPS_HOST
const username = process.env.VPS_USER
const password = process.env.VPS_PASS

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
        reject(new Error(`Timeout after ${timeoutMs}ms: ${command}`))
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
        else reject(new Error(`Exit ${code}: ${command}\n${stdout}`))
      })
    })
  })
}

const conn = new Client()
conn.on('ready', async () => {
  try {
    console.log('\nBuilding backend...')
    await exec(conn, 'cd $HOME/supersoft/tender && sudo -S docker compose up -d --build backend', 900000)
    await exec(
      conn,
      `
set -e
cd $HOME/supersoft/tender
for i in $(seq 1 80); do
  if sudo -S docker compose exec -T backend wget -qO- http://127.0.0.1:8080/actuator/health | grep -q UP; then
    exit 0
  fi
  sleep 5
done
exit 1
`.trim(),
      480000,
    )
    console.log('\nBuilding frontend and refreshing Caddy...')
    await exec(conn, 'cd $HOME/supersoft/tender && sudo -S docker compose up -d --build frontend caddy', 900000)
    await exec(conn, 'cd $HOME/supersoft/tender && sudo -S docker image prune -f && sudo -S docker compose ps && df -h /', 180000)
    console.log('\nSITE_URL=https://68.169.55.92.sslip.io')
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
  readyTimeout: 30000,
  keepaliveInterval: 5000,
  keepaliveCountMax: 30,
  hostVerifier: () => true,
})
