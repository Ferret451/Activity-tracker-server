create table user_goals (
    id uuid primary key,
    user_id uuid not null,
    goal_type varchar(40) not null,
    status varchar(20) not null,
    title varchar(120) not null,
    initial_value double precision not null,
    target_value double precision not null,
    current_value double precision not null,
    start_date date not null,
    target_date date,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_user_goals_user_id foreign key (user_id) references users (id) on delete cascade
);

create index idx_user_goals_user_id on user_goals (user_id);
create index idx_user_goals_status on user_goals (status);

create table user_goal_progress (
    id uuid primary key,
    goal_id uuid not null,
    current_value double precision not null,
    note varchar(500),
    recorded_at timestamp with time zone not null,
    constraint fk_user_goal_progress_goal_id foreign key (goal_id) references user_goals (id) on delete cascade
);

create index idx_user_goal_progress_goal_id on user_goal_progress (goal_id);
create index idx_user_goal_progress_recorded_at on user_goal_progress (recorded_at);

