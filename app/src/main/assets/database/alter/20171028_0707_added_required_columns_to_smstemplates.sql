ALTER TABLE sms_template ADD COLUMN updated_on TIMESTAMP DEFAULT 0;
ALTER TABLE sms_template ADD COLUMN remote_key text;