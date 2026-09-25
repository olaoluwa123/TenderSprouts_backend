SELECT p.id, p.status AS payment_status, p.paid_at, p.gateway_reference, i.status AS invoice_status
FROM payments p
JOIN invoices i ON i.id = p.invoice_id
WHERE p.gateway_reference LIKE 'TSP-WEBHOOK-PROBE%';
