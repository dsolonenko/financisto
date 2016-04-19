ALTER TABLE account ADD COLUMN last_category_id long not null default 0;

ALTER TABLE category ADD COLUMN last_location_id long not null default 0;

ALTER TABLE category ADD COLUMN last_project_id long not null default 0;