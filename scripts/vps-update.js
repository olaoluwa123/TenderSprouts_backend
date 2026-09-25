const { Client } = require('ssh2')
const { loadEnv } = require('./load-env')

loadEnv()
const host = process.env.VPS_HOST
const username = process.env.VPS_USER
const password = process.env.VPS_PASS
const localTar = process.env.VPS_TAR

if (!host || !username || !password || !localTar) {
  console.error('VPS_HOST, VPS_USER, VPS_PASS, and VPS_TAR are required')
  process.exit(1)
}

function connect() {
  return new Promise((resolve, reject) => {
    const conn = new Client()
    conn.on('ready', () => resolve(conn))
    conn.on('error', reject)
    conn.connect({
      host,
      username,
      password,
      readyTimeout: 120000,
      keepaliveInterval: 5000,
      keepaliveCountMax: 60,
      hostVerifier: () => true,
    })
  })
}

async function connectWithRetry(attempts = 6) {
  let lastErr
  for (let i = 1; i <= attempts; i++) {
    try {
      console.log(`SSH connect attempt ${i}/${attempts}...`)
      return await connect()
    } catch (err) {
      lastErr = err
      console.error(`SSH connect failed: ${err.code || err.message}`)
      await new Promise((r) => setTimeout(r, 8000))
    }
  }
  throw lastErr
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

function upload(conn, localFile, remoteFile) {
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

async function main() {
  let conn = await connectWithRetry()
  try {
    await exec(conn, 'mkdir -p $HOME/supersoft/tender')
    console.log('\nUploading archive...')
    await upload(conn, localTar, '/tmp/tender-update.tgz')
    await exec(
      conn,
      'tar -xzf /tmp/tender-update.tgz -C $HOME/supersoft/tender && rm -f /tmp/tender-update.tgz',
    )

    console.log('\nStarting rebuild in background (keeps MySQL data and .env)...')
    await exec(
      conn,
      `
set -e
cd $HOME/supersoft/tender
sudo -S -v
nohup sudo -S docker compose up -d --build backend frontend caddy > /tmp/tender-rebuild.log 2>&1 < /dev/null &
echo REBUILD_STARTED
sleep 2
sudo -S docker compose ps || true
`.trim(),
      120000,
    )
    conn.end()
  } catch (err) {
    try {
      conn.end()
    } catch (_) {
      /* ignore */
    }
    throw err
  }

  console.log('\nPolling jobs and health...')
  for (let i = 1; i <= 90; i++) {
    await new Promise((r) => setTimeout(r, 15000))
    let poll
    try {
      poll = await connectWithRetry(3)
    } catch (err) {
      console.error(`poll ${i}: ssh unavailable (${err.code || err.message})`)
      continue
    }
    try {
      const status = await exec(
        poll,
        [
          'cd $HOME/supersoft/tender',
          'echo "--- compose ---"',
          'sudo -S docker compose ps',
          'echo "--- logtail ---"',
          'sudo -S tail -n 20 /tmp/tender-rebuild.log || true',
          'echo "--- health ---"',
          'sudo -S docker compose exec -T backend wget -qO- http://127.0.0.1:8080/actuator/health || true',
        ].join('; '),
        90000,
      )
      const healthy = /"status"\s*:\s*"UP"|\{"status":"UP"\}/.test(status)
      const frontendUp = /frontend/.test(status) && /Up|running/i.test(status)
      const building = /Building|package |Downloading|CACHED|exporting/i.test(status)
      console.log(`\npoll ${i}: health=${healthy} frontend=${frontendUp} building=${building}`)
      if (healthy && frontendUp && !building) {
        await exec(
          poll,
          'cd $HOME/supersoft/tender && sudo -S docker image prune -f && sudo -S docker compose ps && df -h /',
          180000,
        )
        console.log('\nSITE_URL=https://68.169.55.92.sslip.io')
        poll.end()
        return
      }
    } catch (err) {
      console.error(`poll ${i} failed: ${err.message}`)
    } finally {
      try {
        poll.end()
      } catch (_) {
        /* ignore */
      }
    }
  }
  throw new Error('Rebuild did not become healthy in time')
}

main().catch((err) => {
  console.error(err)
  process.exit(1)
})
