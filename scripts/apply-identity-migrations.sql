CREATE TABLE IF NOT EXISTS behavioural_reports (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    class_id BIGINT NOT NULL,
    teacher_id BIGINT NOT NULL,
    session_id BIGINT NOT NULL,
    term_id BIGINT NOT NULL,
    period_type VARCHAR(20) NOT NULL,
    week_number INT NOT NULL DEFAULT 0,
    punctuality VARCHAR(30),
    neatness VARCHAR(30),
    cooperation VARCHAR(30),
    responsibility VARCHAR(30),
    conduct VARCHAR(30),
    remarks VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    CONSTRAINT uk_behavioural_report UNIQUE (student_id, term_id, period_type, week_number),
    CONSTRAINT fk_br_student FOREIGN KEY (student_id) REFERENCES students (id),
    CONSTRAINT fk_br_class FOREIGN KEY (class_id) REFERENCES classes (id),
    CONSTRAINT fk_br_teacher FOREIGN KEY (teacher_id) REFERENCES teachers (id),
    CONSTRAINT fk_br_session FOREIGN KEY (session_id) REFERENCES academic_sessions (id),
    CONSTRAINT fk_br_term FOREIGN KEY (term_id) REFERENCES terms (id)
);
CREATE INDEX idx_br_class_term_period ON behavioural_reports (class_id, term_id, period_type, week_number);

CREATE TABLE IF NOT EXISTS weekly_test_results (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    subject_id BIGINT NOT NULL,
    class_id BIGINT NOT NULL,
    teacher_id BIGINT NOT NULL,
    session_id BIGINT NOT NULL,
    term_id BIGINT NOT NULL,
    week_number INT NOT NULL,
    score DECIMAL(5, 2),
    max_score DECIMAL(5, 2) NOT NULL DEFAULT 100,
    remarks VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    CONSTRAINT uk_weekly_test_result UNIQUE (student_id, subject_id, term_id, week_number),
    CONSTRAINT fk_wtr_student FOREIGN KEY (student_id) REFERENCES students (id),
    CONSTRAINT fk_wtr_subject FOREIGN KEY (subject_id) REFERENCES subjects (id),
    CONSTRAINT fk_wtr_class FOREIGN KEY (class_id) REFERENCES classes (id),
    CONSTRAINT fk_wtr_teacher FOREIGN KEY (teacher_id) REFERENCES teachers (id),
    CONSTRAINT fk_wtr_session FOREIGN KEY (session_id) REFERENCES academic_sessions (id),
    CONSTRAINT fk_wtr_term FOREIGN KEY (term_id) REFERENCES terms (id)
);
CREATE INDEX idx_wtr_class_term_week ON weekly_test_results (class_id, term_id, week_number);

DROP TABLE IF EXISTS weekly_test_results;

CREATE TABLE IF NOT EXISTS assessment_sheets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_type VARCHAR(20) NOT NULL,
    class_id BIGINT NOT NULL,
    term_id BIGINT NOT NULL,
    session_id BIGINT NOT NULL,
    teacher_id BIGINT NOT NULL,
    week_number INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    CONSTRAINT uk_assessment_sheet UNIQUE (sheet_type, class_id, term_id, week_number),
    CONSTRAINT fk_as_class FOREIGN KEY (class_id) REFERENCES classes (id),
    CONSTRAINT fk_as_term FOREIGN KEY (term_id) REFERENCES terms (id),
    CONSTRAINT fk_as_session FOREIGN KEY (session_id) REFERENCES academic_sessions (id),
    CONSTRAINT fk_as_teacher FOREIGN KEY (teacher_id) REFERENCES teachers (id)
);
CREATE INDEX idx_as_class_term_type ON assessment_sheets (class_id, term_id, sheet_type);

CREATE TABLE IF NOT EXISTS assessment_sheet_columns (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    max_score DECIMAL(5, 2) NOT NULL DEFAULT 100,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    CONSTRAINT fk_asc_sheet FOREIGN KEY (sheet_id) REFERENCES assessment_sheets (id) ON DELETE CASCADE
);
CREATE INDEX idx_asc_sheet ON assessment_sheet_columns (sheet_id, sort_order);

CREATE TABLE IF NOT EXISTS assessment_sheet_cells (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id BIGINT NOT NULL,
    column_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    score DECIMAL(5, 2),
    remarks VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    CONSTRAINT uk_assessment_cell UNIQUE (column_id, student_id),
    CONSTRAINT fk_ascell_sheet FOREIGN KEY (sheet_id) REFERENCES assessment_sheets (id) ON DELETE CASCADE,
    CONSTRAINT fk_ascell_column FOREIGN KEY (column_id) REFERENCES assessment_sheet_columns (id) ON DELETE CASCADE,
    CONSTRAINT fk_ascell_student FOREIGN KEY (student_id) REFERENCES students (id)
);
CREATE INDEX idx_ascell_sheet ON assessment_sheet_cells (sheet_id);

CREATE TABLE IF NOT EXISTS assessment_sheet_types (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id BIGINT NOT NULL,
    code VARCHAR(30) NOT NULL,
    name VARCHAR(50) NOT NULL,
    max_score DECIMAL(5, 2) NOT NULL DEFAULT 100,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    CONSTRAINT uk_assessment_sheet_type UNIQUE (sheet_id, code),
    CONSTRAINT fk_ast_sheet FOREIGN KEY (sheet_id) REFERENCES assessment_sheets (id) ON DELETE CASCADE
);
CREATE INDEX idx_ast_sheet ON assessment_sheet_types (sheet_id, sort_order);

ALTER TABLE assessment_sheet_cells ADD COLUMN type_code VARCHAR(30) NOT NULL DEFAULT 'SCORE';
ALTER TABLE assessment_sheet_cells DROP INDEX uk_assessment_cell;
ALTER TABLE assessment_sheet_cells ADD CONSTRAINT uk_assessment_cell UNIQUE (column_id, student_id, type_code);

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

INSERT INTO flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success)
VALUES
(80, '28', 'behavioural and weekly tests', 'SQL', 'V28__behavioural_and_weekly_tests.sql', -1996733888, 'school', NOW(), 20, 1),
(81, '30', 'assessment sheets', 'SQL', 'V30__assessment_sheets.sql', 1555149002, 'school', NOW(), 20, 1),
(82, '31', 'assessment sheet types', 'SQL', 'V31__assessment_sheet_types.sql', -1109775315, 'school', NOW(), 20, 1),
(83, '33', 'preschool reports', 'SQL', 'V33__preschool_reports.sql', -1754373474, 'school', NOW(), 20, 1);
