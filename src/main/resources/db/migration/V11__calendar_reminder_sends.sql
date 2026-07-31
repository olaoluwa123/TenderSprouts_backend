CREATE TABLE calendar_event_reminder_sends (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    reminder_type VARCHAR(30) NOT NULL,
    sent_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_calendar_reminder_event_user_type UNIQUE (event_id, user_id, reminder_type),
    CONSTRAINT fk_calendar_reminder_event FOREIGN KEY (event_id) REFERENCES school_calendar_events (id) ON DELETE CASCADE,
    CONSTRAINT fk_calendar_reminder_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);
