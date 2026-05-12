-- Store explicit daily calorie target in kcal (optional), keeping existing calorie_goal enum as "mode/strategy"

alter table user_profiles
    add column daily_calorie_target_kcal integer;

alter table user_profiles
    add constraint chk_user_profiles_daily_calorie_target_non_negative
        check (daily_calorie_target_kcal is null or daily_calorie_target_kcal >= 0);

