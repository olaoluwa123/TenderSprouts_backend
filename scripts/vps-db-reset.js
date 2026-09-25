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
        if (/\[sudo\] password/i.test(text) || /password for/i.test(text)) stream.write(`${password}\n`)
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
    await exec(conn, `cd $HOME/supersoft/tender && sudo -S docker compose exec -T mysql mysql -uroot -p${mysqlRootPassword} -e "DROP DATABASE IF EXISTS school_portal; CREATE DATABASE school_portal CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci; GRANT ALL PRIVILEGES ON school_portal.* TO '${dbUsername}'@'%'; FLUSH PRIVILEGES;"`)
    await exec(conn, 'cd $HOME/supersoft/tender && sudo -S docker compose up -d backend', 120000)
    await exec(conn, `
set -e
cd $HOME/supersoft/tender
for i in $(seq 1 50); do
  status=$(sudo -S docker inspect -f '{{.State.Status}}' tender-backend-1 2>/dev/null || echo missing)
  echo "backend status=$status try=$i"
  if sudo -S docker compose exec -T backend wget -qO- http://127.0.0.1:8080/actuator/health 2>/dev/null | grep -q UP; then
    echo HEALTH_OK
    exit 0
  fi
  sleep 5
done
sudo -S docker compose logs --tail 40 backend
exit 1
`.trim(), 360000)
    console.log('Importing dump...')
    await exec(conn, `cd $HOME/supersoft/tender && sudo -S docker compose exec -T mysql mysql -u${dbUsername} -p${dbPassword} school_portal < $HOME/supersoft/tender/tender.sql`, 180000)
    await exec(conn, `cd $HOME/supersoft/tender && sudo -S docker compose exec -T mysql mysql -u${dbUsername} -p${dbPassword} -e "SELECT COUNT(*) AS users FROM school_portal.users; SELECT COUNT(*) AS students FROM school_portal.students;"`)
    await exec(conn, 'cd $HOME/supersoft/tender && sudo -S docker compose restart backend', 120000)
    await exec(conn, `
set -e
cd $HOME/supersoft/tender
for i in $(seq 1 30); do
  if sudo -S docker compose exec -T backend wget -qO- http://127.0.0.1:8080/actuator/health 2>/dev/null | grep -q UP; then
    echo HEALTH_OK
    exit 0
  fi
  sleep 4
done
exit 1
`.trim(), 180000)
    conn.end()
  } catch (err) {
    console.error(err)
    conn.end()
    process.exit(1)
  }
})
conn.connect({ host, username, password, readyTimeout: 30000, keepaliveInterval: 15000, hostVerifier: () => true })
