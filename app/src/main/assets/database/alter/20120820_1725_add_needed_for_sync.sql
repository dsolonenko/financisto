ALTER TABLE project ADD COLUMN updated_on TIMESTAMP DEFAULT 0;
ALTER TABLE project ADD COLUMN remote_key text;

ALTER TABLE payee ADD COLUMN updated_on TIMESTAMP DEFAULT 0;
ALTER TABLE payee ADD COLUMN remote_key text;

ALTER TABLE category ADD COLUMN updated_on TIMESTAMP DEFAULT 0;
ALTER TABLE category ADD COLUMN remote_key text;

ALTER TABLE account ADD COLUMN updated_on TIMESTAMP DEFAULT 0;
ALTER TABLE account ADD COLUMN remote_key text;

ALTER TABLE transactions ADD COLUMN updated_on TIMESTAMP DEFAULT 0;
ALTER TABLE transactions ADD COLUMN remote_key text;

ALTER TABLE currency ADD COLUMN updated_on TIMESTAMP DEFAULT 0;
ALTER TABLE currency ADD COLUMN remote_key text;

ALTER TABLE budget ADD COLUMN updated_on TIMESTAMP DEFAULT 0;
ALTER TABLE budget ADD COLUMN remote_key text;

ALTER TABLE LOCATIONS ADD COLUMN updated_on TIMESTAMP DEFAULT 0;
ALTER TABLE LOCATIONS ADD COLUMN remote_key text;

ALTER TABLE currency_exchange_rate ADD COLUMN updated_on TIMESTAMP DEFAULT 0;
ALTER TABLE currency_exchange_rate ADD COLUMN remote_key text;

CREATE TABLE IF NOT EXISTS delete_log ( table_name text, remote_key text, deleted_on TIMESTAMP);
