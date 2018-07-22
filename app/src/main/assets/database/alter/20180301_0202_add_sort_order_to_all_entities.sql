ALTER TABLE project ADD COLUMN sort_order integer not null default 0;
update project set sort_order=_id;

ALTER TABLE payee ADD COLUMN sort_order integer not null default 0;
update payee set sort_order=_id;

ALTER TABLE currency ADD COLUMN sort_order integer not null default 0;
update currency set sort_order=_id;

ALTER TABLE budget ADD COLUMN sort_order integer not null default 0;
update budget set sort_order=_id;

ALTER TABLE LOCATIONS ADD COLUMN sort_order integer not null default 0;
update LOCATIONS set sort_order=_id;

ALTER TABLE attributes ADD COLUMN sort_order integer not null default 0;
update attributes set sort_order=_id;

