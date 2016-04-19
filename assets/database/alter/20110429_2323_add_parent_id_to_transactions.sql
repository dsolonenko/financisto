ALTER TABLE transactions ADD COLUMN parent_id long not null default 0;

create index if not exists transaction_pid_idx ON transactions (parent_id);