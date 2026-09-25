ALTER TABLE assessment_sheets
    ADD COLUMN published_at TIMESTAMP NULL;

ALTER TABLE behavioural_reports
    ADD COLUMN published_at TIMESTAMP NULL;
