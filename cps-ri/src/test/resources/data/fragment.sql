INSERT INTO DATASPACE (ID, NAME) VALUES
    (1001, 'DATASPACE-001');

INSERT INTO SCHEMA_SET (ID, NAME, DATASPACE_ID) VALUES
    (2001, 'SCHEMA-SET-001', 1001);

INSERT INTO ANCHOR (ID, NAME, DATASPACE_ID, SCHEMA_SET_ID) VALUES
    (3001, 'ANCHOR-001', 1001, 2001),
    (3003, 'ANCHOR-003', 1001, 2001);

INSERT INTO FRAGMENT (ID, XPATH, ANCHOR_ID, PARENT_ID, DATASPACE_ID) VALUES
    (4001, '/parent-1', 3001, null, 1001),
    (4002, '/parent-2', 3001, null, 1001),
    (4003, '/parent-3', 3001, null, 1001),
    (4004, '/parent-1/child-1', 3001, 4001, 1001),
    (4005, '/parent-2/child-2', 3001, 4002, 1001),
    (4006, '/parent-1/child-1/grandchild-1', 3001, 4004, 1001);

INSERT INTO FRAGMENT (ID, DATASPACE_ID, ANCHOR_ID, PARENT_ID, XPATH, ATTRIBUTES) VALUES
    (4101, 1001, 3003, null, '/parent-100', '{"x": "y"}'),
    (4102, 1001, 3003, 4101, '/parent-100/child-001', '{"a": "b", "c": ["d", "e", "f"]}'),
    (4103, 1001, 3003, 4101, '/parent-100/child-002', '{"g": "h", "i": ["j", "k"]}'),
    (4104, 1001, 3003, 4103, '/parent-100/child-002/grand-child', '{"l": "m", "n": ["o", "p"]}');

INSERT INTO FRAGMENT (ID, DATASPACE_ID, ANCHOR_ID, PARENT_ID, XPATH, ATTRIBUTES) VALUES
    (4201, 1001, 3003, null, '/ran-inventory', '{"id": 1}'),
    (4202, 1001, 3003, 4201, '/ran-inventory/sliceProfilesList/', '{"id": 2, "sNSSAI": "a", "latency": 5}'),
    (4203, 1001, 3003, 4202, '/ran-inventory/sliceProfilesList/pLMNIdList', '{"id": 3, "mcc": 310, "mnc": 410}');