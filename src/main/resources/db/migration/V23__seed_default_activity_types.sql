-- Seed global activity_types catalog (idempotent per name + met).

CREATE EXTENSION IF NOT EXISTS pgcrypto;

INSERT INTO activity_types (id, name, met, created_at, updated_at)
SELECT gen_random_uuid(), v.name, v.met, (now() AT TIME ZONE 'utc'), (now() AT TIME ZONE 'utc')
FROM (VALUES
    ('Walking', 3.5),
    ('Walking, light', 2.5),
    ('Walking, brisk', 4.3),
    ('Jogging', 7.0),
    ('Running, moderate', 9.8),
    ('Swimming, leisure', 6.0),
    ('Swimming, moderate', 8.0),
    ('Sitting', 1.5),
    ('Meditation', 1.0),
    ('Reading', 1.3),
    ('Resting', 1.0),
    ('Cycling, leisure', 4.0),
    ('Cycling, moderate', 6.8),
    ('Hiking', 6.0),
    ('Elliptical trainer', 5.0),
    ('Rowing, moderate', 7.0),
    ('Dancing', 5.0),
    ('HIIT', 8.0),
    ('Basketball', 6.5),
    ('Soccer', 7.0),
    ('Tennis', 7.3),
    ('Yoga', 2.5),
    ('Stretching', 2.3),
    ('Weight training', 5.0)
) AS v(name, met)
WHERE NOT EXISTS (
    SELECT 1
    FROM activity_types at
    WHERE lower(trim(at.name)) = lower(trim(v.name))
      AND at.met = v.met
);
