create view v_transaction_attributes AS 
SELECT
	t._id as _id,
	a._id as attribute_id, 
	a.type as attribute_type,
	a.name as attribute_name,
	a.list_values as attribute_list_values,
	a.default_value as attribute_default_value,
	ta.value as attribute_value
FROM
	transactions t
	INNER JOIN transaction_attribute ta ON ta.transaction_id=t._id
	INNER JOIN attributes a ON a._id=ta.attribute_id
ORDER BY 
	a.name;