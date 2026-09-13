-- Company profile + invoice defaults, editable from the Settings screen.
-- Company info is read live everywhere (change it once, it reflects on every invoice/header).
-- DEFAULT_GST_PERCENT and CURRENCY_SYMBOL are defaults applied to NEW invoices; existing
-- invoices keep their snapshotted amounts.
INSERT INTO system_setting (id, setting_key, setting_value, description, created_at, updated_at)
VALUES
    (gen_random_uuid(), 'COMPANY_NAME',        'SKR Garment',
        'Company name shown on invoices and headers', NOW(), NOW()),
    (gen_random_uuid(), 'COMPANY_ADDRESS',     '14 Loom Street, Industrial Area, Patna, Bihar 800001',
        'Company address shown on invoices', NOW(), NOW()),
    (gen_random_uuid(), 'COMPANY_CONTACT',     '+91 90000 12345',
        'Company phone / email shown on invoices', NOW(), NOW()),
    (gen_random_uuid(), 'COMPANY_GSTIN',       '10ABCDE1234F1Z5',
        'Company GST identification number', NOW(), NOW()),
    (gen_random_uuid(), 'DEFAULT_GST_PERCENT', '5',
        'Default GST percent applied to new invoices (snapshotted per invoice)', NOW(), NOW()),
    (gen_random_uuid(), 'CURRENCY_SYMBOL',     '₹',
        'Currency symbol used across the app', NOW(), NOW())
ON CONFLICT (setting_key) DO NOTHING;
