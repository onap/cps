INSERT INTO DATASPACE (ID, NAME) VALUES
    (1001, 'dataspace-001'),
    (1002, 'dataspace-002');

INSERT INTO MODULESET (ID, NAME, DATASPACE_ID) VALUES
    (2001, 'moduleset-001', 1001),
    (2002, 'moduleset-002', 1001);

INSERT INTO MODULE (ID, DATASPACE_ID, MODULESET_ID, NAMESPACE, REVISION, MODULE_CONTENT) VALUES
    (3001, 1001, 2001, 'http://cps.onap.org/test/', '2020-01-01', 'CONTENT-001'),
    (3002, 1001, 2001, 'http://cps.onap.org/test/', '2020-02-02', 'CONTENT-002'),
    (3003, 1001, 2002, 'http://cps.onap.org/test/', '2020-03-03', 'CONTENT-003'),
    (3004, 1001, 2002, 'http://cps.onap.org/test/', '2020-04-04', 'CONTENT-004');

