CREATE TABLE Template(
	id TEXT NOT NULL,
	schema_set TEXT NOT NULL,
	xpath_template TEXT NOT NULL,
	PRIMARY KEY(id, schema_set)
);
