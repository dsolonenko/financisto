CREATE VIEW v_blotter_for_account AS 
SELECT *
FROM v_blotter_for_account_with_splits
WHERE is_template=0 AND parent_id=0;