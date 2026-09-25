CREATE TABLE csv_import_jobs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    type VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL,
    actor_user_id BIGINT NOT NULL,
    original_filename VARCHAR(255) NULL,
    file_bytes BLOB NOT NULL,
    total_rows INT NOT NULL DEFAULT 0,
    success_count INT NOT NULL DEFAULT 0,
    failure_count INT NOT NULL DEFAULT 0,
    results_json TEXT NULL,
    error_message TEXT NULL,
    started_at TIMESTAMP NULL,
    completed_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    CONSTRAINT fk_csv_import_jobs_actor FOREIGN KEY (actor_user_id) REFERENCES users (id) ON DELETE RESTRICT
);

CREATE INDEX idx_csv_import_jobs_status ON csv_import_jobs (status);
