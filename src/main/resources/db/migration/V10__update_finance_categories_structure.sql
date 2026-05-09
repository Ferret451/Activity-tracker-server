-- Add icon_id and color_id columns to finance_categories
alter table finance_categories
    add column icon_id uuid;

alter table finance_categories
    add column color_id uuid;

create index idx_finance_categories_icon_id on finance_categories (icon_id);
create index idx_finance_categories_color_id on finance_categories (color_id);

