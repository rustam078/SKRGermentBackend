-- Left navigation order (JSON array of item keys, ascending). Editable from Settings.
-- Unknown/new keys not listed here fall back to their built-in order at the end.
INSERT INTO system_setting (id, setting_key, setting_value, description, created_at, updated_at)
VALUES
    (gen_random_uuid(), 'MENU_ORDER',
        '["dashboard","production","inventory","products","investment","employees","sales","settings"]',
        'Order of the left sidebar menu items (JSON array of keys, ascending)', NOW(), NOW())
ON CONFLICT (setting_key) DO NOTHING;
