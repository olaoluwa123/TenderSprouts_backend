#!/bin/sh
cd /home/solaolu/supersoft/tender
touch .env
python3 - <<'PY'
from pathlib import Path
p = Path("/home/solaolu/supersoft/tender/.env")
text = p.read_text(encoding="utf-8", errors="replace")
vals = {
    "FRONTEND_ORIGIN": "https://68.169.55.92",
    "REQUIRE_HTTPS": "true",
    "SITE_HOST": "68.169.55.92",
    "ACME_EMAIL": "admin@tendersprouts.com.ng",
}
lines = text.splitlines()
keys = set()
out = []
for line in lines:
    if not line or line.startswith("#") or "=" not in line:
        out.append(line)
        continue
    k = line.split("=", 1)[0]
    if k in vals:
        out.append(f"{k}={vals[k]}")
        keys.add(k)
    else:
        out.append(line)
for k, v in vals.items():
    if k not in keys:
        out.append(f"{k}={v}")
p.write_text("\n".join(out) + "\n", encoding="utf-8")
for k in vals:
    print(next(x for x in out if x.startswith(k + "=")))
PY
wc -l Caddyfile docker-compose.yml
