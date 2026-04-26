create table meal_types (
    id uuid primary key,
    user_id uuid not null,
    name varchar(80) not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_meal_types_user_id foreign key (user_id) references users (id) on delete cascade,
    constraint uq_meal_types_user_name unique (user_id, name)
);

create index idx_meal_types_user_id on meal_types (user_id);

create table food_items (
    id uuid primary key,
    user_id uuid not null,
    name varchar(120) not null,
    source varchar(30),
    fdc_id bigint,
    calories_per_100g double precision not null,
    protein_per_100g double precision not null,
    fat_per_100g double precision not null,
    carbs_per_100g double precision not null,
    note varchar(500),
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_food_items_user_id foreign key (user_id) references users (id) on delete cascade,
    constraint uq_food_items_user_name unique (user_id, name)
);

create index idx_food_items_user_id on food_items (user_id);
create index idx_food_items_fdc_id on food_items (fdc_id);

create table nutrition_records (
    id uuid primary key,
    user_id uuid not null,
    meal_type_id uuid not null,
    food_item_id uuid not null,
    grams double precision not null,
    total_calories_kcal double precision not null,
    total_protein_g double precision not null,
    total_fat_g double precision not null,
    total_carbs_g double precision not null,
    note varchar(500),
    consumed_at timestamp with time zone not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_nutrition_records_user_id foreign key (user_id) references users (id) on delete cascade,
    constraint fk_nutrition_records_meal_type_id foreign key (meal_type_id) references meal_types (id) on delete restrict,
    constraint fk_nutrition_records_food_item_id foreign key (food_item_id) references food_items (id) on delete restrict
);

create index idx_nutrition_records_user_id on nutrition_records (user_id);
create index idx_nutrition_records_consumed_at on nutrition_records (consumed_at);
create index idx_nutrition_records_meal_type_id on nutrition_records (meal_type_id);
create index idx_nutrition_records_food_item_id on nutrition_records (food_item_id);

