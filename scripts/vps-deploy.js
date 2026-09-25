const { Client } = require('ssh2')
const fs = require('fs')
const path = require('path')
const { loadEnv, requireEnv } = require('./load-env')

loadEnv()
requireEnv(['JWT_SECRET', 'MYSQL_ROOT_PASSWORD', 'DB_PASSWORD', 'DB_USERNAME'])
const mysqlRootPassword = process.env.MYSQL_ROOT_PASSWORD
const dbPassword = process.env.DB_PASSWORD
const dbUsername = process.env.DB_USERNAME
const host = process.env.VPS_HOST
const username = process.env.VPS_USER
const password = process.env.VPS_PASS
const localTar = process.env.VPS_TAR
const remoteDir = '~/supersoft/tender'

if (!host || !username || !password || !localTar) {
  console.error('VPS_HOST, VPS_USER, VPS_PASS, and VPS_TAR are required')
  process.exit(1)
}

function exec(conn, command, timeoutMs = 120000) {
  return new Promise((resolve, reject) => {
    conn.exec(command, { pty: true }, (err, stream) => {
      if (err) return reject(err)
      let stdout = ''
      let stderr = ''
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
      stream.stderr.on('data', (data) => {
        const text = data.toString()
        stderr += text
        process.stderr.write(text)
      })
      stream.on('close', (code) => {
        clearTimeout(timer)
        if (code === 0) resolve(stdout)
        else reject(new Error(`Exit ${code}: ${command}\n${stderr || stdout}`))
      })
    })
  })
}

async function upload(conn, localFile, remoteFile) {
  return new Promise((resolve, reject) => {
    conn.sftp((err, sftp) => {
      if (err) return reject(err)
      sftp.fastPut(localFile, remoteFile, (putErr) => {
        sftp.end()
        if (putErr) reject(putErr)
        else resolve()
      })
    })
  })
}

const conn = new Client()
conn.on('ready', async () => {
  try {
    await exec(conn, 'mkdir -p $HOME/supersoft/tender $HOME/supersoft/tmp')
    console.log('\nUploading archive...')
    await upload(conn, localTar, '/tmp/tender-deploy.tgz')
    await exec(conn, 'mkdir -p $HOME/supersoft/tender && tar -xzf /tmp/tender-deploy.tgz -C $HOME/supersoft/tender && rm -f /tmp/tender-deploy.tgz')

    console.log('\nEnsuring Docker is installed...')
    await exec(conn, `
set -e
if ! command -v docker >/dev/null 2>&1; then
  curl -fsSL https://get.docker.com | sudo -S sh
  sudo -S usermod -aG docker ${username} || true
fi
sudo -S docker compose version >/dev/null
`.trim(), 300000)

    const portOut = await exec(conn, `
set +e
if command -v python3 >/dev/null 2>&1; then
  python3 - <<'PY'
import socket
candidates = [80, 8088, 8089, 3000, 8888, 8090, 9080, 18080]
for p in candidates:
    s = socket.socket()
    s.settimeout(0.3)
    try:
        s.bind(("0.0.0.0", p))
        s.close()
        print(p)
        break
    except OSError:
        s.close()
PY
else
  for p in 80 8088 8089 3000 8888 8090 9080 18080; do
    if command -v ss >/dev/null 2>&1; then
      ss -lnt | awk '{print $4}' | grep -qE "[:.]$p\$" && continue
    fi
    echo "$p"
    break
  done
fi
`.trim())
    const port = (portOut.match(/\b(\d{2,5})\b/) || [])[1]
    if (!port) throw new Error('Could not find a free host port')
    console.log(`\nUsing FRONTEND_HOST_PORT=${port}`)

    console.log('\nKeeping existing server .env if present; writing secrets from local .env only when missing...')
    await exec(conn, `
set -e
ENV_FILE=$HOME/supersoft/tender/.env
if [ -f "$ENV_FILE" ]; then
  echo "server .env already exists; not overwriting secrets"
  grep -q '^FRONTEND_HOST_PORT=' "$ENV_FILE" || echo "FRONTEND_HOST_PORT=${port}" >> "$ENV_FILE"
else
  cat > "$ENV_FILE" <<'EOF'
FRONTEND_HOST_PORT=${port}
FRONTEND_ORIGIN=http://${host}:${port}
REQUIRE_HTTPS=false
SEED_ENABLED=false
JWT_SECRET=${process.env.JWT_SECRET}
MYSQL_ROOT_PASSWORD=${process.env.MYSQL_ROOT_PASSWORD}
DB_USERNAME=${dbUsername}
DB_PASSWORD=${process.env.DB_PASSWORD}
RABBITMQ_USER=${process.env.RABBITMQ_USER || 'school'}
RABBITMQ_PASSWORD=${process.env.RABBITMQ_PASSWORD || ''}
EOF
fi
`.trim())

    console.log('\nBuilding and starting MySQL/RabbitMQ...')
    await exec(conn, 'cd $HOME/supersoft/tender && sudo -S docker compose up -d mysql rabbitmq', 180000)
    await exec(conn, `
set -e
cd $HOME/supersoft/tender
for i in $(seq 1 60); do
  if sudo -S docker compose exec -T mysql mysqladmin ping -h 127.0.0.1 -uroot -p${mysqlRootPassword} --silent; then
    exit 0
  fi
  sleep 3
done
exit 1
`.trim(), 240000)

    console.log('\nBuilding backend (Flyway schema)...')
    await exec(conn, 'cd $HOME/supersoft/tender && sudo -S docker compose up -d --build backend', 900000)
    await exec(conn, `
set -e
cd $HOME/supersoft/tender
for i in $(seq 1 80); do
  if sudo -S docker compose exec -T backend wget -qO- http://127.0.0.1:8080/actuator/health | grep -q UP; then
    exit 0
  fi
  sleep 5
done
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
    exit 0
  fi
  sleep 4
done
exit 1
`.trim(), 240000)

    console.log('\nBuilding frontend...')
    await exec(conn, 'cd $HOME/supersoft/tender && sudo -S docker compose up -d --build frontend', 900000)
    await exec(conn, 'cd $HOME/supersoft/tender && sudo -S docker compose ps', 60000)
    console.log(`\nSITE_URL=http://${host}:${port}`)
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
  keepaliveInterval: 15000,
  hostVerifier: () => true,
})
