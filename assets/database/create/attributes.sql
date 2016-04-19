create table if not exists attributes (
	_id integer primary key autoincrement, 
	type integer not null default 1,
	name text not null,
	list_values text,
	default_value text
);

create table if not exists category_attribute (
	category_id integer not null,
	attribute_id integer not null
);

create index if not exists category_attr_idx ON category_attribute (category_id);

create table if not exists transaction_attribute (
	transaction_id integer not null,
	attribute_id integer not null,
	value text
);

create index if not exists transaction_attr_idx ON transaction_attribute (transaction_id);