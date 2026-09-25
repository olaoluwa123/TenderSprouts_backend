ALTER TABLE fee_structures DROP FOREIGN KEY fk_fee_structures_class;
DROP INDEX uk_fee_structures_class_session ON fee_structures;
ALTER TABLE fee_structures
    ADD CONSTRAINT fk_fee_structures_class
    FOREIGN KEY (class_id) REFERENCES classes (id) ON DELETE RESTRICT;
CREATE UNIQUE INDEX uk_fee_structures_class_term_name
    ON fee_structures (class_id, term_id, name);
CREATE INDEX idx_fee_structures_class_term ON fee_structures (class_id, term_id);
CREATE INDEX idx_invoices_student_status ON invoices (student_id, status);
UPDATE flyway_schema_history SET success = 1, execution_time = 50 WHERE version = '19';
