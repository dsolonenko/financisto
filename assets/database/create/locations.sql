create table if not exists locations (
	_id integer primary key autoincrement, 
	name text not null,	
	datetime long not null,
	provider text,
	accuracy float,
	latitude double,
	longitude double,
	is_payee integer not null default 0,
	resolved_address text
);