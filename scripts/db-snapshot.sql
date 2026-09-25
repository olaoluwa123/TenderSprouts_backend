SELECT id, email, role FROM users;
SELECT id, status, amount, student_id, fee_structure_id FROM invoices;
SELECT id, status, amount, gateway_reference, invoice_id, paid_at FROM payments;
SELECT payment_id, invoice_id FROM payment_invoices;
SELECT id FROM students LIMIT 5;
SELECT id FROM classes LIMIT 5;
SELECT id FROM academic_sessions LIMIT 5;
SELECT id FROM terms LIMIT 5;
