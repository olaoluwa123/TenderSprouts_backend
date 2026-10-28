ALTER TABLE preschool_reports
    ADD COLUMN month_number INT NOT NULL DEFAULT 0;

ALTER TABLE preschool_reports
    DROP INDEX uk_preschool_report;

ALTER TABLE preschool_reports
    ADD CONSTRAINT uk_preschool_report UNIQUE (class_id, term_id, month_number);
