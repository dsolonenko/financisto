create table if not exists transactions ( 
	_id integer primary key autoincrement,
	from_account_id long not null,
	to_account_id long not null default 0,
	category_id long not null default 0,
	project_id long not null default 0,
	location_id long not null default 0,
	note text,
	from_amount integer not null default 0,
	to_amount integer not null default 0,
	datetime long not null,
	provider text,
	accuracy float,
	latitude double,
	longitude double
);
	
create index if not exists transaction_from_act_idx ON transactions (from_account_id);

create index if not exists transaction_to_act_idx ON transactions (to_account_id);

create index if not exists transaction_dt_idx ON transactions (datetime desc);