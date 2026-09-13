-- Default QR label rendering config (which text fields print on each label, and
-- whether the product name runs vertically on the left). Stored as JSON string.
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
    'QR_LABEL_CONFIG',
    '{"showName":true,"showPrice":true,"showSerial":true,"nameVertical":false}',
    'QR label print settings: show/hide fields and product-name orientation',
    NOW(),
    NOW()
)
ON CONFLICT (setting_key) DO NOTHING;
