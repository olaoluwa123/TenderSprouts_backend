from pathlib import Path

p = Path("/home/solaolu/supersoft/tender/.env")
text = p.read_text(encoding="utf-8", errors="replace") if p.exists() else ""
keys = {}
for line in text.splitlines():
    if not line or line.startswith("#") or "=" not in line:
        continue
    k, v = line.split("=", 1)
    keys[k] = v
print("=== MAIL ENV (presence only) ===")
for k in (
    "MAIL_ENABLED",
    "MAIL_HOST",
    "MAIL_PORT",
    "MAIL_USERNAME",
    "MAIL_PASSWORD",
    "MAIL_FROM",
    "MAIL_STARTTLS",
    "MAIL_SSL",
):
    v = keys.get(k, "")
    if k in ("MAIL_PASSWORD", "MAIL_USERNAME"):
        print(f"{k} set={bool(v.strip())} len={len(v.strip())}")
    else:
        print(f"{k}={v or '(missing)'}")
