BEGIN;

CREATE EXTENSION IF NOT EXISTS pgcrypto;

INSERT INTO users (
    id,
    email,
    username,
    first_name,
    last_name,
    password_hash,
    role,
    created_at,
    updated_at
)
VALUES (
    gen_random_uuid(),
    'admin@local.test',
    'admin_20260518_root',
    'Admin',
    'Local',
    crypt('Admin@2026!', gen_salt('bf', 10)),
    'ADMIN',
    NOW(),
    NOW()
)
ON CONFLICT (email) DO UPDATE
SET
    username = EXCLUDED.username,
    first_name = EXCLUDED.first_name,
    last_name = EXCLUDED.last_name,
    password_hash = EXCLUDED.password_hash,
    role = 'ADMIN',
    updated_at = NOW();

COMMIT;

SELECT id, email, username, role, created_at, updated_at
FROM users
WHERE email = 'admin@local.test';

