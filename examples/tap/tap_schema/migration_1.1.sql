-- SQL Script based on TAP 1.1 (http://www.ivoa.net/documents/TAP/20160428/WD-TAP-1.1-20160428.pdf)
-- 
-- DESCRIPTION:
--     This script aims to migrate from a TAP_SCHEMA already existing into a TAP_SCHEMA as described
--     by the IVOA in the version 1.1 of the TAP standard.
-- 
--     This script is executed using 3 distinct and successive transactions: one for the step 0,
--     another one for the step 1 (actually managed by the script tap_schema_1.1.sql) and a last
--     one for the rest of the script.
-- 
-- WARNING 1: FORMER TAP_SCHEMA NOT DESTROYED
--     This SQL script does not destroy the former TAP_SCHEMA ; it is just renaming it into "old_TAP_SCHEMA".
--     It is particularly useful if you had added non-standard tables and/or columns. Thus, all former information
--     are still stored somewhere so that you can import them again with your own SQL script.

--     If you however want the former TAP_SCHEMA to be destroyed, just uncomment the last instruction of this SQL script:
--     the step 5 (i.e. the DROP SCHEMA ... CASCADE).
-- 
-- WARNING 2: SPECIAL COLUMN "DBNAME"
--     The special column "dbname" is expected in TAP_SCHEMA.schemas, TAP_SCHEMA.tables and TAP_SCHEMA.columns.
--     It is a column possibly expected by the TAP Library in order to perform an easy matching between ADQL names
--     and DB names during the ADQL to SQL translation. It allows to have different schema, table and/or column
--     name in ADQL/TAP than in the real database.
-- 
--     If your TAP_SCHEMA does not already have this column, you must uncomment the 3 commented lines of the step
--     2 of this script.
-- 
-- NOTE: 
--     Tested and designed for PostgreSQL ; it may work with some modifications for other DBMS
-- 
-- AUTHOR: Grégory Mantelet (ARI)
-- DATE  : July 2016


BEGIN;

-- 0. CHANGE THE SCHEMA NAME OF TAP_SCHEMA 1.0:

ALTER SCHEMA "TAP_SCHEMA" RENAME TO "old_TAP_SCHEMA";

-- 0bis. FIX SOME POSSIBLE PROBLEM WITH NULL VALUES:

UPDATE "old_TAP_SCHEMA"."tables" SET table_type = 'table'
	WHERE table_type IS NULL;

UPDATE "old_TAP_SCHEMA"."columns" SET indexed = 0
	WHERE indexed IS NULL;

UPDATE "old_TAP_SCHEMA"."columns" SET principal = 0
	WHERE principal IS NULL;

UPDATE "old_TAP_SCHEMA"."columns" SET std = 0
	WHERE std IS NULL;

COMMIT;

-- 1. CREATE TAP_SCHEMA IN VERSION 1.1:

\i tap_schema_1.1.sql

BEGIN;

-- 2. ADD THE SPECIAL COLUMN 'dbname' POSSIBLY USED BY THE TAP LIBRARY:

--ALTER TABLE "old_TAP_SCHEMA"."schemas" ADD COLUMN "dbname" VARCHAR;
--ALTER TABLE "old_TAP_SCHEMA"."tables"  ADD COLUMN "dbname" VARCHAR;
--ALTER TABLE "old_TAP_SCHEMA"."columns" ADD COLUMN "dbname" VARCHAR;
 
ALTER TABLE "TAP_SCHEMA"."schemas" ADD COLUMN "dbname" VARCHAR;
ALTER TABLE "TAP_SCHEMA"."tables"  ADD COLUMN "dbname" VARCHAR;
ALTER TABLE "TAP_SCHEMA"."columns" ADD COLUMN "dbname" VARCHAR;

-- 3. FILL THE NEW TAP_SCHEMA WITH THE FORMER VALUES (except those of TAP_SCHEMA):

INSERT INTO "TAP_SCHEMA"."schemas"
	(SELECT *
	 FROM "old_TAP_SCHEMA"."schemas"
	 WHERE LOWER("schema_name") <> 'tap_schema');

INSERT INTO "TAP_SCHEMA"."tables"
	(SELECT schema_name, table_name, table_type, description, utype, -1 AS "table_index", dbname
	 FROM "old_TAP_SCHEMA"."tables"
	 WHERE LOWER("schema_name") <> 'tap_schema');

INSERT INTO "TAP_SCHEMA"."columns"
	(SELECT table_name, column_name, datatype, "size" AS "arraysize", "size", c.description, c.utype, unit, ucd, indexed, principal, std, -1 AS "column_index", c.dbname
	 FROM "old_TAP_SCHEMA"."columns" c JOIN "old_TAP_SCHEMA"."tables" USING("table_name")
	 WHERE LOWER("schema_name") <> 'tap_schema');

INSERT INTO "TAP_SCHEMA"."keys"
	(SELECT *
	 FROM "old_TAP_SCHEMA"."keys");

INSERT INTO "TAP_SCHEMA"."key_columns"
	(SELECT *
	 FROM "old_TAP_SCHEMA"."key_columns");

-- 4. CHANGE THE table_index of TAP_SCHEMA tables so that they are at the end when ordering by ascending table_index:

UPDATE "TAP_SCHEMA"."tables" SET "table_index" = (SELECT COUNT(*) FROM "TAP_SCHEMA"."tables")-5 WHERE LOWER("schema_name") = 'tap_schema' AND LOWER("table_name") IN ('schemas', 'tap_schema.schemas');
UPDATE "TAP_SCHEMA"."tables" SET "table_index" = (SELECT COUNT(*) FROM "TAP_SCHEMA"."tables")-4 WHERE LOWER("schema_name") = 'tap_schema' AND LOWER("table_name") IN ('tables', 'tap_schema.tables');
UPDATE "TAP_SCHEMA"."tables" SET "table_index" = (SELECT COUNT(*) FROM "TAP_SCHEMA"."tables")-3 WHERE LOWER("schema_name") = 'tap_schema' AND LOWER("table_name") IN ('columns', 'tap_schema.columns');
UPDATE "TAP_SCHEMA"."tables" SET "table_index" = (SELECT COUNT(*) FROM "TAP_SCHEMA"."tables")-2 WHERE LOWER("schema_name") = 'tap_schema' AND LOWER("table_name") IN ('keys', 'tap_schema.keys');
UPDATE "TAP_SCHEMA"."tables" SET "table_index" = (SELECT COUNT(*) FROM "TAP_SCHEMA"."tables")-1 WHERE LOWER("schema_name") = 'tap_schema' AND LOWER("table_name") IN ('key_columns', 'tap_schema.key_columns');

-- 5. REMOVE THE FORMER TAP_SCHEMA:

--DROP SCHEMA "old_TAP_SCHEMA" CASCADE;

COMMIT;
