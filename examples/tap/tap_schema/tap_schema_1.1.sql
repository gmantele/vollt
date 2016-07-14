-- SQL Script based on TAP 1.1 (http://www.ivoa.net/documents/TAP/20160428/WD-TAP-1.1-20160428.pdf)
-- 
-- DESCRIPTION:
--     This script create the schema TAP_SCHEMA and all its standard tables, as defined by the TAP standard
--     in the version 1.1.
-- 
--     The whole script is executed in a transaction in order to avoid partial creation of TAP_SCHEMA
--     in case of an SQL error.
-- 
-- NOTE:
--     Tested and designed for PostgreSQL ; it may work with some modifications for other DBMS
-- 
-- AUTHOR: Grégory Mantelet (ARI)
-- DATE  : July 2016


BEGIN;

-- 0. CREATE SCHEMA TAP_SCHEMA

CREATE SCHEMA "TAP_SCHEMA";

-- 1. CREATE TABLES TAP_SCHEMA.*

-- WARNING: the order of columns in TAP_SCHEMA.columns has changed between tap_schema_1.0.sql and tap_schema_1.1.sql!
--          All your own ingestion SQL scripts should be adapted except if the order of columns is explicitly given in the INSERT INTO instruction.

CREATE TABLE "TAP_SCHEMA"."schemas"     ("schema_name" VARCHAR PRIMARY KEY, "description" VARCHAR           , "utype" VARCHAR);
CREATE TABLE "TAP_SCHEMA"."tables"      ("schema_name" VARCHAR NOT NULL   , "table_name" VARCHAR PRIMARY KEY, "table_type" VARCHAR NOT NULL DEFAULT 'table'  , "description" VARCHAR, "utype" VARCHAR, "table_index" INTEGER DEFAULT -1);
CREATE TABLE "TAP_SCHEMA"."columns"     ("table_name" VARCHAR             , "column_name" VARCHAR           , "datatype" VARCHAR NOT NULL    , "arraysize" INTEGER  , "size" INTEGER , "description" VARCHAR, "utype" VARCHAR, "unit" VARCHAR, "ucd" VARCHAR, "indexed" SMALLINT NOT NULL DEFAULT 0 CHECK("indexed" BETWEEN 0 AND 1), "principal" SMALLINT NOT NULL DEFAULT 0 CHECK("principal" BETWEEN 0 AND 1), "std" SMALLINT NOT NULL DEFAULT 0 CHECK("std" BETWEEN 0 AND 1), "column_index" INTEGER DEFAULT -1, PRIMARY KEY("table_name","column_name"));
CREATE TABLE "TAP_SCHEMA"."keys"        ("key_id" VARCHAR PRIMARY KEY     , "from_table" VARCHAR NOT NULL   , "target_table" VARCHAR NOT NULL, "description" VARCHAR, "utype" VARCHAR);
CREATE TABLE "TAP_SCHEMA"."key_columns" ("key_id" VARCHAR                 , "from_column" VARCHAR           , "target_column" VARCHAR        , PRIMARY KEY("key_id", "from_column", "target_column"));

-- 2. FILL TAP_SCHEMA.schemas

INSERT INTO "TAP_SCHEMA"."schemas" VALUES ('TAP_SCHEMA', 'Set of tables listing and describing the schemas, tables and columns published in this TAP service.', NULL);

-- 2bis. FILL TAP_SCHEMA.tables

INSERT INTO "TAP_SCHEMA"."tables" VALUES ('TAP_SCHEMA', 'TAP_SCHEMA.schemas'    , 'table', 'List of schemas published in this TAP service.'                                                                                                                                 , NULL, 0);
INSERT INTO "TAP_SCHEMA"."tables" VALUES ('TAP_SCHEMA', 'TAP_SCHEMA.tables'     , 'table', 'List of tables published in this TAP service.'                                                                                                                                  , NULL, 1);
INSERT INTO "TAP_SCHEMA"."tables" VALUES ('TAP_SCHEMA', 'TAP_SCHEMA.columns'    , 'table', 'List of columns of all tables listed in TAP_SCHEMA.TABLES and published in this TAP service.'                                                                                   , NULL, 2);
INSERT INTO "TAP_SCHEMA"."tables" VALUES ('TAP_SCHEMA', 'TAP_SCHEMA.keys'       , 'table', 'List all foreign keys but provides just the tables linked by the foreign key. To know which columns of these tables are linked, see in TAP_SCHEMA.key_columns using the key_id.', NULL, 3);
INSERT INTO "TAP_SCHEMA"."tables" VALUES ('TAP_SCHEMA', 'TAP_SCHEMA.key_columns', 'table', 'List all foreign keys but provides just the columns linked by the foreign key. To know the table of these columns, see in TAP_SCHEMA.keys using the key_id.'                    , NULL, 4);

-- 2ter. FILL TAP_SCHEMA.columns

INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.schemas', 'schema_name', 'VARCHAR', -1, -1, 'schema name, possibly qualified'            , NULL, NULL, NULL, 1, 1, 1, 0);
INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.schemas', 'description', 'VARCHAR', -1, -1, 'brief description of schema'                , NULL, NULL, NULL, 0, 1, 1, 1);
INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.schemas', 'utype'      , 'VARCHAR', -1, -1, 'UTYPE if schema corresponds to a data model', NULL, NULL, NULL, 0, 0, 1, 2);

INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.tables', 'table_index', 'INTEGER', -1, -1, 'this index is used to recommend table ordering for clients' , NULL, NULL, NULL, 0, 0, 1, 0);
INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.tables', 'schema_name', 'VARCHAR', -1, -1, 'the schema name from TAP_SCHEMA.schemas'                    , NULL, NULL, NULL, 0, 1, 1, 1);
INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.tables', 'table_name' , 'VARCHAR', -1, -1, 'table name as it should be used in queries'                 , NULL, NULL, NULL, 1, 1, 1, 2);
INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.tables', 'table_type' , 'VARCHAR', -1, -1, 'one of: table, view'                                        , NULL, NULL, NULL, 0, 0, 1, 3);
INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.tables', 'description', 'VARCHAR', -1, -1, 'brief description of table'                                 , NULL, NULL, NULL, 0, 1, 1, 4);
INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.tables', 'utype'      , 'VARCHAR', -1, -1, 'UTYPE if table corresponds to a data model'                 , NULL, NULL, NULL, 0, 0, 1, 5);

INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.columns', 'column_index', 'INTEGER' , -1, -1, 'this index is used to recommend column ordering for clients' , NULL, NULL, NULL, 0, 0, 1, 0);
INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.columns', 'table_name'  , 'VARCHAR' , -1, -1, 'table name from TAP_SCHEMA.tables'                           , NULL, NULL, NULL, 1, 1, 1, 1);
INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.columns', 'column_name' , 'VARCHAR' , -1, -1, 'column name'                                                 , NULL, NULL, NULL, 1, 1, 1, 2);
INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.columns', 'datatype'    , 'VARCHAR' , -1, -1, 'an XType or a TAPType'                                       , NULL, NULL, NULL, 0, 1, 1, 3);
INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.columns', 'arraysize'   , 'INTEGER' , -1, -1, 'length of variable length datatypes'                         , NULL, NULL, NULL, 0, 0, 1, 4);
INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.columns', '"size"'      , 'INTEGER' , -1, -1, 'same as "arraysize" but kept for backward compatibility only', NULL, NULL, NULL, 0, 0, 1, 5);
INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.columns', 'description' , 'VARCHAR' , -1, -1, 'brief description of column'                                 , NULL, NULL, NULL, 0, 1, 1, 6);
INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.columns', 'utype'       , 'VARCHAR' , -1, -1, 'UTYPE of column if any'                                      , NULL, NULL, NULL, 0, 0, 1, 7);
INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.columns', 'unit'        , 'VARCHAR' , -1, -1, 'unit in VO standard format'                                  , NULL, NULL, NULL, 0, 1, 1, 8);
INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.columns', 'ucd'         , 'VARCHAR' , -1, -1, 'UCD of column if any'                                        , NULL, NULL, NULL, 0, 1, 1, 9);
INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.columns', 'indexed'     , 'SMALLINT', -1, -1, 'an indexed column; 1 means true, 0 means false'              , NULL, NULL, NULL, 0, 0, 1, 10);
INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.columns', 'principal'   , 'SMALLINT', -1, -1, 'a principal column; 1 means true, 0 means false'             , NULL, NULL, NULL, 0, 0, 1, 11);
INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.columns', 'std'         , 'SMALLINT', -1, -1, 'a standard column; 1 means true, 0 means false'              , NULL, NULL, NULL, 0, 0, 1, 12);

INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.keys', 'key_id'      , 'VARCHAR', -1, -1, 'unique key identifier'     , NULL, NULL, NULL, 1, 1, 1, 0);
INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.keys', 'from_table'  , 'VARCHAR', -1, -1, 'fully qualified table name', NULL, NULL, NULL, 0, 1, 1, 1);
INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.keys', 'target_table', 'VARCHAR', -1, -1, 'fully qualified table name', NULL, NULL, NULL, 0, 1, 1, 2);
INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.keys', 'description' , 'VARCHAR', -1, -1, 'description of this key'   , NULL, NULL, NULL, 0, 1, 1, 3);
INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.keys', 'utype'       , 'VARCHAR', -1, -1, 'utype of this key'         , NULL, NULL, NULL, 0, 0, 1, 4);

INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.key_columns', 'key_id'       , 'VARCHAR', -1, -1, 'unique key identifier'              , NULL, NULL, NULL, 1, 1, 1, 0);
INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.key_columns', 'from_column'  , 'VARCHAR', -1, -1, 'key column name in the from_table'  , NULL, NULL, NULL, 1, 1, 1, 1);
INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.key_columns', 'target_column', 'VARCHAR', -1, -1, 'key column name in the target_table', NULL, NULL, NULL, 1, 1, 1, 2);

COMMIT;
