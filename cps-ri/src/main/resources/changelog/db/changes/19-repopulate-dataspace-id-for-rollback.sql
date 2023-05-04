UPDATE
    fragment AS f
SET
    dataspace_id = a.dataspace_id
FROM
    anchor AS a
WHERE
    f.anchor_id = a.id;
