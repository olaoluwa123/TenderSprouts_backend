ALTER TABLE announcements ADD COLUMN audience VARCHAR(30) DEFAULT 'ALL_PARENTS';
ALTER TABLE announcements ADD COLUMN published_at TIMESTAMP;
ALTER TABLE announcements ADD COLUMN expires_at TIMESTAMP;

UPDATE announcements
SET audience = CASE WHEN class_id IS NOT NULL THEN 'CLASS' ELSE 'ALL_PARENTS' END;

CREATE TABLE announcement_recipients (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    announcement_id BIGINT NOT NULL,
    recipient_type VARCHAR(20) NOT NULL,
    recipient_id BIGINT NOT NULL,
    CONSTRAINT fk_announcement_recipients_announcement
        FOREIGN KEY (announcement_id) REFERENCES announcements (id) ON DELETE CASCADE
);

CREATE INDEX idx_announcement_recipients_announcement ON announcement_recipients (announcement_id);
CREATE INDEX idx_announcement_recipients_type_id ON announcement_recipients (recipient_type, recipient_id);
CREATE INDEX idx_announcements_audience_created ON announcements (audience, created_at DESC);
