ALTER TABLE payments MODIFY invoice_id BIGINT NULL;
UPDATE flyway_schema_history SET success = 1, execution_time = 40 WHERE version = '24';

ALTER TABLE assessment_sheet_cells DROP FOREIGN KEY fk_ascell_column;
ALTER TABLE assessment_sheet_cells DROP INDEX uk_assessment_cell;
ALTER TABLE assessment_sheet_cells ADD CONSTRAINT uk_assessment_cell UNIQUE (column_id, student_id, type_code);
ALTER TABLE assessment_sheet_cells ADD CONSTRAINT fk_ascell_column FOREIGN KEY (column_id) REFERENCES assessment_sheet_columns (id) ON DELETE CASCADE;

CREATE TABLE IF NOT EXISTS preschool_reports (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    class_id BIGINT NOT NULL,
    term_id BIGINT NOT NULL,
    session_id BIGINT NOT NULL,
    teacher_id BIGINT NOT NULL,
    published_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    CONSTRAINT uk_preschool_report UNIQUE (class_id, term_id),
    CONSTRAINT fk_pr_class FOREIGN KEY (class_id) REFERENCES classes (id),
    CONSTRAINT fk_pr_term FOREIGN KEY (term_id) REFERENCES terms (id),
    CONSTRAINT fk_pr_session FOREIGN KEY (session_id) REFERENCES academic_sessions (id),
    CONSTRAINT fk_pr_teacher FOREIGN KEY (teacher_id) REFERENCES teachers (id)
);
CREATE INDEX idx_pr_class_term ON preschool_reports (class_id, term_id);

CREATE TABLE IF NOT EXISTS preschool_report_comments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    report_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    subject_id BIGINT NOT NULL,
    subject_name VARCHAR(150) NOT NULL,
    comment TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    CONSTRAINT uk_preschool_comment UNIQUE (report_id, student_id, subject_id),
    CONSTRAINT fk_prc_report FOREIGN KEY (report_id) REFERENCES preschool_reports (id) ON DELETE CASCADE,
    CONSTRAINT fk_prc_student FOREIGN KEY (student_id) REFERENCES students (id),
    CONSTRAINT fk_prc_subject FOREIGN KEY (subject_id) REFERENCES subjects (id)
);
CREATE INDEX idx_prc_report_student ON preschool_report_comments (report_id, student_id);

CREATE TABLE IF NOT EXISTS preschool_report_summaries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    report_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    class_teacher_comment TEXT,
    head_teacher_remark TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    CONSTRAINT uk_preschool_summary UNIQUE (report_id, student_id),
    CONSTRAINT fk_prs_report FOREIGN KEY (report_id) REFERENCES preschool_reports (id) ON DELETE CASCADE,
    CONSTRAINT fk_prs_student FOREIGN KEY (student_id) REFERENCES students (id)
);
CREATE INDEX idx_prs_report_student ON preschool_report_summaries (report_id, student_id);

INSERT IGNORE INTO flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success)
VALUES
(80, '28', 'behavioural and weekly tests', 'SQL', 'V28__behavioural_and_weekly_tests.sql', -1996733888, 'school', NOW(), 20, 1),
(81, '30', 'assessment sheets', 'SQL', 'V30__assessment_sheets.sql', 1555149002, 'school', NOW(), 20, 1),
(82, '31', 'assessment sheet types', 'SQL', 'V31__assessment_sheet_types.sql', -1109775315, 'school', NOW(), 20, 1),
(83, '33', 'preschool reports', 'SQL', 'V33__preschool_reports.sql', -1754373474, 'school', NOW(), 20, 1);
