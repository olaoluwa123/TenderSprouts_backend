DELETE pi FROM payment_invoices pi
JOIN payments p ON p.id = pi.payment_id
WHERE p.gateway_reference LIKE 'TSP-WEBHOOK-PROBE%';
DELETE FROM payments WHERE gateway_reference LIKE 'TSP-WEBHOOK-PROBE%';
DELETE FROM invoices WHERE fee_structure_id IN (SELECT id FROM (SELECT id FROM fee_structures WHERE name = 'WEBHOOK_PROBE') t);
DELETE FROM fee_structures WHERE name = 'WEBHOOK_PROBE';
