DELETE pi FROM payment_invoices pi
JOIN payments p ON p.id = pi.payment_id
WHERE p.gateway_reference LIKE 'TSP-WEBHOOK-PROBE%';
DELETE FROM payments WHERE gateway_reference LIKE 'TSP-WEBHOOK-PROBE%';
DELETE FROM invoices WHERE fee_structure_id IN (SELECT id FROM (SELECT id FROM fee_structures WHERE name = 'WEBHOOK_PROBE') t);
DELETE FROM fee_structures WHERE name = 'WEBHOOK_PROBE';

INSERT INTO fee_structures (name, class_id, session_id, term_id, amount, description, is_active, created_at)
VALUES ('WEBHOOK_PROBE', 1, 1, 1, 1.00, 'webhook probe', 1, NOW());
INSERT INTO invoices (student_id, fee_structure_id, amount, due_date, status, created_at)
VALUES (1, LAST_INSERT_ID(), 1.00, CURDATE(), 'PENDING', NOW());
INSERT INTO payments (invoice_id, amount, status, gateway_reference, created_at)
VALUES (LAST_INSERT_ID(), 1.00, 'PENDING', 'TSP-WEBHOOK-PROBE', NOW());
INSERT INTO payments (invoice_id, amount, status, gateway_reference, created_at)
SELECT invoice_id, 1.00, 'PENDING', 'TSP-WEBHOOK-PROBE-AMT', NOW()
FROM payments WHERE gateway_reference = 'TSP-WEBHOOK-PROBE';
INSERT INTO payment_invoices (payment_id, invoice_id)
SELECT p.id, p.invoice_id FROM payments p WHERE p.gateway_reference LIKE 'TSP-WEBHOOK-PROBE%';
SELECT p.id AS payment_id, p.status, p.gateway_reference, i.id AS invoice_id, i.status AS invoice_status, i.amount
FROM payments p
JOIN invoices i ON i.id = p.invoice_id
WHERE p.gateway_reference LIKE 'TSP-WEBHOOK-PROBE%';
