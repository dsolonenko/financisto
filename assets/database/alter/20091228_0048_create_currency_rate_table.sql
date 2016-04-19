create table if not exists currency_rate (
	from_currency_id integer not null,
	to_currency_id integer not null,
	rate double not null,
	effective_date long not null
);
	
