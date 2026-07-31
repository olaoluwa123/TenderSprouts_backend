CREATE TABLE student_enrollments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    class_id BIGINT NOT NULL,
    session_id BIGINT NOT NULL,
    start_date DATE NULL,
    end_date DATE NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    CONSTRAINT uk_student_enrollments_student_session UNIQUE (student_id, session_id),
    CONSTRAINT fk_student_enrollments_student FOREIGN KEY (student_id) REFERENCES students (id) ON DELETE RESTRICT,
    CONSTRAINT fk_student_enrollments_class FOREIGN KEY (class_id) REFERENCES classes (id) ON DELETE RESTRICT,
    CONSTRAINT fk_student_enrollments_session FOREIGN KEY (session_id) REFERENCES academic_sessions (id) ON DELETE RESTRICT
);

ALTER TABLE students
    ADD CONSTRAINT fk_students_current_enrollment
    FOREIGN KEY (current_enrollment_id) REFERENCES student_enrollments (id) ON DELETE SET NULL;
