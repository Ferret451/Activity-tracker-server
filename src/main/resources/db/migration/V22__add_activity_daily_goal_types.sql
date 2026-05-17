alter table user_goals
    drop constraint chk_user_goals_goal_type;

alter table user_goals
    add constraint chk_user_goals_goal_type check (goal_type in (
        'WEIGHT_KG',
        'DAILY_CALORIES',
        'DAILY_PROTEIN_G',
        'DAILY_FAT_G',
        'DAILY_CARBS_G',
        'DAILY_STEPS',
        'DAILY_WALKING_MINUTES',
        'DAILY_BURNED_CALORIES_KCAL'
    ));
