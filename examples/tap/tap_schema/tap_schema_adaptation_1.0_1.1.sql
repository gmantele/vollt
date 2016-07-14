-- SQL Script based on TAP 1.1 (http://www.ivoa.net/documents/TAP/20160428/WD-TAP-1.1-20160428.pdf)
-- 
-- DESCRIPTION:
--     This script adds new columns (i.e. arraysize, table_index and column_index) in the existing tables
--     of TAP_SCHEMA and adds corresponding entries in TAP_SCHEMA.columns. After execution of this script,
--     the TAP_SCHEMA should be entirely compatible with a TAP 1.1 service.
-- 
--     The whole script is executed in a transaction in order to avoid partial modification of TAP_SCHEMA
--     in case of an SQL error.
-- 
-- NOTE:
--     Tested and designed for PostgreSQL ; it may work with some modifications for other DBMS
-- 
-- AUTHOR: Grégory Mantelet (ARI)
-- DATE  : July 2016


BEGIN;

-- 1. ADD arraysize

ALTER TABLE "TAP_SCHEMA"."columns" ADD COLUMN "arraysize" INTEGER;
	
INSERT INTO "TAP_SCHEMA"."columns" ("table_name", "column_name", "datatype", "arraysize", "size", "description", "utype", "unit", "ucd", "indexed", "principal", "std")
	VALUES ('TAP_SCHEMA.columns', 'arraysize', 'INTEGER' , -1, -1, 'length of variable length datatypes', NULL, NULL, NULL, 0, 0, 1);

UPDATE "TAP_SCHEMA"."columns" SET "arraysize" = "size";

-- 2. DOUBLE QUOTE size

UPDATE "TAP_SCHEMA"."columns" SET "column_name" = '"size"' WHERE LOWER("table_name") IN ('tap_schema.columns', 'columns') AND LOWER("column_name") = 'size';

-- 3. ADD column_index

ALTER TABLE "TAP_SCHEMA"."columns" ADD COLUMN "column_index" INTEGER DEFAULT -1;

INSERT INTO "TAP_SCHEMA"."columns" ("table_name", "column_name", "datatype", "arraysize", "size", "description", "utype", "unit", "ucd", "indexed", "principal", "std", "column_index")
	VALUES ('TAP_SCHEMA.columns', 'column_index', 'INTEGER', -1, -1, 'this index is used to recommend column ordering for clients' , NULL, NULL, NULL, 0, 0, 1, 0);

UPDATE "TAP_SCHEMA"."columns" SET "column_index" = 0 WHERE LOWER("table_name") IN ('tap_schema.schemas', 'schemas') AND LOWER("column_name") = 'schema_name';
UPDATE "TAP_SCHEMA"."columns" SET "column_index" = 1 WHERE LOWER("table_name") IN ('tap_schema.schemas', 'schemas') AND LOWER("column_name") = 'description';
UPDATE "TAP_SCHEMA"."columns" SET "column_index" = 2 WHERE LOWER("table_name") IN ('tap_schema.schemas', 'schemas') AND LOWER("column_name") = 'utype';

UPDATE "TAP_SCHEMA"."columns" SET "column_index" = 1 WHERE LOWER("table_name") IN ('tap_schema.tables', 'tables') AND LOWER("column_name") = 'schema_name';
UPDATE "TAP_SCHEMA"."columns" SET "column_index" = 2 WHERE LOWER("table_name") IN ('tap_schema.tables', 'tables') AND LOWER("column_name") = 'table_name';
UPDATE "TAP_SCHEMA"."columns" SET "column_index" = 3 WHERE LOWER("table_name") IN ('tap_schema.tables', 'tables') AND LOWER("column_name") = 'table_type';
UPDATE "TAP_SCHEMA"."columns" SET "column_index" = 4 WHERE LOWER("table_name") IN ('tap_schema.tables', 'tables') AND LOWER("column_name") = 'description';
UPDATE "TAP_SCHEMA"."columns" SET "column_index" = 5 WHERE LOWER("table_name") IN ('tap_schema.tables', 'tables') AND LOWER("column_name") = 'utype';

UPDATE "TAP_SCHEMA"."columns" SET "column_index" = 1 WHERE LOWER("table_name") IN ('tap_schema.columns', 'columns') AND LOWER("column_name") = 'table_name';
UPDATE "TAP_SCHEMA"."columns" SET "column_index" = 2 WHERE LOWER("table_name") IN ('tap_schema.columns', 'columns') AND LOWER("column_name") = 'column_name';
UPDATE "TAP_SCHEMA"."columns" SET "column_index" = 3 WHERE LOWER("table_name") IN ('tap_schema.columns', 'columns') AND LOWER("column_name") = 'datatype';
UPDATE "TAP_SCHEMA"."columns" SET "column_index" = 4 WHERE LOWER("table_name") IN ('tap_schema.columns', 'columns') AND LOWER("column_name") = 'arraysize';
UPDATE "TAP_SCHEMA"."columns" SET "column_index" = 5 WHERE LOWER("table_name") IN ('tap_schema.columns', 'columns') AND LOWER("column_name") = '"size"';
UPDATE "TAP_SCHEMA"."columns" SET "column_index" = 6 WHERE LOWER("table_name") IN ('tap_schema.columns', 'columns') AND LOWER("column_name") = 'description';
UPDATE "TAP_SCHEMA"."columns" SET "column_index" = 7 WHERE LOWER("table_name") IN ('tap_schema.columns', 'columns') AND LOWER("column_name") = 'utype';
UPDATE "TAP_SCHEMA"."columns" SET "column_index" = 8 WHERE LOWER("table_name") IN ('tap_schema.columns', 'columns') AND LOWER("column_name") = 'unit';
UPDATE "TAP_SCHEMA"."columns" SET "column_index" = 9 WHERE LOWER("table_name") IN ('tap_schema.columns', 'columns') AND LOWER("column_name") = 'ucd';
UPDATE "TAP_SCHEMA"."columns" SET "column_index" = 10 WHERE LOWER("table_name") IN ('tap_schema.columns', 'columns') AND LOWER("column_name") = 'indexed';
UPDATE "TAP_SCHEMA"."columns" SET "column_index" = 11 WHERE LOWER("table_name") IN ('tap_schema.columns', 'columns') AND LOWER("column_name") = 'principal';
UPDATE "TAP_SCHEMA"."columns" SET "column_index" = 12 WHERE LOWER("table_name") IN ('tap_schema.columns', 'columns') AND LOWER("column_name") = 'std';

UPDATE "TAP_SCHEMA"."columns" SET "column_index" = 0 WHERE LOWER("table_name") IN ('tap_schema.keys', 'keys') AND LOWER("column_name") = 'key_id';
UPDATE "TAP_SCHEMA"."columns" SET "column_index" = 1 WHERE LOWER("table_name") IN ('tap_schema.keys', 'keys') AND LOWER("column_name") = 'from_table';
UPDATE "TAP_SCHEMA"."columns" SET "column_index" = 2 WHERE LOWER("table_name") IN ('tap_schema.keys', 'keys') AND LOWER("column_name") = 'target_table';
UPDATE "TAP_SCHEMA"."columns" SET "column_index" = 3 WHERE LOWER("table_name") IN ('tap_schema.keys', 'keys') AND LOWER("column_name") = 'description';
UPDATE "TAP_SCHEMA"."columns" SET "column_index" = 4 WHERE LOWER("table_name") IN ('tap_schema.keys', 'keys') AND LOWER("column_name") = 'utype';

UPDATE "TAP_SCHEMA"."columns" SET "column_index" = 0 WHERE LOWER("table_name") IN ('tap_schema.key_columns', 'key_columns') AND LOWER("column_name") = 'key_id';
UPDATE "TAP_SCHEMA"."columns" SET "column_index" = 1 WHERE LOWER("table_name") IN ('tap_schema.key_columns', 'key_columns') AND LOWER("column_name") = 'from_column';
UPDATE "TAP_SCHEMA"."columns" SET "column_index" = 2 WHERE LOWER("table_name") IN ('tap_schema.key_columns', 'key_columns') AND LOWER("column_name") = 'target_column';

-- 4. ADD table_index

ALTER TABLE "TAP_SCHEMA"."tables" ADD COLUMN "table_index" INTEGER DEFAULT -1;

INSERT INTO "TAP_SCHEMA"."columns" ("table_name", "column_name", "datatype", "arraysize", "size", "description", "utype", "unit", "ucd", "indexed", "principal", "std", "column_index")
	VALUES ('TAP_SCHEMA.tables', 'table_index', 'INTEGER', -1, -1, 'this index is used to recommend table ordering for clients', NULL, NULL, NULL, 0, 0, 1, 0);

UPDATE "TAP_SCHEMA"."tables" SET "table_index" = (SELECT COUNT(*) FROM "TAP_SCHEMA"."tables")-5 WHERE LOWER("schema_name") = 'tap_schema' AND LOWER("table_name") IN ('schemas', 'tap_schema.schemas');
UPDATE "TAP_SCHEMA"."tables" SET "table_index" = (SELECT COUNT(*) FROM "TAP_SCHEMA"."tables")-4 WHERE LOWER("schema_name") = 'tap_schema' AND LOWER("table_name") IN ('tables', 'tap_schema.tables');
UPDATE "TAP_SCHEMA"."tables" SET "table_index" = (SELECT COUNT(*) FROM "TAP_SCHEMA"."tables")-3 WHERE LOWER("schema_name") = 'tap_schema' AND LOWER("table_name") IN ('columns', 'tap_schema.columns');
UPDATE "TAP_SCHEMA"."tables" SET "table_index" = (SELECT COUNT(*) FROM "TAP_SCHEMA"."tables")-2 WHERE LOWER("schema_name") = 'tap_schema' AND LOWER("table_name") IN ('keys', 'tap_schema.keys');
UPDATE "TAP_SCHEMA"."tables" SET "table_index" = (SELECT COUNT(*) FROM "TAP_SCHEMA"."tables")-1 WHERE LOWER("schema_name") = 'tap_schema' AND LOWER("table_name") IN ('key_columns', 'tap_schema.key_columns');

COMMIT;
