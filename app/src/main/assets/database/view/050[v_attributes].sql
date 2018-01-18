create view v_attributes AS 
SELECT
	a._id as _id,
	a.title as title,
	a.type as type,
	a.list_values as list_values,
	a.default_value as default_value,
	c._id as category_id,
	c.left as category_left,
	c.right as category_right
FROM
	attributes as a,
	category_attribute as ca,
	category c
WHERE
	ca.attribute_id=a._id
	AND ca.category_id=c._id;
	
