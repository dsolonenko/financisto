ALTER TABLE budget ADD COLUMN budget_account_id long;
ALTER TABLE budget ADD COLUMN budget_currency_id long;

UPDATE budget SET budget_currency_id = currency_id;