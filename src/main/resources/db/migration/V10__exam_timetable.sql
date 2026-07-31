CREATE TABLE exam_timetable_slots (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    class_id BIGINT NOT NULL,
    subject_id BIGINT NOT NULL,
    teacher_id BIGINT NULL,
    term_id BIGINT NOT NULL,
    session_id BIGINT NOT NULL,
    exam_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    room VARCHAR(100) NULL,
    instructions TEXT NULL,
    published_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    CONSTRAINT uk_exam_slots_class_subject_term_date_time
        UNIQUE (class_id, subject_id, term_id, exam_date, start_time),
    CONSTRAINT fk_exam_slots_class FOREIGN KEY (class_id) REFERENCES classes (id) ON DELETE RESTRICT,
    CONSTRAINT fk_exam_slots_subject FOREIGN KEY (subject_id) REFERENCES subjects (id) ON DELETE RESTRICT,
    CONSTRAINT fk_exam_slots_teacher FOREIGN KEY (teacher_id) REFERENCES teachers (id) ON DELETE SET NULL,
    CONSTRAINT fk_exam_slots_term FOREIGN KEY (term_id) REFERENCES terms (id) ON DELETE RESTRICT,
    CONSTRAINT fk_exam_slots_session FOREIGN KEY (session_id) REFERENCES academic_sessions (id) ON DELETE RESTRICT
);

CREATE INDEX idx_exam_slots_class_term ON exam_timetable_slots (class_id, term_id);
