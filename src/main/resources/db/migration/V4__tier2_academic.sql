CREATE TABLE assessment_types (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(20) NOT NULL,
    name VARCHAR(50) NOT NULL,
    default_max_score DECIMAL(5, 2) NULL,
    weight DECIMAL(5, 2) NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    CONSTRAINT uk_assessment_types_code UNIQUE (code)
);

INSERT INTO assessment_types (code, name, default_max_score, weight) VALUES
    ('CA', 'Continuous Assessment', 30.00, 30.00),
    ('EXAM', 'Examination', 70.00, 70.00),
    ('PROJECT', 'Project', 100.00, 0.00);

CREATE TABLE term_results (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    term_id BIGINT NOT NULL,
    session_id BIGINT NOT NULL,
    class_id BIGINT NOT NULL,
    average_score DECIMAL(5, 2) NULL,
    rank_in_class INT NULL,
    comments TEXT NULL,
    published_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    CONSTRAINT uk_term_results_student_term UNIQUE (student_id, term_id),
    CONSTRAINT fk_term_results_student FOREIGN KEY (student_id) REFERENCES students (id) ON DELETE RESTRICT,
    CONSTRAINT fk_term_results_term FOREIGN KEY (term_id) REFERENCES terms (id) ON DELETE RESTRICT,
    CONSTRAINT fk_term_results_session FOREIGN KEY (session_id) REFERENCES academic_sessions (id) ON DELETE RESTRICT,
    CONSTRAINT fk_term_results_class FOREIGN KEY (class_id) REFERENCES classes (id) ON DELETE RESTRICT
);

CREATE TABLE notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(150) NULL,
    body TEXT NULL,
    entity_type VARCHAR(50) NULL,
    entity_id BIGINT NULL,
    read_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_notifications_user_read_at ON notifications (user_id, read_at);

CREATE TABLE school_calendar_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(150) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NULL,
    event_type VARCHAR(30) NOT NULL,
    class_id BIGINT NULL,
    description TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    CONSTRAINT fk_school_calendar_events_class FOREIGN KEY (class_id) REFERENCES classes (id) ON DELETE SET NULL
);
