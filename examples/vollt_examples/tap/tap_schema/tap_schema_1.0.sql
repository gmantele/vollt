-- SQL Script based on TAP 1.0 (http://www.ivoa.net/Documents/TAP/20100327/REC-TAP-1.0.pdf)
-- 
-- DESCRIPTION:
--     This script create the schema TAP_SCHEMA and all its standard tables, as defined by the TAP standard
--     in the version 1.0.
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

CREATE TABLE "TAP_SCHEMA"."schemas"     ("schema_name" VARCHAR PRIMARY KEY, "description" VARCHAR           , "utype" VARCHAR);
CREATE TABLE "TAP_SCHEMA"."tables"      ("schema_name" VARCHAR            , "table_name" VARCHAR PRIMARY KEY, "table_type" VARCHAR   , "description" VARCHAR, "utype" VARCHAR, PRIMARY KEY("table_name"));
CREATE TABLE "TAP_SCHEMA"."columns"     ("table_name" VARCHAR             , "column_name" VARCHAR           , "description" VARCHAR  , "unit" VARCHAR       , "ucd" VARCHAR  , "utype" VARCHAR, "datatype" VARCHAR, "size" INTEGER, "principal" SMALLINT CHECK("principal" BETWEEN 0 AND 1), "indexed" SMALLINT CHECK("indexed" BETWEEN 0 AND 1), "std" SMALLINT CHECK("std" BETWEEN 0 AND 1), PRIMARY KEY("table_name","column_name"));
CREATE TABLE "TAP_SCHEMA"."keys"        ("key_id" VARCHAR PRIMARY KEY     , "from_table" VARCHAR            , "target_table" VARCHAR , "description" VARCHAR, "utype" VARCHAR);
CREATE TABLE "TAP_SCHEMA"."key_columns" ("key_id" VARCHAR                 , "from_column" VARCHAR           , "target_column" VARCHAR, PRIMARY KEY("key_id", "from_column", "target_column"));

-- 2. FILL TAP_SCHEMA.schemas

INSERT INTO "TAP_SCHEMA"."schemas" VALUES ('TAP_SCHEMA', 'Set of tables listing and describing the schemas, tables and columns published in this TAP service.', NULL);

-- 2bis. FILL TAP_SCHEMA.tables

INSERT INTO "TAP_SCHEMA"."tables" VALUES ('TAP_SCHEMA', 'TAP_SCHEMA.schemas'    , 'table', 'List of schemas published in this TAP service.'                                                                                                                                 , NULL);
INSERT INTO "TAP_SCHEMA"."tables" VALUES ('TAP_SCHEMA', 'TAP_SCHEMA.tables'     , 'table', 'List of tables published in this TAP service.'                                                                                                                                  , NULL);
INSERT INTO "TAP_SCHEMA"."tables" VALUES ('TAP_SCHEMA', 'TAP_SCHEMA.columns'    , 'table', 'List of columns of all tables listed in TAP_SCHEMA.TABLES and published in this TAP service.'                                                                                   , NULL);
INSERT INTO "TAP_SCHEMA"."tables" VALUES ('TAP_SCHEMA', 'TAP_SCHEMA.keys'       , 'table', 'List all foreign keys but provides just the tables linked by the foreign key. To know which columns of these tables are linked, see in TAP_SCHEMA.key_columns using the key_id.', NULL);
INSERT INTO "TAP_SCHEMA"."tables" VALUES ('TAP_SCHEMA', 'TAP_SCHEMA.key_columns', 'table', 'List all foreign keys but provides just the columns linked by the foreign key. To know the table of these columns, see in TAP_SCHEMA.keys using the key_id.'                    , NULL);

-- 2ter. FILL TAP_SCHEMA.columns

INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.schemas', 'schema_name', 'schema name, possibly qualified'            , NULL, NULL, NULL, 'VARCHAR', -1, 1, 1, 1);
INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.schemas', 'description', 'brief description of schema'                , NULL, NULL, NULL, 'VARCHAR', -1, 1, 0, 1);
INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.schemas', 'utype'      , 'UTYPE if schema corresponds to a data model', NULL, NULL, NULL, 'VARCHAR', -1, 0, 0, 1);

INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.tables', 'schema_name', 'the schema name from TAP_SCHEMA.schemas'   , NULL, NULL, NULL, 'VARCHAR', -1, 1, 1, 1);
INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.tables', 'table_name' , 'table name as it should be used in queries', NULL, NULL, NULL, 'VARCHAR', -1, 1, 1, 1);
INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.tables', 'table_type' , 'one of: table, view'                       , NULL, NULL, NULL, 'VARCHAR', -1, 0, 0, 1);
INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.tables', 'description', 'brief description of table'                , NULL, NULL, NULL, 'VARCHAR', -1, 1, 0, 1);
INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.tables', 'utype'      , 'UTYPE if table corresponds to a data model', NULL, NULL, NULL, 'VARCHAR', -1, 0, 0, 1);

INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.columns', 'table_name' , 'table name from TAP_SCHEMA.tables'              , NULL, NULL, NULL, 'VARCHAR', -1, 1, 1, 1);
INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.columns', 'column_name', 'column name'                                    , NULL, NULL, NULL, 'VARCHAR', -1, 1, 1, 1);
INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.columns', 'description', 'brief description of column'                    , NULL, NULL, NULL, 'VARCHAR', -1, 1, 0, 1);
INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.columns', 'unit'       , 'unit in VO standard format'                     , NULL, NULL, NULL, 'VARCHAR', -1, 1, 0, 1);
INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.columns', 'ucd'        , 'UCD of column if any'                           , NULL, NULL, NULL, 'VARCHAR', -1, 1, 0, 1);
INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.columns', 'utype'      , 'UTYPE of column if any'                         , NULL, NULL, NULL, 'VARCHAR', -1, 0, 0, 1);
INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.columns', 'datatype'   , 'ADQL datatype as in section 2.5'                , NULL, NULL, NULL, 'VARCHAR', -1, 1, 0, 1);
INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.columns', 'size'       , 'length of variable length datatypes'            , NULL, NULL, NULL, 'INTEGER', -1, 0, 0, 1);
INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.columns', 'principal'  , 'a principal column; 1 means true, 0 means false', NULL, NULL, NULL, 'INTEGER', -1, 0, 0, 1);
INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.columns', 'indexed'    , 'an indexed column; 1 means true, 0 means false' , NULL, NULL, NULL, 'INTEGER', -1, 0, 0, 1);
INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.columns', 'std'        , 'a standard column; 1 means true, 0 means false' , NULL, NULL, NULL, 'INTEGER', -1, 0, 0, 1);

INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.keys', 'key_id'      , 'unique key identifier'     , NULL, NULL, NULL, 'VARCHAR', -1, 1, 1, 1);
INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.keys', 'from_table'  , 'fully qualified table name', NULL, NULL, NULL, 'VARCHAR', -1, 1, 0, 1);
INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.keys', 'target_table', 'fully qualified table name', NULL, NULL, NULL, 'VARCHAR', -1, 1, 0, 1);
INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.keys', 'description' , 'description of this key'   , NULL, NULL, NULL, 'VARCHAR', -1, 1, 0, 1);
INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.keys', 'utype'       , 'utype of this key'         , NULL, NULL, NULL, 'VARCHAR', -1, 0, 0, 1);

INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.key_columns', 'key_id'       , 'unique key identifier'              , NULL, NULL, NULL, 'VARCHAR', -1, 1, 1, 1);
INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.key_columns', 'from_column'  , 'key column name in the from_table'  , NULL, NULL, NULL, 'VARCHAR', -1, 1, 1, 1);
INSERT INTO "TAP_SCHEMA"."columns" VALUES ('TAP_SCHEMA.key_columns', 'target_column', 'key column name in the target_table', NULL, NULL, NULL, 'VARCHAR', -1, 1, 1, 1);

COMMIT;
