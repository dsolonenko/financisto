create table if not exists currency (
	_id integer primary key autoincrement,
	name text not null,
	title text not null,
	symbol text not null
);
	
