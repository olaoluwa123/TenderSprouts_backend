CREATE TABLE IF NOT EXISTS payment_invoices (
    payment_id BIGINT NOT NULL,
    invoice_id BIGINT NOT NULL,
    CONSTRAINT fk_payment_invoices_payment FOREIGN KEY (payment_id) REFERENCES payments (id) ON DELETE CASCADE,
    CONSTRAINT fk_payment_invoices_invoice FOREIGN KEY (invoice_id) REFERENCES invoices (id) ON DELETE RESTRICT,
    CONSTRAINT uk_payment_invoices UNIQUE (payment_id, invoice_id)
);
CREATE INDEX idx_payment_invoices_invoice ON payment_invoices (invoice_id);
INSERT INTO payment_invoices (payment_id, invoice_id)
SELECT id, invoice_id FROM payments WHERE invoice_id IS NOT NULL;
ALTER TABLE payments MODIFY invoice_id BIGINT NULL;
UPDATE flyway_schema_history SET success = 1, execution_time = 40 WHERE version = '24';
