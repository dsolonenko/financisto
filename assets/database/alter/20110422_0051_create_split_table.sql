create table if not exists split (
	_id integer primary key autoincrement,
	transaction_id integer not null,
	category_id integer not null,
	amount integer not null,
	note text
);

create index if not exists split_txn_idx ON split (transaction_id);

create index if not exists split_cat_idx ON split (category_id);