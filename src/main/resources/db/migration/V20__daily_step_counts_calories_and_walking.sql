ALTER TABLE daily_step_counts
    ADD COLUMN burned_calories_kcal double precision NOT NULL DEFAULT 0,
    ADD COLUMN walking_duration_minutes integer NOT NULL DEFAULT 0;

ALTER TABLE daily_step_counts
    ADD CONSTRAINT chk_daily_step_counts_burned_calories_nonneg
        CHECK (burned_calories_kcal >= 0),
    ADD CONSTRAINT chk_daily_step_counts_walking_duration_nonneg
        CHECK (walking_duration_minutes >= 0);
