-- Toggle: when enabled, sales add GST (DEFAULT_GST_PERCENT) on top of the net amount
-- (subtotal - discount). When disabled, sales are recorded without GST. The percent
-- itself is the existing DEFAULT_GST_PERCENT setting.
INSERT INTO system_setting (id, setting_key, setting_value, description, created_at, updated_at)
VALUES
    (gen_random_uuid(), 'SALES_GST_ENABLED', 'false',
        'When true, GST (DEFAULT_GST_PERCENT) is added to every new sale; when false, sales have no GST', NOW(), NOW())
ON CONFLICT (setting_key) DO NOTHING;
