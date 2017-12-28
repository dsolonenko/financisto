CREATE VIEW v_blotter_flatsplits AS
SELECT *
FROM v_all_transactions
WHERE is_template = 0 and _id not in (SELECT DISTINCT parent_id FROM transactions WHERE is_template=0 AND parent_id > 0);