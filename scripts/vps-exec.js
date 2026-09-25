const { Client } = require('ssh2')
const host = process.env.VPS_HOST
const username = process.env.VPS_USER
const password = process.env.VPS_PASS
const command = process.argv.slice(2).join(' ')
const conn = new Client()
conn.on('ready', () => {
  conn.exec(command, { pty: true }, (err, stream) => {
    if (err) throw err
    stream.on('data', (d) => {
      const text = d.toString()
      process.stdout.write(text)
      if (/\[sudo\] password/i.test(text) || /password for/i.test(text)) stream.write(`${password}\n`)
    })
    stream.stderr.on('data', (d) => process.stderr.write(d))
    stream.on('close', (code) => {
      conn.end()
      process.exit(code || 0)
    })
  })
})
conn.connect({ host, username, password, hostVerifier: () => true })
