CREATE TABLE email_outbox (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    to_email VARCHAR(255) NOT NULL,
    subject VARCHAR(500) NOT NULL,
    body TEXT NOT NULL,
    attachments_json TEXT NULL,
    attempt INT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL,
    last_error VARCHAR(500) NULL,
    next_attempt_at TIMESTAMP NULL,
    sent_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL
);

CREATE INDEX idx_email_outbox_status_next ON email_outbox (status, next_attempt_at);
