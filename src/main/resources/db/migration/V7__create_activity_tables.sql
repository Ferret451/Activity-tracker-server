create table activity_types (
    id uuid primary key,
    user_id uuid not null,
    name varchar(80) not null,
    met double precision not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_activity_types_user_id foreign key (user_id) references users (id) on delete cascade,
    constraint uq_activity_types_user_name unique (user_id, name)
);

create index idx_activity_types_user_id on activity_types (user_id);

create table activity_records (
    id uuid primary key,
    user_id uuid not null,
    activity_type_id uuid not null,
    duration_minutes integer not null,
    distance_km double precision,
    note varchar(500),
    occurred_at timestamp with time zone not null,
    burned_calories_kcal double precision not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_activity_records_user_id foreign key (user_id) references users (id) on delete cascade,
    constraint fk_activity_records_activity_type_id foreign key (activity_type_id) references activity_types (id) on delete restrict
);

create index idx_activity_records_user_id on activity_records (user_id);
create index idx_activity_records_occurred_at on activity_records (occurred_at);
create index idx_activity_records_activity_type_id on activity_records (activity_type_id);

