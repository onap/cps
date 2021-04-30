INSERT INTO DATASPACE (ID, NAME) VALUES
    (1001, 'DATASPACE-001');

INSERT INTO SCHEMA_SET (ID, NAME, DATASPACE_ID) VALUES
    (2001, 'SCHEMA-SET-001', 1001);

INSERT INTO ANCHOR (ID, NAME, DATASPACE_ID, SCHEMA_SET_ID) VALUES
    (3003, 'ANCHOR-003', 1001, 2001);

INSERT INTO FRAGMENT (ID, DATASPACE_ID, ANCHOR_ID, PARENT_ID, XPATH, ATTRIBUTES) VALUES
    (1, 1001, 3003, null, '/bookstore', null),
    (2, 1001, 3003, 1, '/bookstore/bookID', '{"volume": "6", "id" : 1}'),
    (3, 1001, 3003, 2, '/bookstore/bookID/categories[@genre="Kids"]', '{"id": 1,"genre": "Kids"}'),
    (4, 1001, 3003, 2, '/bookstore/bookID/categories[@genre="SciFi"]', '{"id": 2,"genre": "SciFi"}'),
    (5, 1001, 3003, 3, '/bookstore/bookID/categories[@genre="Kids"]/book', '{"id": 1,"genre": "Kids", "lang" : "en"}'),
    (6, 1001, 3003, 4, '/bookstore/bookID/categories[@genre="SciFi"]/book', '{"id": 2,"genre": "SciFi", "lang" : "en"}'),
    (7, 1001, 3003, 5, '/bookstore/bookID/categories[@genre="Kids"]/book/info[@title="The Golden Compass" and @price=15]','{"id": 2, "genre": "Kids", "title": "The Golden Compass","price": 15}'),
    (8, 1001, 3003, 6, '/bookstore/bookID/categories[@genre="SciFi"]/book/info[@title="Feersum Endjinn" and @price=20]','{"id": 1, "genre": "SciFi", "title": "Feersum Endjinn", "price": 20}');