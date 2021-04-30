INSERT INTO DATASPACE (ID, NAME) VALUES
    (1001, 'DATASPACE-001');

INSERT INTO SCHEMA_SET (ID, NAME, DATASPACE_ID) VALUES
    (2001, 'SCHEMA-SET-001', 1001);

INSERT INTO ANCHOR (ID, NAME, DATASPACE_ID, SCHEMA_SET_ID) VALUES
    (1003, 'ANCHOR-003', 1001, 2001);

INSERT INTO FRAGMENT (ID, DATASPACE_ID, ANCHOR_ID, PARENT_ID, XPATH, ATTRIBUTES) VALUES
    (1, 1001, 1003, null, '/shops', null),
    (2, 1001, 1003, 1, '/shops/shop[@id=1]', '{"id" : 1, "type" : "bookstore"}'),
    (3, 1001, 1003, 2, '/shops/shop[@id=1]/categories[@code=1]', '{"code" : 1, "type" : "bookstore", "name": "SciFi"}'),
    (4, 1001, 1003, 2, '/shops/shop[@id=1]/categories[@code=2]', '{"code" : 2, "type" : "bookstore", "name": "Fiction"}'),
    (5, 1001, 1003, 3, '/shops/shop[@id=1]/categories[@code=1]/book', '{"price" : 15, "title": "Dune"}'),
    (6, 1001, 1003, 4, '/shops/shop[@id=1]/categories[@code=2]/book', '{"price" : 15, "title": "Chapters"}'),
    (7, 1001, 1003, 5, '/shops/shop[@id=1]/categories[@code=1]/book/author[@FirstName="Joe" and @Surname="Bloggs"]', '{"FirstName" : "Joe", "Surname": "Bloggs","title": "Dune"}'),
    (8, 1001, 1003, 6, '/shops/shop[@id=1]/categories[@code=2]/book/author[@FirstName="Joe" and @Surname="Smith"]', '{"FirstName" : "Joe", "Surname": "Smith","title": "Chapters"}');

    INSERT INTO FRAGMENT (ID, DATASPACE_ID, ANCHOR_ID, PARENT_ID, XPATH, ATTRIBUTES) VALUES
    (9, 1001, 1003, 1, '/shops/shop[@id=2]', '{"type" : "bookstore"}'),
    (10, 1001, 1003, 9, '/shops/shop[@id=2]/categories[@code=1]', '{"code" : 2, "type" : "bookstore", "name": "Kids"}'),
    (11, 1001, 1003, 10, '/shops/shop[@id=2]/categories[@code=2]', '{"code" : 2, "type" : "bookstore", "name": "Fiction"}');

    INSERT INTO FRAGMENT (ID, DATASPACE_ID, ANCHOR_ID, PARENT_ID, XPATH, ATTRIBUTES) VALUES
    (12, 1001, 1003, 1, '/shops/shop[@id=3]', '{"type" : "garden centre"}'),
    (13, 1001, 1003, 12, '/shops/shop[@id=3]/categories[@code=1]', '{"id" : 1, "type" : "garden centre", "name": "indoor plants"}'),
    (14, 1001, 1003, 12, '/shops/shop[@id=3]/categories[@code=2]', '{"id" : 2, "type" : "garden centre", "name": "outdoor plants"}');