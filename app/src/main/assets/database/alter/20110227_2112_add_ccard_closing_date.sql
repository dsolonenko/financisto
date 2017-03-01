create table if not exists ccard_closing_date (
	account_id long not null,
	period integer not null,
	closing_day integer not null
);