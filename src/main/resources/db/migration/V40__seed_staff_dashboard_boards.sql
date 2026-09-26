-- Which dashboard boards a STAFF user can see (JSON array of board keys). ADMIN sees all.
-- Board keys: overview, sales, inventory, expenses, employees. Editable from
-- Settings -> Users & Roles. Default: sales + inventory only.
INSERT INTO system_setting (id, setting_key, setting_value, description, created_at, updated_at)
VALUES
    (gen_random_uuid(), 'STAFF_DASHBOARD_BOARDS', '["sales","inventory"]',
     'Dashboard boards visible to STAFF (JSON array of board keys); ADMIN sees all', NOW(), NOW())
ON CONFLICT (setting_key) DO NOTHING;
