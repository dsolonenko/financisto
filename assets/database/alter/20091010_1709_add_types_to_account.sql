ALTER TABLE account ADD COLUMN type text not null default 'CASH';
ALTER TABLE account ADD COLUMN issuer text;
ALTER TABLE account ADD COLUMN number text;