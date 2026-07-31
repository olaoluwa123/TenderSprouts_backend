CREATE TABLE periods (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    period_number INT NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    CONSTRAINT uk_periods_period_number UNIQUE (period_number)
);

CREATE TABLE timetable_slots (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    class_id BIGINT NOT NULL,
    subject_id BIGINT NOT NULL,
    teacher_id BIGINT NOT NULL,
    period_id BIGINT NOT NULL,
    session_id BIGINT NOT NULL,
    day_of_week INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    CONSTRAINT uk_timetable_slots_class_day_period_session UNIQUE (class_id, day_of_week, period_id, session_id),
    CONSTRAINT fk_timetable_slots_class FOREIGN KEY (class_id) REFERENCES classes (id) ON DELETE RESTRICT,
    CONSTRAINT fk_timetable_slots_subject FOREIGN KEY (subject_id) REFERENCES subjects (id) ON DELETE RESTRICT,
    CONSTRAINT fk_timetable_slots_teacher FOREIGN KEY (teacher_id) REFERENCES teachers (id) ON DELETE RESTRICT,
    CONSTRAINT fk_timetable_slots_period FOREIGN KEY (period_id) REFERENCES periods (id) ON DELETE RESTRICT,
    CONSTRAINT fk_timetable_slots_session FOREIGN KEY (session_id) REFERENCES academic_sessions (id) ON DELETE RESTRICT
);

CREATE TABLE assignments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(150) NOT NULL,
    description TEXT NULL,
    class_id BIGINT NOT NULL,
    subject_id BIGINT NOT NULL,
    teacher_id BIGINT NOT NULL,
    session_id BIGINT NOT NULL,
    due_date DATE NULL,
    max_score DECIMAL(5, 2) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    CONSTRAINT fk_assignments_class FOREIGN KEY (class_id) REFERENCES classes (id) ON DELETE RESTRICT,
    CONSTRAINT fk_assignments_subject FOREIGN KEY (subject_id) REFERENCES subjects (id) ON DELETE RESTRICT,
    CONSTRAINT fk_assignments_teacher FOREIGN KEY (teacher_id) REFERENCES teachers (id) ON DELETE RESTRICT,
    CONSTRAINT fk_assignments_session FOREIGN KEY (session_id) REFERENCES academic_sessions (id) ON DELETE RESTRICT
);

CREATE TABLE assignment_submissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    assignment_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    submitted_at TIMESTAMP NULL,
    score DECIMAL(5, 2) NULL,
    feedback TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    CONSTRAINT uk_assignment_submissions_assignment_student UNIQUE (assignment_id, student_id),
    CONSTRAINT fk_assignment_submissions_assignment FOREIGN KEY (assignment_id) REFERENCES assignments (id) ON DELETE CASCADE,
    CONSTRAINT fk_assignment_submissions_student FOREIGN KEY (student_id) REFERENCES students (id) ON DELETE RESTRICT
);

CREATE TABLE files (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    storage_key VARCHAR(500) NOT NULL,
    original_filename VARCHAR(255) NULL,
    mime_type VARCHAR(100) NULL,
    file_size BIGINT NULL,
    uploaded_by BIGINT NULL,
    entity_type VARCHAR(50) NULL,
    entity_id BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    CONSTRAINT fk_files_uploaded_by FOREIGN KEY (uploaded_by) REFERENCES users (id) ON DELETE SET NULL
);

CREATE TABLE fee_structures (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    class_id BIGINT NULL,
    session_id BIGINT NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    description VARCHAR(500) NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    CONSTRAINT fk_fee_structures_class FOREIGN KEY (class_id) REFERENCES classes (id) ON DELETE SET NULL,
    CONSTRAINT fk_fee_structures_session FOREIGN KEY (session_id) REFERENCES academic_sessions (id) ON DELETE RESTRICT
);

CREATE TABLE invoices (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    fee_structure_id BIGINT NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    due_date DATE NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    CONSTRAINT fk_invoices_student FOREIGN KEY (student_id) REFERENCES students (id) ON DELETE RESTRICT,
    CONSTRAINT fk_invoices_fee_structure FOREIGN KEY (fee_structure_id) REFERENCES fee_structures (id) ON DELETE RESTRICT
);

CREATE TABLE payments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    invoice_id BIGINT NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    gateway_reference VARCHAR(255) NULL,
    paid_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    CONSTRAINT fk_payments_invoice FOREIGN KEY (invoice_id) REFERENCES invoices (id) ON DELETE RESTRICT
);

CREATE TABLE incidents (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    incident_date DATE NOT NULL,
    description TEXT NOT NULL,
    action_taken TEXT NULL,
    reported_by BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    CONSTRAINT fk_incidents_student FOREIGN KEY (student_id) REFERENCES students (id) ON DELETE RESTRICT,
    CONSTRAINT fk_incidents_reported_by FOREIGN KEY (reported_by) REFERENCES users (id) ON DELETE SET NULL
);

CREATE TABLE school_settings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    setting_key VARCHAR(100) NOT NULL,
    setting_value TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    CONSTRAINT uk_school_settings_key UNIQUE (setting_key)
);

INSERT INTO school_settings (setting_key, setting_value) VALUES
    ('school.name', 'School Portal'),
    ('grading.scale', '100'),
    ('timezone', 'UTC');
