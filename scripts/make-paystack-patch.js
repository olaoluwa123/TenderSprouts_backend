const fs = require('fs')
const path = require('path')
const envPath = path.resolve(__dirname, '..', '.env')
const env = {}
for (const line of fs.readFileSync(envPath, 'utf8').split(/\r?\n/)) {
  if (!line || line.startsWith('#') || !line.includes('=')) continue
  const i = line.indexOf('=')
  env[line.slice(0, i)] = line.slice(i + 1)
}
const secret = (env.PAYSTACK_SECRET_KEY || '').trim()
const pub = (env.PAYSTACK_PUBLIC_KEY || '').trim()
if (!secret || !pub) {
  console.error('Missing PAYSTACK keys in .env')
  process.exit(2)
}
const py = `from pathlib import Path
p = Path("/home/solaolu/supersoft/tender/.env")
text = p.read_text(encoding="utf-8", errors="replace") if p.exists() else ""
vals = {
    "PAYSTACK_ENABLED": "true",
    "PAYSTACK_SECRET_KEY": ${JSON.stringify(secret)},
    "PAYSTACK_PUBLIC_KEY": ${JSON.stringify(pub)},
}
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
print("paystack env patched")
`
fs.writeFileSync(path.join(__dirname, '.tmp-patch-paystack.py'), py)
console.log('wrote scripts/.tmp-patch-paystack.py')
