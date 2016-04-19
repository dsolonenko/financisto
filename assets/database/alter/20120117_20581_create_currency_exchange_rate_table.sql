create table if not exists currency_exchange_rate (
	from_currency_id integer not null,
	to_currency_id integer not null,
	rate_date long not null,
	rate float not null,
	PRIMARY KEY (from_currency_id, to_currency_id, rate_date)
);