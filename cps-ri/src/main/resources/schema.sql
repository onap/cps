CREATE TABLE IF NOT EXISTS RELATION_TYPE
(
    RELATION_TYPE TEXT NOT NULL,
    ID SERIAL PRIMARY KEY
);

CREATE TABLE IF NOT EXISTS DATASPACE
(
    ID SERIAL PRIMARY KEY,
    NAME TEXT NOT NULL,
    CONSTRAINT "UQ_NAME" UNIQUE (NAME)
);

CREATE TABLE IF NOT EXISTS SCHEMA_NODE
(
    SCHEMA_NODE_IDENTIFIER TEXT NOT NULL,
    ID SERIAL PRIMARY KEY
);

CREATE TABLE IF NOT EXISTS MODULE
(
    NAMESPACE TEXT NOT NULL,
    REVISION TEXT NOT NULL,
    MODULE_CONTENT TEXT NOT NULL,
    DATASPACE_ID BIGINT NOT NULL,
    ID SERIAL PRIMARY KEY,
    UNIQUE (DATASPACE_ID, NAMESPACE, REVISION),
    CONSTRAINT module_dataspace FOREIGN KEY (DATASPACE_ID) REFERENCES DATASPACE (id) ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS FRAGMENT
(
    ID BIGSERIAL PRIMARY KEY,
    XPATH TEXT NOT NULL,
    DATASPACE_ID INTEGER NOT NULL REFERENCES DATASPACE(ID),
    ATTRIBUTES JSONB,
    ANCHOR_ID BIGINT REFERENCES FRAGMENT(ID),
    PARENT_ID BIGINT REFERENCES FRAGMENT(ID),
    MODULE_ID INTEGER REFERENCES MODULE(ID),
    SCHEMA_NODE_ID INTEGER REFERENCES SCHEMA_NODE(ID)
);

CREATE TABLE IF NOT EXISTS RELATION
(
    FROM_FRAGMENT_ID BIGINT NOT NULL REFERENCES FRAGMENT(ID),
    TO_FRAGMENT_ID   BIGINT NOT NULL REFERENCES FRAGMENT(ID),
    RELATION_TYPE_ID  INTEGER NOT NULL REFERENCES RELATION_TYPE(ID),
    FROM_REL_XPATH TEXT NOT NULL,
    TO_REL_XPATH TEXT NOT NULL,
    CONSTRAINT RELATION_PKEY PRIMARY KEY (TO_FRAGMENT_ID, FROM_FRAGMENT_ID, RELATION_TYPE_ID)
);


CREATE INDEX  IF NOT EXISTS "FKI_FRAGMENT_DATASPACE_ID_FK"     ON FRAGMENT USING BTREE(DATASPACE_ID) ;
CREATE INDEX  IF NOT EXISTS "FKI_FRAGMENT_MODULE_ID_FK"        ON FRAGMENT USING BTREE(MODULE_ID) ;
CREATE INDEX  IF NOT EXISTS "FKI_FRAGMENT_PARENT_ID_FK"        ON FRAGMENT USING BTREE(PARENT_ID) ;
CREATE INDEX  IF NOT EXISTS "FKI_FRAGMENT_ANCHOR_ID_FK"        ON FRAGMENT USING BTREE(ANCHOR_ID) ;
CREATE INDEX  IF NOT EXISTS "PERF_SCHEMA_NODE_SCHEMA_NODE_ID"  ON SCHEMA_NODE USING BTREE(SCHEMA_NODE_IDENTIFIER) ;
CREATE INDEX  IF NOT EXISTS "FKI_SCHEMA_NODE_ID_TO_ID"         ON FRAGMENT USING BTREE(SCHEMA_NODE_ID) ;
CREATE INDEX  IF NOT EXISTS "FKI_RELATION_TYPE_ID_FK"          ON RELATION USING BTREE(RELATION_TYPE_ID);
CREATE INDEX  IF NOT EXISTS "FKI_RELATIONS_FROM_ID_FK"         ON RELATION USING BTREE(FROM_FRAGMENT_ID);
CREATE INDEX  IF NOT EXISTS "FKI_RELATIONS_TO_ID_FK"           ON RELATION USING BTREE(TO_FRAGMENT_ID);
CREATE INDEX  IF NOT EXISTS "PERF_MODULE_MODULE_CONTENT"       ON MODULE USING BTREE(MODULE_CONTENT);
CREATE UNIQUE INDEX  IF NOT EXISTS "UQ_FRAGMENT_XPATH"ON FRAGMENT USING btree(xpath COLLATE pg_catalog."default" text_pattern_ops, dataspace_id);