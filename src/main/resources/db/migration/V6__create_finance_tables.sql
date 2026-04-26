create table finance_categories (
    id uuid primary key,
    user_id uuid not null,
    name varchar(80) not null,
    operation_type varchar(20) not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_finance_categories_user_id foreign key (user_id) references users (id) on delete cascade,
    constraint uq_finance_categories_user_type_name unique (user_id, operation_type, name)
);

create index idx_finance_categories_user_id on finance_categories (user_id);
create index idx_finance_categories_operation_type on finance_categories (operation_type);

create table finance_records (
    id uuid primary key,
    user_id uuid not null,
    category_id uuid not null,
    operation_type varchar(20) not null,
    amount numeric(14, 2) not null,
    note varchar(500),
    occurred_at timestamp with time zone not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_finance_records_user_id foreign key (user_id) references users (id) on delete cascade,
    constraint fk_finance_records_category_id foreign key (category_id) references finance_categories (id) on delete restrict
);

create index idx_finance_records_user_id on finance_records (user_id);
create index idx_finance_records_occurred_at on finance_records (occurred_at);
create index idx_finance_records_category_id on finance_records (category_id);
create index idx_finance_records_operation_type on finance_records (operation_type);

