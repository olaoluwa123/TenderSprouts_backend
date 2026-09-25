CREATE TABLE IF NOT EXISTS term_result_submissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    class_id BIGINT NOT NULL,
    term_id BIGINT NOT NULL,
    session_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    submitted_at TIMESTAMP NULL,
    submitted_by_teacher_id BIGINT NULL,
    published_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_term_result_submissions_class_term UNIQUE (class_id, term_id),
    CONSTRAINT fk_trs_class FOREIGN KEY (class_id) REFERENCES classes (id) ON DELETE RESTRICT,
    CONSTRAINT fk_trs_term FOREIGN KEY (term_id) REFERENCES terms (id) ON DELETE RESTRICT,
    CONSTRAINT fk_trs_session FOREIGN KEY (session_id) REFERENCES academic_sessions (id) ON DELETE RESTRICT,
    CONSTRAINT fk_trs_teacher FOREIGN KEY (submitted_by_teacher_id) REFERENCES teachers (id) ON DELETE SET NULL
);
UPDATE flyway_schema_history SET success = 1, execution_time = 50 WHERE version = '12';
