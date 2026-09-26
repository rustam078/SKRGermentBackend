-- STAFF permission matrix (module -> view/write/delete). ADMIN is implicit full access and
-- is not stored here. Editable from Settings -> Users & Roles. Default: staff can view the
-- dashboard, sell and view sales, and view inventory/products/investment; no deletes, no
-- production/employees/settings access.
INSERT INTO system_setting (id, setting_key, setting_value, description, created_at, updated_at)
VALUES
    (gen_random_uuid(), 'ROLE_PERMISSIONS',
     '{"STAFF":{"dashboard":{"view":true,"write":false,"delete":false},"sales":{"view":true,"write":true,"delete":false},"inventory":{"view":true,"write":false,"delete":false},"products":{"view":true,"write":false,"delete":false},"investment":{"view":true,"write":false,"delete":false},"production":{"view":false,"write":false,"delete":false},"employees":{"view":false,"write":false,"delete":false},"settings":{"view":false,"write":false,"delete":false}}}',
     'Per-role, per-module permissions (view/write/delete) for STAFF; ADMIN is always full access', NOW(), NOW())
ON CONFLICT (setting_key) DO NOTHING;
