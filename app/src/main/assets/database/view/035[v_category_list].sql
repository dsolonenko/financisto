CREATE VIEW v_category_list AS
SELECT 
	B._id AS parent_id, 
	B.title AS parent_title, 
	B.left AS parent_left, 
	B.right AS parent_right, 
	B.type AS parent_type,
	P._id as _id,
	P.title AS title, 
	P.left as left,
	P.right as right,
	P.type as type
FROM category AS B, category AS P
WHERE P.left BETWEEN B.left AND B.right
AND B._id = (SELECT MAX(S._id)
FROM category AS S
WHERE S.left < P.left
AND S.right > P.right);
