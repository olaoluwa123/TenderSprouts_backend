const fs = require('fs')
const path = require('path')
const envPath = path.resolve(__dirname, '..', '.env')
const env = {}
for (const line of fs.readFileSync(envPath, 'utf8').split(/\r?\n/)) {
  if (!line || line.startsWith('#') || !line.includes('=')) continue
  const i = line.indexOf('=')
  env[line.slice(0, i)] = line.slice(i + 1).trim()
}
const keys = [
  'MAIL_ENABLED',
  'MAIL_HOST',
  'MAIL_PORT',
  'MAIL_USERNAME',
  'MAIL_PASSWORD',
  'MAIL_FROM',
  'MAIL_SMTP_AUTH',
  'MAIL_STARTTLS',
  'MAIL_SSL',
]
const vals = {}
for (const key of keys) {
  if (!env[key]) {
    console.error(`Missing ${key} in local .env`)
    process.exit(2)
  }
  vals[key] = env[key]
}
const py = `from pathlib import Path
p = Path("/home/solaolu/supersoft/tender/.env")
text = p.read_text(encoding="utf-8", errors="replace") if p.exists() else ""
vals = ${JSON.stringify(vals, null, 4)}
lines = text.splitlines()
seen = set()
out = []
for line in lines:
    if not line or line.startswith("#") or "=" not in line:
        out.append(line)
        continue
    k = line.split("=", 1)[0]
    if k in vals:
        out.append(f"{k}={vals[k]}")
        seen.add(k)
    else:
        out.append(line)
for k, v in vals.items():
    if k not in seen:
        out.append(f"{k}={v}")
p.write_text("\\n".join(out) + "\\n", encoding="utf-8")
print("mail env patched")
print("MAIL_ENABLED=" + vals["MAIL_ENABLED"])
print("MAIL_HOST=" + vals["MAIL_HOST"])
print("MAIL_FROM=" + vals["MAIL_FROM"])
print("MAIL_USERNAME set")
print("MAIL_PASSWORD set")
`
fs.writeFileSync(path.join(__dirname, '.tmp-patch-mail.py'), py)
console.log('wrote scripts/.tmp-patch-mail.py')
