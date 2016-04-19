create table if not exists category ( 
	_id integer primary key autoincrement,
	title text not null,
	left integer not null default 0,
	right integer not null default 0
);

create index if not exists category_left_idx ON category (left);