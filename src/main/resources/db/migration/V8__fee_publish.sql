ALTER TABLE fee_structures ADD COLUMN due_date DATE NULL;
ALTER TABLE fee_structures ADD COLUMN published_at TIMESTAMP NULL;

ALTER TABLE fee_structures MODIFY COLUMN class_id BIGINT NOT NULL;

CREATE UNIQUE INDEX uk_fee_structures_class_session ON fee_structures (class_id, session_id);

ALTER TABLE invoices ADD CONSTRAINT uk_invoices_student_fee_structure UNIQUE (student_id, fee_structure_id);
