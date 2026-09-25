const { Client } = require('ssh2')
const { loadEnv, requireEnv } = require('./load-env')

loadEnv()
requireEnv(['MYSQL_ROOT_PASSWORD', 'DB_PASSWORD', 'DB_USERNAME'])
const mysqlRootPassword = process.env.MYSQL_ROOT_PASSWORD
const dbPassword = process.env.DB_PASSWORD
const dbUsername = process.env.DB_USERNAME
const host = process.env.VPS_HOST
const username = process.env.VPS_USER
const password = process.env.VPS_PASS

function exec(conn, command, timeoutMs = 120000) {
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
    await exec(conn, 'cd $HOME/supersoft/tender && wc -c docker-compose.yml && grep image docker-compose.yml')
    await exec(conn, 'sudo -S docker volume rm tender_mysql_data || true')
    await exec(conn, 'cd $HOME/supersoft/tender && sudo -S docker compose up -d mysql rabbitmq', 180000)
    await exec(conn, `
set -e
cd $HOME/supersoft/tender
for i in $(seq 1 60); do
  if sudo -S docker compose exec -T mysql mysqladmin ping -h 127.0.0.1 -uroot -p${mysqlRootPassword} --silent; then
    echo MYSQL_READY
    exit 0
  fi
  sleep 3
done
sudo -S docker compose logs --tail 40 mysql
exit 1
`.trim(), 240000)

    console.log('\nBuilding backend...')
    await exec(conn, 'cd $HOME/supersoft/tender && sudo -S docker compose up -d --build backend', 900000)
    await exec(conn, `
set -e
cd $HOME/supersoft/tender
for i in $(seq 1 80); do
  if sudo -S docker compose exec -T backend wget -qO- http://127.0.0.1:8080/actuator/health | grep -q UP; then
    echo BACKEND_UP
    exit 0
  fi
  sleep 5
done
sudo -S docker compose logs --tail 80 backend
exit 1
`.trim(), 480000)

    console.log('\nImporting tender.sql...')
    await exec(conn, `cd $HOME/supersoft/tender && sudo -S docker compose exec -T mysql mysql -u${dbUsername} -p${dbPassword} school_portal < $HOME/supersoft/tender/tender.sql`, 180000)
    await exec(conn, 'cd $HOME/supersoft/tender && sudo -S docker compose restart backend', 180000)
    await exec(conn, `
set -e
cd $HOME/supersoft/tender
for i in $(seq 1 40); do
  if sudo -S docker compose exec -T backend wget -qO- http://127.0.0.1:8080/actuator/health | grep -q UP; then
    echo BACKEND_UP
    exit 0
  fi
  sleep 4
done
exit 1
`.trim(), 240000)

    await exec(conn, 'sudo -S docker builder prune -af || true', 120000)
    console.log('\nBuilding frontend...')
    await exec(conn, 'cd $HOME/supersoft/tender && sudo -S docker compose up -d --build frontend', 900000)
    await exec(conn, 'sudo -S docker builder prune -af || true', 120000)
    await exec(conn, 'cd $HOME/supersoft/tender && sudo -S docker compose ps && df -h /')
    console.log('\nSITE_URL=http://68.169.55.92:8088')
    conn.end()
  } catch (err) {
    console.error(err)
    conn.end()
    process.exit(1)
  }
})
conn.connect({ host, username, password, readyTimeout: 30000, keepaliveInterval: 15000, hostVerifier: () => true })
