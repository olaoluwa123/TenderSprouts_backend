const { Client } = require('ssh2')
const fs = require('fs')
const path = require('path')

const host = process.env.VPS_HOST
const username = process.env.VPS_USER
const password = process.env.VPS_PASS
const root = path.resolve(__dirname, '..')

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

function writeRemote(conn, localPath, remotePath) {
  return new Promise((resolve, reject) => {
    const body = fs.readFileSync(localPath)
    conn.exec(`cat > ${remotePath}`, (err, stream) => {
      if (err) return reject(err)
      stream.on('close', (code) => {
        if (code === 0) {
          console.log(`wrote ${localPath} -> ${remotePath}`)
          resolve()
        } else reject(new Error(`write failed ${remotePath} exit ${code}`))
      })
      stream.stderr.on('data', (d) => process.stderr.write(d))
      stream.end(body)
    })
  })
}

const conn = new Client()
conn.on('ready', async () => {
  try {
    await writeRemote(conn, path.join(root, 'Caddyfile'), '/home/solaolu/supersoft/tender/Caddyfile')
    await writeRemote(conn, path.join(root, 'docker-compose.yml'), '/home/solaolu/supersoft/tender/docker-compose.yml')

    await exec(conn, `bash -lc '
set -e
cd "$HOME/supersoft/tender"
touch .env
grep -q "^FRONTEND_ORIGIN=" .env && sed -i "s|^FRONTEND_ORIGIN=.*|FRONTEND_ORIGIN=https://68.169.55.92|" .env || echo "FRONTEND_ORIGIN=https://68.169.55.92" >> .env
grep -q "^REQUIRE_HTTPS=" .env && sed -i "s|^REQUIRE_HTTPS=.*|REQUIRE_HTTPS=true|" .env || echo "REQUIRE_HTTPS=true" >> .env
grep -q "^SITE_HOST=" .env && sed -i "s|^SITE_HOST=.*|SITE_HOST=68.169.55.92|" .env || echo "SITE_HOST=68.169.55.92" >> .env
grep -q "^ACME_EMAIL=" .env && sed -i "s|^ACME_EMAIL=.*|ACME_EMAIL=admin@tendersprouts.com.ng|" .env || echo "ACME_EMAIL=admin@tendersprouts.com.ng" >> .env
echo ENV_KEYS
grep -E "^(FRONTEND_ORIGIN|REQUIRE_HTTPS|SITE_HOST|ACME_EMAIL)=" .env
'`)

    await exec(conn, 'sudo -S ufw status || true; sudo -S ufw allow 80/tcp || true; sudo -S ufw allow 443/tcp || true; sudo -S ufw allow 443/udp || true')
    await exec(conn, 'df -h /')
    await exec(conn, 'cd $HOME/supersoft/tender && sudo -S docker compose up -d', 300000)
    await exec(conn, 'cd $HOME/supersoft/tender && sudo -S docker compose ps')
    await exec(conn, 'sleep 8; curl -sI http://127.0.0.1/actuator/health | head -20; echo ---; curl -skI https://127.0.0.1/actuator/health | head -20')
    await exec(conn, 'cd $HOME/supersoft/tender && sudo -S docker compose logs --tail=80 caddy')
    console.log('SITE_URL=https://68.169.55.92')
    console.log('SITE_URL_ALT=https://68.169.55.92.sslip.io')
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
