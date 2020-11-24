INSERT INTO DATASPACE (ID, NAME) VALUES
    (1001, 'DATASPACE-001');

INSERT INTO SCHEMA_SET (ID, NAME, DATASPACE_ID) VALUES
    (2001, 'SCHEMA-SET-001', 1001), (2002, 'SCHEMA-SET-002', 1001);

INSERT INTO ANCHOR (ID, NAME, DATASPACE_ID, SCHEMA_SET_ID) VALUES
    (3001, 'ANCHOR-001', 1001, 2001);