CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(100) NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    last_login_at TIMESTAMP NULL,
    email_verified_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE TABLE academic_sessions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(20) NOT NULL,
    start_date DATE NULL,
    end_date DATE NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL
);

CREATE TABLE classes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL
);

CREATE TABLE subjects (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    CONSTRAINT uk_subjects_name UNIQUE (name)
);

CREATE TABLE terms (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id BIGINT NOT NULL,
    name VARCHAR(20) NOT NULL,
    start_date DATE NULL,
    end_date DATE NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    CONSTRAINT fk_terms_session FOREIGN KEY (session_id) REFERENCES academic_sessions (id) ON DELETE RESTRICT
);

CREATE TABLE admins (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    full_name VARCHAR(100) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    CONSTRAINT uk_admins_user_id UNIQUE (user_id),
    CONSTRAINT fk_admins_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE RESTRICT
);

CREATE TABLE teachers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    full_name VARCHAR(100) NULL,
    phone VARCHAR(20) NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    CONSTRAINT uk_teachers_user_id UNIQUE (user_id),
    CONSTRAINT fk_teachers_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE RESTRICT
);

CREATE TABLE parents (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    full_name VARCHAR(100) NULL,
    phone VARCHAR(20) NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    CONSTRAINT uk_parents_user_id UNIQUE (user_id),
    CONSTRAINT fk_parents_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE RESTRICT
);

CREATE TABLE students (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    first_name VARCHAR(50) NULL,
    last_name VARCHAR(50) NULL,
    date_of_birth DATE NULL,
    admission_number VARCHAR(50) NULL,
    gender VARCHAR(10) NULL,
    user_id BIGINT NULL,
    class_id BIGINT NULL,
    current_enrollment_id BIGINT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    CONSTRAINT uk_students_admission_number UNIQUE (admission_number),
    CONSTRAINT fk_students_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT fk_students_class FOREIGN KEY (class_id) REFERENCES classes (id) ON DELETE SET NULL
);

CREATE TABLE class_subject (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    class_id BIGINT NOT NULL,
    subject_id BIGINT NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    CONSTRAINT uk_class_subject_class_subject UNIQUE (class_id, subject_id),
    CONSTRAINT fk_class_subject_class FOREIGN KEY (class_id) REFERENCES classes (id) ON DELETE RESTRICT,
    CONSTRAINT fk_class_subject_subject FOREIGN KEY (subject_id) REFERENCES subjects (id) ON DELETE RESTRICT
);

CREATE TABLE teacher_subject (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    teacher_id BIGINT NOT NULL,
    subject_id BIGINT NOT NULL,
    class_id BIGINT NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    CONSTRAINT uk_teacher_subject_teacher_subject_class UNIQUE (teacher_id, subject_id, class_id),
    CONSTRAINT fk_teacher_subject_teacher FOREIGN KEY (teacher_id) REFERENCES teachers (id) ON DELETE RESTRICT,
    CONSTRAINT fk_teacher_subject_subject FOREIGN KEY (subject_id) REFERENCES subjects (id) ON DELETE RESTRICT,
    CONSTRAINT fk_teacher_subject_class FOREIGN KEY (class_id) REFERENCES classes (id) ON DELETE RESTRICT
);

CREATE TABLE teacher_class (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    teacher_id BIGINT NOT NULL,
    class_id BIGINT NOT NULL,
    start_date DATE NULL,
    end_date DATE NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    CONSTRAINT uk_teacher_class_teacher_class UNIQUE (teacher_id, class_id),
    CONSTRAINT fk_teacher_class_teacher FOREIGN KEY (teacher_id) REFERENCES teachers (id) ON DELETE RESTRICT,
    CONSTRAINT fk_teacher_class_class FOREIGN KEY (class_id) REFERENCES classes (id) ON DELETE RESTRICT
);

CREATE TABLE parent_student (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    parent_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    CONSTRAINT uk_parent_student_parent_student UNIQUE (parent_id, student_id),
    CONSTRAINT fk_parent_student_parent FOREIGN KEY (parent_id) REFERENCES parents (id) ON DELETE RESTRICT,
    CONSTRAINT fk_parent_student_student FOREIGN KEY (student_id) REFERENCES students (id) ON DELETE RESTRICT
);

