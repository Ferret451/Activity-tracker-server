-- Nutrition: stronger integrity, numeric types, and better indexes
-- Goals: add DB-level enums via CHECKs, value/date constraints, and sync trigger for current_value

-- 1) Ensure meal_types and food_items can be referenced by (id, user_id)
alter table meal_types
    add constraint uq_meal_types_id_user unique (id, user_id);

alter table food_items
    add constraint uq_food_items_id_user unique (id, user_id);

-- 2) Convert nutrition numeric fields to numeric(10,2)
alter table food_items
    alter column calories_per_100g type numeric(10,2) using round(calories_per_100g::numeric, 2),
    alter column protein_per_100g type numeric(10,2) using round(protein_per_100g::numeric, 2),
    alter column fat_per_100g type numeric(10,2) using round(fat_per_100g::numeric, 2),
    alter column carbs_per_100g type numeric(10,2) using round(carbs_per_100g::numeric, 2);

alter table nutrition_records
    alter column grams type numeric(10,2) using round(grams::numeric, 2),
    alter column total_calories_kcal type numeric(10,2) using round(total_calories_kcal::numeric, 2),
    alter column total_protein_g type numeric(10,2) using round(total_protein_g::numeric, 2),
    alter column total_fat_g type numeric(10,2) using round(total_fat_g::numeric, 2),
    alter column total_carbs_g type numeric(10,2) using round(total_carbs_g::numeric, 2);

-- 3) Nutrition CHECK constraints
alter table food_items
    add constraint chk_food_items_macros_non_negative check (
        calories_per_100g >= 0 and
        protein_per_100g >= 0 and
        fat_per_100g >= 0 and
        carbs_per_100g >= 0
    );

alter table nutrition_records
    add constraint chk_nutrition_records_grams_positive check (grams > 0),
    add constraint chk_nutrition_records_totals_non_negative check (
        total_calories_kcal >= 0 and
        total_protein_g >= 0 and
        total_fat_g >= 0 and
        total_carbs_g >= 0
    );

-- 4) Enforce "belongs to same user" at DB level
alter table nutrition_records
    add constraint fk_nutrition_records_meal_type_user
        foreign key (meal_type_id, user_id)
            references meal_types (id, user_id)
            on delete restrict,
    add constraint fk_nutrition_records_food_item_user
        foreign key (food_item_id, user_id)
            references food_items (id, user_id)
            on delete restrict;

-- Keep old FK constraints for meal_type_id/food_item_id? Drop to avoid redundancy/conflicts.
alter table nutrition_records
    drop constraint if exists fk_nutrition_records_meal_type_id,
    drop constraint if exists fk_nutrition_records_food_item_id;

-- 5) Better indexes for common access paths
drop index if exists idx_nutrition_records_user_id;
drop index if exists idx_nutrition_records_consumed_at;
create index idx_nutrition_records_user_consumed_at_desc on nutrition_records (user_id, consumed_at desc);

-- fdc lookup is per-user
drop index if exists idx_food_items_fdc_id;
create index idx_food_items_user_fdc_id on food_items (user_id, fdc_id);

-- 6) Goals: convert values to numeric and add CHECK constraints for enums/date/value ranges
alter table user_goals
    alter column initial_value type numeric(12,2) using round(initial_value::numeric, 2),
    alter column target_value type numeric(12,2) using round(target_value::numeric, 2),
    alter column current_value type numeric(12,2) using round(current_value::numeric, 2);

alter table user_goal_progress
    alter column current_value type numeric(12,2) using round(current_value::numeric, 2);

alter table user_goals
    add constraint chk_user_goals_goal_type check (goal_type in (
        'WEIGHT_KG',
        'DAILY_CALORIES',
        'DAILY_PROTEIN_G',
        'DAILY_FAT_G',
        'DAILY_CARBS_G'
    )),
    add constraint chk_user_goals_status check (status in (
        'ACTIVE',
        'COMPLETED',
        'ARCHIVED'
    )),
    add constraint chk_user_goals_date_range check (target_date is null or target_date >= start_date),
    add constraint chk_user_goals_values_non_negative check (
        initial_value >= 0 and
        target_value >= 0 and
        current_value >= 0
    );

alter table user_goal_progress
    add constraint chk_user_goal_progress_value_non_negative check (current_value >= 0);

-- 7) Trigger: keep user_goals.current_value in sync with latest progress entry
create or replace function sync_user_goal_current_value(p_goal_id uuid)
returns void as $$
begin
    update user_goals ug
    set current_value = coalesce(
        (
            select ugp.current_value
            from user_goal_progress ugp
            where ugp.goal_id = p_goal_id
            order by ugp.recorded_at desc, ugp.id desc
            limit 1
        ),
        ug.initial_value
    ),
    updated_at = now() at time zone 'utc'
    where ug.id = p_goal_id;
end;
$$ language plpgsql;

create or replace function trg_user_goal_progress_sync_current_value()
returns trigger as $$
declare
    v_goal_id uuid;
begin
    v_goal_id := coalesce(new.goal_id, old.goal_id);
    perform sync_user_goal_current_value(v_goal_id);
    return coalesce(new, old);
end;
$$ language plpgsql;

drop trigger if exists trg_user_goal_progress_sync_after_insert on user_goal_progress;
drop trigger if exists trg_user_goal_progress_sync_after_update on user_goal_progress;
drop trigger if exists trg_user_goal_progress_sync_after_delete on user_goal_progress;

create trigger trg_user_goal_progress_sync_after_insert
after insert on user_goal_progress
for each row
execute function trg_user_goal_progress_sync_current_value();

create trigger trg_user_goal_progress_sync_after_update
after update of current_value, recorded_at on user_goal_progress
for each row
execute function trg_user_goal_progress_sync_current_value();

create trigger trg_user_goal_progress_sync_after_delete
after delete on user_goal_progress
for each row
execute function trg_user_goal_progress_sync_current_value();

