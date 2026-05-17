ALTER TABLE activity_types
    ADD COLUMN supports_distance boolean NOT NULL DEFAULT false;

UPDATE activity_types
SET supports_distance = true
WHERE lower(trim(name)) IN (
    'walking',
    'walking, light',
    'walking, brisk',
    'jogging',
    'running, moderate',
    'swimming, leisure',
    'swimming, moderate',
    'cycling, leisure',
    'cycling, moderate',
    'hiking',
    'elliptical trainer',
    'rowing, moderate',
    'dancing',
    'hiit',
    'basketball',
    'soccer',
    'tennis'
);

CREATE OR REPLACE FUNCTION check_activity_record_distance()
    RETURNS trigger AS
$$
BEGIN
    IF NEW.distance_km IS NOT NULL AND NOT EXISTS (
        SELECT 1
        FROM activity_types
        WHERE id = NEW.activity_type_id
          AND supports_distance = true
    ) THEN
        RAISE EXCEPTION 'Activity type does not support distance';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_activity_records_distance_support
    BEFORE INSERT OR UPDATE OF distance_km, activity_type_id
    ON activity_records
    FOR EACH ROW
    EXECUTE FUNCTION check_activity_record_distance();
