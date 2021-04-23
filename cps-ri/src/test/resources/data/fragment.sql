INSERT INTO DATASPACE (ID, NAME) VALUES
    (1001, 'DATASPACE-001');

INSERT INTO SCHEMA_SET (ID, NAME, DATASPACE_ID) VALUES
    (2001, 'SCHEMA-SET-001', 1001);

INSERT INTO ANCHOR (ID, NAME, DATASPACE_ID, SCHEMA_SET_ID) VALUES
    (3001, 'ANCHOR-001', 1001, 2001),
    (3003, 'ANCHOR-003', 1001, 2001);

INSERT INTO FRAGMENT (ID, DATASPACE_ID, ANCHOR_ID, PARENT_ID, XPATH) VALUES
    (4001, 1001, 3001, null, '/parent-1'),
    (4002, 1001, 3001, null, '/parent-2'),
    (4003, 1001, 3001, null, '/parent-3'),
    (4004, 1001, 3001, 4001, '/parent-1/child-1'),
    (4005, 1001, 3001, 4002, '/parent-2/child-2'),
    (4006, 1001, 3001, 4004, '/parent-1/child-1/grandchild-1');

INSERT INTO FRAGMENT (ID, DATASPACE_ID, ANCHOR_ID, PARENT_ID, XPATH, ATTRIBUTES) VALUES
    (4101, 1001, 3003, null, '/parent-100', '{"parent-leaf": "parent-leaf value"}'),
    (4102, 1001, 3003, 4101, '/parent-100/child-001', '{"first-child-leaf": "first-child-leaf value"}'),
    (4103, 1001, 3003, 4101, '/parent-100/child-002', '{"second-child-leaf": "second-child-leaf value"}'),
    (4104, 1001, 3003, 4103, '/parent-100/child-002/grand-child', '{"grand-child-leaf": "grand-child-leaf value"}');

INSERT INTO FRAGMENT (ID, DATASPACE_ID, ANCHOR_ID, PARENT_ID, XPATH, ATTRIBUTES) VALUES
    (4201, 1001, 3003, null, '/parent-200', '{"leaf-value": "original"}'),
    (4202, 1001, 3003, 4201, '/parent-200/child-201', '{"leaf-value": "original"}'),
    (4203, 1001, 3003, 4202, '/parent-200/child-201/grand-child', '{"leaf-value": "original"}'),
    (4204, 1001, 3003, 4201, '/parent-200/child-202', '{"common-leaf-name": "common-leaf value", "common-leaf-name-int" : 5}'),
    (4205, 1001, 3003, 4204, '/parent-200/child-202/grand-child-202', '{"common-leaf-name": "common-leaf value", "common-leaf-name-int" : 5}'),
    (4206, 1001, 3003, null, '/parent-201', '{"leaf-value": "original"}'),
    (4207, 1001, 3003, 4201, '/parent-201/child-202', '{"common-leaf-name": "common-leaf other value", "common-leaf-name-int" : 5}');