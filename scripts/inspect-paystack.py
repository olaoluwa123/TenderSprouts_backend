from pathlib import Path

env = Path("/home/solaolu/supersoft/tender/.env")
keys = {}
if env.exists():
    for line in env.read_text(encoding="utf-8", errors="replace").splitlines():
        if not line or line.startswith("#") or "=" not in line:
            continue
        k, v = line.split("=", 1)
        keys[k] = v
print("=== PAYSTACK ENV (presence only) ===")
for k in ("PAYSTACK_ENABLED", "PAYSTACK_SECRET_KEY", "PAYSTACK_PUBLIC_KEY"):
    v = keys.get(k, "")
    if k.endswith("_KEY"):
        prefix = v.strip()[:8] if v.strip() else "-"
        print(f"{k} set={bool(v.strip())} prefix={prefix} len={len(v.strip())}")
    else:
        print(f"{k}={v or '(missing)'}")
