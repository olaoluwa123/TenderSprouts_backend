#!/bin/sh
cd /home/solaolu/supersoft/tender
echo '=== PAYSTACK ENV (presence only) ==='
python3 - <<'PY'
from pathlib import Path
text = Path('.env').read_text(encoding='utf-8', errors='replace')
keys = {}
for line in text.splitlines():
    if not line or line.startswith('#') or '=' not in line:
        continue
    k, v = line.split('=', 1)
    keys[k] = v
for k in ('PAYSTACK_ENABLED', 'PAYSTACK_SECRET_KEY', 'PAYSTACK_PUBLIC_KEY'):
    v = keys.get(k, '')
    if k.endswith('_KEY'):
        print(f'{k} set={bool(v.strip())} prefix={v[:8] if v.strip() else "-"} len={len(v.strip())}')
    else:
        print(f'{k}={v or "(missing)"}')
PY
echo '=== BACKEND CONTAINER ENV ==='
sudo -S docker exec tender-backend-1 sh -c 'echo ENABLED=$PAYSTACK_ENABLED; echo SECRET_SET=$( [ -n "$PAYSTACK_SECRET_KEY" ] && echo yes || echo no ); echo PUBLIC_SET=$( [ -n "$PAYSTACK_PUBLIC_KEY" ] && echo yes || echo no )'
echo '=== DB COUNTS ==='
sudo -S docker exec tender-mysql-1 mysql -uschool -schoolpass school_portal -e "SELECT COUNT(*) users FROM users; SELECT id,email,role FROM users; SELECT id,status,amount,student_id FROM invoices; SELECT id,status,amount,gateway_reference,invoice_id,paid_at FROM payments; SELECT * FROM payment_invoices; SELECT id,name FROM fee_structures LIMIT 10; SELECT id,first_name,last_name FROM students;"