CREATE TABLE grades (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    subject_id BIGINT NOT NULL,
    teacher_id BIGINT NOT NULL,
    class_id BIGINT NOT NULL,
    session_id BIGINT NOT NULL,
    term_id BIGINT NOT NULL,
    assessment_type VARCHAR(20) NOT NULL,
    score DECIMAL(5, 2) NULL,
    max_score DECIMAL(5, 2) NULL,
    remarks VARCHAR(500) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    CONSTRAINT uk_grades_student_subject_term_assessment UNIQUE (student_id, subject_id, term_id, assessment_type),
    CONSTRAINT fk_grades_student FOREIGN KEY (student_id) REFERENCES students (id) ON DELETE RESTRICT,
    CONSTRAINT fk_grades_subject FOREIGN KEY (subject_id) REFERENCES subjects (id) ON DELETE RESTRICT,
    CONSTRAINT fk_grades_teacher FOREIGN KEY (teacher_id) REFERENCES teachers (id) ON DELETE RESTRICT,
    CONSTRAINT fk_grades_class FOREIGN KEY (class_id) REFERENCES classes (id) ON DELETE RESTRICT,
    CONSTRAINT fk_grades_session FOREIGN KEY (session_id) REFERENCES academic_sessions (id) ON DELETE RESTRICT,
    CONSTRAINT fk_grades_term FOREIGN KEY (term_id) REFERENCES terms (id) ON DELETE RESTRICT
);

CREATE TABLE attendance (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    class_id BIGINT NOT NULL,
    session_id BIGINT NULL,
    term_id BIGINT NULL,
    date DATE NOT NULL,
    status VARCHAR(20) NOT NULL,
    marked_by BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    CONSTRAINT uk_attendance_student_date UNIQUE (student_id, date),
    CONSTRAINT fk_attendance_student FOREIGN KEY (student_id) REFERENCES students (id) ON DELETE RESTRICT,
    CONSTRAINT fk_attendance_class FOREIGN KEY (class_id) REFERENCES classes (id) ON DELETE RESTRICT,
    CONSTRAINT fk_attendance_session FOREIGN KEY (session_id) REFERENCES academic_sessions (id) ON DELETE SET NULL,
    CONSTRAINT fk_attendance_term FOREIGN KEY (term_id) REFERENCES terms (id) ON DELETE SET NULL,
    CONSTRAINT fk_attendance_marked_by FOREIGN KEY (marked_by) REFERENCES users (id) ON DELETE SET NULL
);

CREATE TABLE announcements (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(150) NULL,
    content TEXT NULL,
    class_id BIGINT NULL,
    created_by BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    CONSTRAINT fk_announcements_class FOREIGN KEY (class_id) REFERENCES classes (id) ON DELETE SET NULL,
    CONSTRAINT fk_announcements_created_by FOREIGN KEY (created_by) REFERENCES users (id) ON DELETE SET NULL
);

CREATE TABLE messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sender_id BIGINT NOT NULL,
    receiver_id BIGINT NOT NULL,
    student_id BIGINT NULL,
    thread_id BIGINT NULL,
    content TEXT NOT NULL,
    read_at TIMESTAMP NULL,
    deleted_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    CONSTRAINT fk_messages_sender FOREIGN KEY (sender_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_messages_receiver FOREIGN KEY (receiver_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_messages_student FOREIGN KEY (student_id) REFERENCES students (id) ON DELETE SET NULL
);

CREATE TABLE audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    admin_id BIGINT NULL,
    user_id BIGINT NULL,
    user_role VARCHAR(20) NULL,
    action VARCHAR(255) NULL,
    entity_type VARCHAR(50) NULL,
    entity_id BIGINT NULL,
    ip_address VARCHAR(45) NULL,
    request_id VARCHAR(64) NULL,
    old_value TEXT NULL,
    new_value TEXT NULL,
    details TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    CONSTRAINT fk_audit_logs_admin FOREIGN KEY (admin_id) REFERENCES admins (id) ON DELETE SET NULL,
    CONSTRAINT fk_audit_logs_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL
);
