ALTER TABLE budget ADD COLUMN recur text;

ALTER TABLE budget ADD COLUMN recur_num integer not null default 0;

ALTER TABLE budget ADD COLUMN is_current integer not null default 1;

ALTER TABLE budget ADD COLUMN parent_budget_id long not null default 0;