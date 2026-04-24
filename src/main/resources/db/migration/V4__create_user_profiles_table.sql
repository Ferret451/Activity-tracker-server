create table user_profiles (
    user_id uuid primary key,
    weight_kg double precision not null,
    height_cm integer not null,
    age integer not null,
    sex varchar(10) not null,
    activity_level varchar(20) not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_user_profiles_user_id foreign key (user_id) references users (id) on delete cascade
);

