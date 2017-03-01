ALTER TABLE transactions ADD COLUMN is_template integer not null default 0;

CREATE INDEX IF NOT EXISTS idx_is_template ON transactions(is_template);