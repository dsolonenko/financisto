create table if not exists account ( 
	_id integer primary key autoincrement, 
	title text not null, 
	creation_date long not null,
	currency_id integer not null,
	total_amount integer not null default 0
);
