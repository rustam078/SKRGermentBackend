CREATE TABLE system_setting
(
    id              UUID PRIMARY KEY,

    setting_key     VARCHAR(100) NOT NULL UNIQUE,

    setting_value   VARCHAR(500) NOT NULL,

    description     VARCHAR(500),

    created_at      TIMESTAMP,

    updated_at      TIMESTAMP
);

INSERT INTO system_setting
(
    id,
    setting_key,
    setting_value,
    description,
    created_at,
    updated_at
)
VALUES
(
    gen_random_uuid(),
    'LOW_STOCK_THRESHOLD',
    '50',
    'Global minimum stock alert threshold',
    NOW(),
    NOW()
);