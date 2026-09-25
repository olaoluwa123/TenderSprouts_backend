const { Client } = require('ssh2')
const host = process.env.VPS_HOST
const username = process.env.VPS_USER
const password = process.env.VPS_PASS
const local = process.argv[2]
const remote = process.argv[3]
const conn = new Client()
conn.on('ready', () => {
  conn.sftp((err, sftp) => {
    if (err) throw err
    sftp.fastPut(require('path').resolve(local), remote, { mode: 0o644 }, (putErr) => {
      sftp.end()
      conn.end()
      if (putErr) {
        console.error(putErr)
        process.exit(1)
      }
      console.log(`uploaded ${local} -> ${remote}`)
    })
  })
})
conn.connect({ host, username, password, hostVerifier: () => true })
