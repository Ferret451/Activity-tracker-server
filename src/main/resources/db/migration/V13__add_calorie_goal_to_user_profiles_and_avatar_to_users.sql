alter table user_profiles
    add column calorie_goal varchar(30) not null default 'MAINTAIN';

alter table users
    add column avatar_object_key varchar(512);

