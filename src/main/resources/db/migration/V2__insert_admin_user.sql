INSERT INTO app_user
(
    id,
    username,
    password,
    full_name,
    role,
    is_active,
    created_at,
    updated_at
)
VALUES
(
    gen_random_uuid(),
    'admin',
    'admin123',
    'System Administrator',
    'ADMIN',
    true,
    now(),
    now()
);