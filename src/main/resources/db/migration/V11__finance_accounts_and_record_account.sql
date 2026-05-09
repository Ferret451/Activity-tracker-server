CREATE TABLE finance_accounts (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    name VARCHAR(120) NOT NULL,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    balance NUMERIC(14, 2) NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_finance_accounts_user_id FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX uq_finance_accounts_one_primary_per_user
    ON finance_accounts (user_id)
    WHERE is_primary;

CREATE INDEX idx_finance_accounts_user_id ON finance_accounts (user_id);

ALTER TABLE finance_records
    ADD COLUMN account_id UUID CONSTRAINT fk_finance_records_account_id REFERENCES finance_accounts (id);

INSERT INTO finance_accounts (id, user_id, name, is_primary, balance, created_at, updated_at)
SELECT gen_random_uuid(), d.user_id, 'Основной', TRUE, 0, NOW(), NOW()
FROM (SELECT DISTINCT user_id FROM finance_records) d;

UPDATE finance_records fr
SET account_id = fa.id
FROM finance_accounts fa
WHERE fr.user_id = fa.user_id AND fa.is_primary = TRUE;

UPDATE finance_accounts fa
SET balance = agg.signed_total
FROM (
    SELECT fr.account_id,
           SUM(CASE WHEN fr.operation_type = 'INCOME' THEN fr.amount ELSE -fr.amount END) AS signed_total
    FROM finance_records fr
    WHERE fr.account_id IS NOT NULL
    GROUP BY fr.account_id
) agg
WHERE fa.id = agg.account_id;

ALTER TABLE finance_records
    ALTER COLUMN account_id SET NOT NULL;

CREATE INDEX idx_finance_records_account_id ON finance_records (account_id);
CREATE INDEX idx_finance_records_user_occurred_at ON finance_records (user_id, occurred_at DESC);
