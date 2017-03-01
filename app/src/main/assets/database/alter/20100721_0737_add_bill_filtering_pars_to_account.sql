ALTER TABLE account ADD COLUMN closing_day integer not null default 0;
ALTER TABLE account ADD COLUMN payment_day integer not null default 0;
ALTER TABLE transactions ADD COLUMN is_ccard_payment integer not null default 0;