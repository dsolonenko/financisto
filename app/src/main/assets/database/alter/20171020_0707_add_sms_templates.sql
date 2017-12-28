create table if not exists sms_template (
	_id integer primary key autoincrement,
	title text not null,
	template text not null,
	category_id integer not null,
	account_id integer
);

create index if not exists smstemplate_num_idx ON sms_template (title);