-- Multiple fee line-items per class per term (tuition, PTA, books, etc.)
ALTER TABLE fee_structures ADD COLUMN term_id BIGINT NULL;

ALTER TABLE fee_structures
    ADD CONSTRAINT fk_fee_structures_term
    FOREIGN KEY (term_id) REFERENCES terms (id) ON DELETE RESTRICT;

DROP INDEX uk_fee_structures_class_session ON fee_structures;

CREATE UNIQUE INDEX uk_fee_structures_class_term_name
    ON fee_structures (class_id, term_id, name);

CREATE INDEX idx_fee_structures_class_term ON fee_structures (class_id, term_id);
CREATE INDEX idx_invoices_student_status ON invoices (student_id, status);
