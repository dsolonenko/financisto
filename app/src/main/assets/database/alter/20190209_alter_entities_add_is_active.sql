ALTER TABLE payee ADD COLUMN is_active boolean not null default 1;
ALTER TABLE locations ADD COLUMN is_active boolean not null default 1;
ALTER TABLE attributes ADD COLUMN is_active boolean not null default 1;
ALTER TABLE category ADD COLUMN is_active boolean not null default 1;
ALTER TABLE currency ADD COLUMN is_active boolean not null default 1;
ALTER TABLE sms_template ADD COLUMN is_active boolean not null default 1;