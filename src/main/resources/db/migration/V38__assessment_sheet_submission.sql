ALTER TABLE assessment_sheets
    ADD COLUMN submitted_at TIMESTAMP NULL;

ALTER TABLE assessment_sheets
    ADD COLUMN submitted_by_teacher_id BIGINT NULL;
