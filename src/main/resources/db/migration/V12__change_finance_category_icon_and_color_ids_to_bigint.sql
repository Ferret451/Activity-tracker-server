DROP INDEX IF EXISTS idx_finance_categories_icon_id;
DROP INDEX IF EXISTS idx_finance_categories_color_id;

ALTER TABLE finance_categories
    ALTER COLUMN icon_id TYPE BIGINT USING NULL,
    ALTER COLUMN color_id TYPE BIGINT USING NULL;

CREATE INDEX idx_finance_categories_icon_id ON finance_categories (icon_id);
CREATE INDEX idx_finance_categories_color_id ON finance_categories (color_id);

