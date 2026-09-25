const { Client } = require('ssh2')
const fs = require('fs')
const path = require('path')
const host = process.env.VPS_HOST
const username = process.env.VPS_USER
const password = process.env.VPS_PASS
const local = path.resolve(process.argv[2])
const remote = process.argv[3]
const conn = new Client()
conn.on('ready', () => {
  const cmd = `cat > ${remote}`
  conn.exec(cmd, (err, stream) => {
    if (err) throw err
    stream.on('data', (d) => process.stdout.write(d))
    stream.stderr.on('data', (d) => process.stderr.write(d))
    stream.on('close', (code) => {
      conn.end()
      if (code === 0) console.log(`wrote ${local} -> ${remote}`)
      process.exit(code || 0)
    })
    stream.end(fs.readFileSync(local))
  })
})
conn.connect({ host, username, password, hostVerifier: () => true })
