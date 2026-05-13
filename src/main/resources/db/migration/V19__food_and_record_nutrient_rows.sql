-- Additional nutrients per 100g on user food catalog items (USDA-style list, excluding energy/macros stored on food_items)

create table food_item_nutrients (
    id uuid primary key,
    food_item_id uuid not null,
    nutrient_number varchar(16),
    name varchar(200) not null,
    unit_name varchar(32),
    amount_per_100g numeric(10, 2) not null,
    constraint fk_food_item_nutrients_food_item_id foreign key (food_item_id) references food_items (id) on delete cascade,
    constraint chk_food_item_nutrients_amount_non_negative check (amount_per_100g >= 0)
);

create index idx_food_item_nutrients_food_item_id on food_item_nutrients (food_item_id);

create unique index uq_food_item_nutrients_food_key on food_item_nutrients (
    food_item_id,
    coalesce(nutrient_number, ''),
    lower(trim(name))
);

-- Snapshot of additional nutrients for a single consumed portion (mirrors denormalized totals on nutrition_records)

create table nutrition_record_nutrients (
    id uuid primary key,
    nutrition_record_id uuid not null,
    nutrient_number varchar(16),
    name varchar(200) not null,
    unit_name varchar(32),
    total_amount numeric(10, 2) not null,
    constraint fk_nutrition_record_nutrients_record_id foreign key (nutrition_record_id) references nutrition_records (id) on delete cascade,
    constraint chk_nutrition_record_nutrients_total_non_negative check (total_amount >= 0)
);

create index idx_nutrition_record_nutrients_record_id on nutrition_record_nutrients (nutrition_record_id);

create unique index uq_nutrition_record_nutrients_record_key on nutrition_record_nutrients (
    nutrition_record_id,
    coalesce(nutrient_number, ''),
    lower(trim(name))
);
