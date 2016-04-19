
CREATE INDEX idx_key_act ON account (remote_key);
CREATE INDEX idx_key_cat ON category (remote_key);
CREATE INDEX idx_key_pro ON project (remote_key);
CREATE INDEX idx_key_payee ON payee (remote_key);
CREATE INDEX idx_key_cur ON currency (remote_key);
CREATE INDEX idx_key_loc ON LOCATIONS (remote_key);
