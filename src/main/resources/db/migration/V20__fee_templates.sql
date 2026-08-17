CREATE TABLE fee_templates (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    class_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    CONSTRAINT uk_fee_templates_class UNIQUE (class_id),
    CONSTRAINT fk_fee_templates_class FOREIGN KEY (class_id) REFERENCES classes (id) ON DELETE CASCADE
);

CREATE TABLE fee_template_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    default_amount DECIMAL(10, 2) NOT NULL,
    description VARCHAR(500) NULL,
    sort_order INT NOT NULL DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    CONSTRAINT uk_fee_template_items_template_name UNIQUE (template_id, name),
    CONSTRAINT fk_fee_template_items_template
        FOREIGN KEY (template_id) REFERENCES fee_templates (id) ON DELETE CASCADE
);

CREATE INDEX idx_fee_template_items_template ON fee_template_items (template_id, sort_order);
