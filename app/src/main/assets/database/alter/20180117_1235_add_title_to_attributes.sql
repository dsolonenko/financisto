create table if not exists attributes_t (
	_id integer primary key autoincrement,
	type integer not null default 1,
	title text,
	list_values text,
	default_value text
);

INSERT INTO attributes_t (_id, type, title, list_values, default_value)
   SELECT _id, type, name, list_values, default_value FROM attributes;

DROP TABLE attributes;

ALTER TABLE attributes_t RENAME TO attributes;

