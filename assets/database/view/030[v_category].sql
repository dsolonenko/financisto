create view v_category AS 
SELECT 
	node._id as _id,
	node.title as title,
	node.left as left,
	node.right as right,
	node.type as type,
	node.last_location_id as last_location_id,
	node.last_project_id as last_project_id,
	node.sort_order as sort_order,
	count(parent._id)-1 as level
FROM
	category as node,
	category as parent
WHERE node.left BETWEEN parent.left AND parent.right
GROUP BY node._id ORDER BY node.left;
	
