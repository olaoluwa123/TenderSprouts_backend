-- Admission applications queue for the admin dashboard.
CREATE TABLE admission_applications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    applicant_first_name VARCHAR(50) NOT NULL,
    applicant_last_name VARCHAR(50) NOT NULL,
    date_of_birth DATE NULL,
    gender VARCHAR(10) NULL,
    prospective_class_id BIGINT NULL,
    parent_full_name VARCHAR(100) NULL,
    parent_email VARCHAR(150) NULL,
    parent_phone VARCHAR(30) NULL,
    status VARCHAR(20) NOT NULL,
    notes TEXT NULL,
    reviewed_by BIGINT NULL,
    reviewed_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    CONSTRAINT fk_admission_applications_class FOREIGN KEY (prospective_class_id) REFERENCES classes (id) ON DELETE SET NULL,
    CONSTRAINT fk_admission_applications_reviewed_by FOREIGN KEY (reviewed_by) REFERENCES users (id) ON DELETE SET NULL
);

CREATE INDEX idx_admission_applications_status_created ON admission_applications (status, created_at DESC);

ALTER TABLE announcements ADD COLUMN is_important BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX idx_announcements_important_created ON announcements (is_important, created_at DESC);
CREATE INDEX idx_school_calendar_events_start ON school_calendar_events (start_date);
