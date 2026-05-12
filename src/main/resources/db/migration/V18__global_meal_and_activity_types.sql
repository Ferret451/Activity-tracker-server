-- Global meal_types and activity_types (no per-user rows); drop auto-seed trigger.

CREATE EXTENSION IF NOT EXISTS pgcrypto;

DROP TRIGGER IF EXISTS trg_seed_default_meal_types_after_user_insert ON users;
DROP FUNCTION IF EXISTS seed_default_meal_types_for_new_user() CASCADE;

-- ---------- meal_types ----------
CREATE TEMP TABLE tmp_meal_map_v18 AS
SELECT m.id AS old_id, c.canonical_id AS new_id
FROM meal_types m
INNER JOIN (
    SELECT DISTINCT ON (lower(trim(name)))
        id AS canonical_id,
        lower(trim(name)) AS lname
    FROM meal_types
    ORDER BY lower(trim(name)), created_at ASC, id ASC
) c ON lower(trim(m.name)) = c.lname;

UPDATE nutrition_records nr
SET meal_type_id = mm.new_id
FROM tmp_meal_map_v18 mm
WHERE nr.meal_type_id = mm.old_id;

ALTER TABLE nutrition_records DROP CONSTRAINT IF EXISTS fk_nutrition_records_meal_type_user;
ALTER TABLE nutrition_records DROP CONSTRAINT IF EXISTS fk_nutrition_records_food_item_user;

DELETE FROM meal_types
WHERE id IN (SELECT old_id FROM tmp_meal_map_v18 WHERE old_id <> new_id);

ALTER TABLE meal_types DROP CONSTRAINT IF EXISTS uq_meal_types_id_user;
ALTER TABLE meal_types DROP CONSTRAINT IF EXISTS fk_meal_types_user_id;
ALTER TABLE meal_types DROP COLUMN user_id;

DROP INDEX IF EXISTS idx_meal_types_user_id;

ALTER TABLE meal_types DROP CONSTRAINT IF EXISTS uq_meal_types_user_name;

DROP INDEX IF EXISTS uq_meal_types_name_lower;
CREATE UNIQUE INDEX uq_meal_types_name_lower ON meal_types (lower(trim(name)));

DROP TABLE IF EXISTS tmp_meal_map_v18;

ALTER TABLE nutrition_records
    ADD CONSTRAINT fk_nutrition_records_meal_type_id
        FOREIGN KEY (meal_type_id) REFERENCES meal_types (id) ON DELETE RESTRICT;

ALTER TABLE nutrition_records
    ADD CONSTRAINT fk_nutrition_records_food_item_id
        FOREIGN KEY (food_item_id) REFERENCES food_items (id) ON DELETE RESTRICT;

-- ---------- activity_types ----------
CREATE TEMP TABLE tmp_activity_map_v18 AS
SELECT m.id AS old_id, c.canonical_id AS new_id
FROM activity_types m
INNER JOIN (
    SELECT DISTINCT ON (lower(trim(name)), met)
        id AS canonical_id,
        lower(trim(name)) AS lname,
        met
    FROM activity_types
    ORDER BY lower(trim(name)), met, created_at ASC, id ASC
) c ON lower(trim(m.name)) = c.lname AND m.met = c.met;

UPDATE activity_records ar
SET activity_type_id = mm.new_id
FROM tmp_activity_map_v18 mm
WHERE ar.activity_type_id = mm.old_id;

UPDATE workout_sessions ws
SET activity_type_id = mm.new_id
FROM tmp_activity_map_v18 mm
WHERE ws.activity_type_id = mm.old_id;

DELETE FROM activity_types
WHERE id IN (SELECT old_id FROM tmp_activity_map_v18 WHERE old_id <> new_id);

ALTER TABLE activity_types DROP CONSTRAINT IF EXISTS fk_activity_types_user_id;
ALTER TABLE activity_types DROP COLUMN user_id;

DROP INDEX IF EXISTS idx_activity_types_user_id;

ALTER TABLE activity_types DROP CONSTRAINT IF EXISTS uq_activity_types_user_name;

DROP INDEX IF EXISTS uq_activity_types_name_met;
CREATE UNIQUE INDEX uq_activity_types_name_met ON activity_types (lower(trim(name)), met);

DROP TABLE IF EXISTS tmp_activity_map_v18;

INSERT INTO meal_types (id, name, created_at, updated_at)
SELECT gen_random_uuid(), v.name, (now() AT TIME ZONE 'utc'), (now() AT TIME ZONE 'utc')
FROM (VALUES ('breakfast'), ('lunch'), ('dinner'), ('snack')) AS v(name)
WHERE NOT EXISTS (SELECT 1 FROM meal_types);

INSERT INTO activity_types (id, name, met, created_at, updated_at)
SELECT gen_random_uuid(), 'Walking', 3.5, (now() AT TIME ZONE 'utc'), (now() AT TIME ZONE 'utc')
WHERE NOT EXISTS (SELECT 1 FROM activity_types);
