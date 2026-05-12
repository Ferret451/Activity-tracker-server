-- Roles for admin vs regular user
ALTER TABLE users
    ADD COLUMN role varchar(20) NOT NULL DEFAULT 'USER',
    ADD CONSTRAINT chk_users_role CHECK (role IN ('USER', 'ADMIN'));

-- App preferences (theme, language, notifications, etc.)
CREATE TABLE user_settings (
    user_id uuid PRIMARY KEY,
    preferences_json jsonb NOT NULL DEFAULT '{}'::jsonb,
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone NOT NULL,
    CONSTRAINT fk_user_settings_user_id FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

-- Live workout tracking session
CREATE TABLE workout_sessions (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL,
    activity_type_id uuid NOT NULL,
    status varchar(20) NOT NULL,
    started_at timestamp with time zone NOT NULL,
    ended_at timestamp with time zone,
    activity_record_id uuid,
    note varchar(500),
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone NOT NULL,
    CONSTRAINT chk_workout_sessions_status CHECK (status IN ('ACTIVE', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT fk_workout_sessions_user_id FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_workout_sessions_activity_type_id FOREIGN KEY (activity_type_id) REFERENCES activity_types (id) ON DELETE RESTRICT,
    CONSTRAINT fk_workout_sessions_activity_record_id FOREIGN KEY (activity_record_id) REFERENCES activity_records (id) ON DELETE SET NULL
);

CREATE UNIQUE INDEX uq_workout_sessions_one_active_per_user
    ON workout_sessions (user_id)
    WHERE (status = 'ACTIVE');

CREATE INDEX idx_workout_sessions_user_started ON workout_sessions (user_id, started_at DESC);

CREATE TABLE workout_route_points (
    id uuid PRIMARY KEY,
    session_id uuid NOT NULL,
    point_seq integer NOT NULL,
    latitude double precision NOT NULL,
    longitude double precision NOT NULL,
    recorded_at timestamp with time zone NOT NULL,
    CONSTRAINT fk_workout_route_points_session_id FOREIGN KEY (session_id) REFERENCES workout_sessions (id) ON DELETE CASCADE,
    CONSTRAINT uq_workout_route_points_session_seq UNIQUE (session_id, point_seq)
);

CREATE INDEX idx_workout_route_points_session_seq ON workout_route_points (session_id, point_seq);

CREATE TABLE weight_entries (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL,
    weight_kg numeric(10, 3) NOT NULL,
    measured_at timestamp with time zone NOT NULL,
    note varchar(500),
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone NOT NULL,
    CONSTRAINT fk_weight_entries_user_id FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_weight_entries_weight_positive CHECK (weight_kg > 0)
);

CREATE INDEX idx_weight_entries_user_measured ON weight_entries (user_id, measured_at DESC);

CREATE TABLE daily_step_counts (
    user_id uuid NOT NULL,
    step_date date NOT NULL,
    steps integer NOT NULL,
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone NOT NULL,
    PRIMARY KEY (user_id, step_date),
    CONSTRAINT fk_daily_step_counts_user_id FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_daily_step_counts_steps_nonneg CHECK (steps >= 0)
);
