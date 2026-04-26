-- Seed default meal types for existing users and auto-seed for new users.

create extension if not exists pgcrypto;

insert into meal_types (id, user_id, name, created_at, updated_at)
select
    gen_random_uuid() as id,
    u.id as user_id,
    dm.name,
    now() at time zone 'utc',
    now() at time zone 'utc'
from users u
cross join (
    values
        ('breakfast'),
        ('lunch'),
        ('dinner'),
        ('snack')
) as dm(name)
where not exists (
    select 1
    from meal_types mt
    where mt.user_id = u.id
      and lower(mt.name) = dm.name
)
on conflict (user_id, name) do nothing;

create or replace function seed_default_meal_types_for_new_user()
returns trigger as $$
begin
    insert into meal_types (id, user_id, name, created_at, updated_at)
    values
        (gen_random_uuid(), new.id, 'breakfast', now() at time zone 'utc', now() at time zone 'utc'),
        (gen_random_uuid(), new.id, 'lunch', now() at time zone 'utc', now() at time zone 'utc'),
        (gen_random_uuid(), new.id, 'dinner', now() at time zone 'utc', now() at time zone 'utc'),
        (gen_random_uuid(), new.id, 'snack', now() at time zone 'utc', now() at time zone 'utc')
    on conflict (user_id, name) do nothing;

    return new;
end;
$$ language plpgsql;

drop trigger if exists trg_seed_default_meal_types_after_user_insert on users;

create trigger trg_seed_default_meal_types_after_user_insert
after insert on users
for each row
execute function seed_default_meal_types_for_new_user();


