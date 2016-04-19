create table if not exists budget ( 
	_id integer primary key autoincrement,
	title text,
	category_id long not null,		
	currency_id long not null,
	amount integer not null,
	include_subcategories integer not null default 1,
	start_date long,
	end_date long,
	repeat integer
);

