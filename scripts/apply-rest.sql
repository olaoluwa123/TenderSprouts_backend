ALTER TABLE students ADD COLUMN height VARCHAR(50) NULL;
ALTER TABLE students ADD COLUMN weight VARCHAR(50) NULL;
ALTER TABLE parents ADD COLUMN address TEXT NULL;

CREATE TABLE IF NOT EXISTS csv_import_jobs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    type VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL,
    actor_user_id BIGINT NOT NULL,
    original_filename VARCHAR(255) NULL,
    file_bytes BLOB NOT NULL,
    total_rows INT NOT NULL DEFAULT 0,
    success_count INT NOT NULL DEFAULT 0,
    failure_count INT NOT NULL DEFAULT 0,
    results_json TEXT NULL,
    error_message TEXT NULL,
    started_at TIMESTAMP NULL,
    completed_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    CONSTRAINT fk_csv_import_jobs_actor FOREIGN KEY (actor_user_id) REFERENCES users (id) ON DELETE RESTRICT
);
CREATE INDEX idx_csv_import_jobs_status ON csv_import_jobs (status);

ALTER TABLE classes ADD COLUMN class_group VARCHAR(20) NOT NULL DEFAULT 'PRIMARY';

ALTER TABLE assessment_sheets ADD COLUMN published_at TIMESTAMP NULL;
ALTER TABLE behavioural_reports ADD COLUMN published_at TIMESTAMP NULL;

INSERT IGNORE INTO flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success)
VALUES
(84, '25', 'student height weight', 'SQL', 'V25__student_height_weight.sql', -1347081670, 'school', NOW(), 10, 1),
(85, '26', 'parent address', 'SQL', 'V26__parent_address.sql', 1514963923, 'school', NOW(), 10, 1),
(86, '27', 'csv import jobs', 'SQL', 'V27__csv_import_jobs.sql', -781920329, 'school', NOW(), 10, 1),
(87, '29', 'class group', 'SQL', 'V29__class_group.sql', -159647882, 'school', NOW(), 10, 1),
(88, '32', 'teacher publish reports', 'SQL', 'V32__teacher_publish_reports.sql', -240292491, 'school', NOW(), 10, 1);
