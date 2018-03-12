ALTER TABLE sms_template ADD COLUMN sort_order integer not null default 0;
update sms_template set sort_order=_id;