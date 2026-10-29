CREATE TABLE school_calendar_poster (
    id BIGINT PRIMARY KEY,
    original_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_bytes MEDIUMBLOB NOT NULL,
    uploaded_by_user_id BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL
);
