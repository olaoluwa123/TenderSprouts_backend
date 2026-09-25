const { Client } = require('ssh2')
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
    await exec(conn, 'sudo -S docker builder prune -af || true', 120000)
    await exec(conn, 'df -h /')
    await exec(conn, 'cd $HOME/supersoft/tender && sudo -S docker compose build frontend && sudo -S docker compose up -d frontend', 900000)
    await exec(conn, 'sudo -S docker builder prune -af || true', 120000)
    await exec(conn, 'cd $HOME/supersoft/tender && sudo -S docker compose ps')
    await exec(conn, 'curl -sI http://127.0.0.1:8088 | head -15')
    console.log('SITE_URL=http://68.169.55.92:8088')
    conn.end()
  } catch (err) {
    console.error(err)
    conn.end()
    process.exit(1)
  }
})
conn.connect({ host, username, password, readyTimeout: 30000, keepaliveInterval: 15000, hostVerifier: () => true })
