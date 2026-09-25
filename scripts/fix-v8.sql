ALTER TABLE fee_structures DROP FOREIGN KEY fk_fee_structures_class;
ALTER TABLE fee_structures MODIFY COLUMN class_id BIGINT NOT NULL;
ALTER TABLE fee_structures
    ADD CONSTRAINT fk_fee_structures_class
    FOREIGN KEY (class_id) REFERENCES classes (id) ON DELETE RESTRICT;
CREATE UNIQUE INDEX uk_fee_structures_class_session ON fee_structures (class_id, session_id);
ALTER TABLE invoices ADD CONSTRAINT uk_invoices_student_fee_structure UNIQUE (student_id, fee_structure_id);
UPDATE flyway_schema_history SET success = 1, execution_time = 100 WHERE version = '8';
