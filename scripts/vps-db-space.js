const { Client } = require('ssh2')
const { loadEnv } = require('./load-env')

loadEnv()
const host = process.env.VPS_HOST
const username = process.env.VPS_USER
const password = process.env.VPS_PASS

const remote = `
set -e
cd $HOME/supersoft/tender
echo '=== disk ==='
sudo -S df -h /
echo '=== docker ==='
sudo -S docker system df
echo '=== mysql volume ==='
MP=$(sudo -S docker volume inspect tender_mysql_data --format '{{.Mountpoint}}')
echo "mount=$MP"
sudo -S du -sh "$MP" || true
echo '=== schema mb ==='
sudo -S docker compose exec -T mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" --silent --skip-column-names -e "SELECT ROUND(SUM(data_length + index_length)/1024/1024, 2) FROM information_schema.tables WHERE table_schema = '"'"'school_portal'"'"';"'
echo '=== top tables mb ==='
sudo -S docker compose exec -T mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" --silent --skip-column-names -e "SELECT CONCAT(table_name, \" \", ROUND((data_length + index_length)/1024/1024, 2)) FROM information_schema.tables WHERE table_schema = '"'"'school_portal'"'"' ORDER BY (data_length + index_length) DESC LIMIT 15;"'
`.trim()

const conn = new Client()
conn.on('ready', () => {
  conn.exec(remote, { pty: true }, (err, stream) => {
    if (err) throw err
    stream.on('data', (d) => {
      const text = d.toString()
      process.stdout.write(text)
      if (/\[sudo\] password/i.test(text) || /password for/i.test(text)) {
        stream.write(`${password}\n`)
      }
    })
    stream.stderr.on('data', (d) => process.stderr.write(d))
    stream.on('close', (code) => {
      conn.end()
      process.exit(code || 0)
    })
  })
})
conn.on('error', (err) => {
  console.error(err)
  process.exit(1)
})
conn.connect({
  host,
  username,
  password,
  readyTimeout: 60000,
  hostVerifier: () => true,
})
