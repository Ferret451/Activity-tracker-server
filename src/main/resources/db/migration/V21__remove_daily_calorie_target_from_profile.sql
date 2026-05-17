alter table user_profiles
    drop constraint if exists chk_user_profiles_daily_calorie_target_non_negative;

alter table user_profiles
    drop column if exists daily_calorie_target_kcal;
