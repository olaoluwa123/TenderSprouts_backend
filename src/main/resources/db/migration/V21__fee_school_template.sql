CREATE TABLE fee_school_template_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    default_amount DECIMAL(10, 2) NOT NULL,
    description VARCHAR(500) NULL,
    sort_order INT NOT NULL DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    CONSTRAINT uk_fee_school_template_items_name UNIQUE (name)
);

CREATE INDEX idx_fee_school_template_items_sort ON fee_school_template_items (sort_order, name);
