CREATE TABLE staff_attendance (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    teacher_id BIGINT NOT NULL,
    date DATE NOT NULL,
    status VARCHAR(20) NOT NULL,
    marked_by BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    CONSTRAINT uk_staff_attendance_teacher_date UNIQUE (teacher_id, date),
    CONSTRAINT fk_staff_attendance_teacher FOREIGN KEY (teacher_id) REFERENCES teachers (id) ON DELETE RESTRICT,
    CONSTRAINT fk_staff_attendance_marked_by FOREIGN KEY (marked_by) REFERENCES users (id) ON DELETE SET NULL
);

CREATE INDEX idx_staff_attendance_date ON staff_attendance (date);
CREATE INDEX idx_staff_attendance_teacher_date ON staff_attendance (teacher_id, date);
