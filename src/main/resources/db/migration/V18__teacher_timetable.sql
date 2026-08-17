CREATE TABLE teacher_timetable_slots (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    teacher_id BIGINT NOT NULL,
    day_of_week VARCHAR(20) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    class_id BIGINT NOT NULL,
    subject_id BIGINT NULL,
    room VARCHAR(100) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    CONSTRAINT fk_tts_teacher FOREIGN KEY (teacher_id) REFERENCES teachers (id) ON DELETE CASCADE,
    CONSTRAINT fk_tts_class FOREIGN KEY (class_id) REFERENCES classes (id) ON DELETE RESTRICT,
    CONSTRAINT fk_tts_subject FOREIGN KEY (subject_id) REFERENCES subjects (id) ON DELETE SET NULL
);

CREATE INDEX idx_tts_teacher_day ON teacher_timetable_slots (teacher_id, day_of_week);
