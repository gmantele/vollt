_**Date:** 25th Sept. 2019_
_**Author:** Grégory Mantelet_

# Identifiers' rules in ADQL-Lib

## Definitions

These rules apply to any kind of identifier (e.g. table, column).

To simplify explanations, we will consider here that an identifier is composed of 3 pieces of information:

- adqlName _- string_
- adqlCaseSensitive _- boolean_
- dbName _- string_

`adqlName` and `dbName` are never stored in their qualified (e.g. the table prefix inside a full column name) or their delimited (e.g. `"aTable"` is the delimited form of `aTable`) form ; surrounded double quotes (and escaped double quotes: `""`) and prefixes are used/checked at initialization but always discarded for storage.

If `dbName` is not specified, it is the same as `adqlName`.

`adqlCaseSensitive` is set to `true` only if `adqlName` was delimited at initialization.

*Rules about how to build this 3-tuple depend on the origin of the identifier (e.g. `TAP_SCHEMA`, subquery, CTE) ; these rules are detailed below.*

Let's now see how to write ADQL and SQL queries with a such 3-tuple...

## Identifiers in ADQL

In ADQL queries, an identifier MUST be delimited only in the following cases:

* if a ADQL/SQL reserved keyword

  ```sql
  -- declared table identifier: adqlName=`distance`
  SELECT ... FROM distance -- INCORRECT because `distance` is a reserved keyword
  SELECT ... FROM "distance" -- CORRECT
  ```

* if not a regular ADQL identifier

  ```sql
  -- declared table identifier: adqlName=`2do`
  SELECT ... FROM 2do -- INCORRECT because `2do` is starting with a digit
  SELECT ... FROM "2do" -- CORRECT
  ```

* if ambiguous with another identifier of the same type

  ```sql
  -- declared column identifiers: adqlName=`id` in `table1` and adqlName=`id` in `table2`
  SELECT id FROM table1, table2 -- INCORRECT because the column `id` exists in both tables
  SELECT table1.id FROM table1, table2 -- CORRECT
  ```



In any other case, the identifier _MAY_ be delimited, but if not, you are free to write it in upper-/lower-/mixed-case.

If the identifier is declared in a *CASE-SENSITIVE* way, it MUST be respected when delimited in the ADQL query.

If the identifier is *CASE-INSENSITIVE*, its delimited ADQL version MUST be all in lower-case.

Then, the following ADQL queries are perfectly allowed:

```sql
-- declared table: adqlName=`aTable`, adqlCaseSensitive=`false`
SELECT ... FROM atable -- OK
SELECT ... FROM ATABLE -- OK
SELECT ... FROM "atable" -- OK (because lower-case if not declared CS)
SELECT ... FROM "aTable" -- INCORRECT

-- declared table: adqlName=`aTable`, adqlCaseSensitive=`true`
SELECT ... FROM atable -- OK
SELECT ... FROM ATABLE -- OK
SELECT ... FROM "atable" -- INCORRECT
SELECT ... FROM "aTable" -- OK
```

## SQL translation

_In this part, we will consider PostgreSQL as SQL target._

The `dbName` of an identifier is _always_ considered as _case-sensitive_. So, it will _always_ be written delimited in SQL queries.

**Examples of SQL queries:**

```sql
-- declared table: adqlName=`aTable`, dbName=-
-- ADQL: SELECT ... FROM atable
SELECT ... FROM "aTable"

-- declare table: adqlName=`aTable`, dbName=`DBTable`
-- ADQL: SELECT ... FROM atable
SELECT ... FROM "DBTable"
```

## Automatic column aliases in SQL

To ensure having the expected labels in SQL query results, aliases are automatically added (if none is specified) to all items of the `SELECT` clause.

As `dbName`s, these default aliases are considered as case sensitive.

They are built using the `adqlName` of the aliased identifiers. If this name is _not case-sensitive_, the alias will be written in lower-case. But if _case-sensitive_, it is written exactly the same.

**Examples of SQL queries:**

```sql
-- declared table: adqlName=`aTable`, dbName=`DBTable`
-- declared columns in `aTable`:
--   * adqlName=`ColCS`, adqlCaseSensitive=`false`, dbName=`dbCol1`
--   * adqlName=`ColCI`, adqlCaseSensitive=`true`, dbName=`dbCol2`
-- ADQL: SELECT colcs, colci FROM atable
SELECT "dbCol1" AS "colcs", "dbCol2" AS "ColCI" FROM "DBTable"
```



## Schemas/Tables/Columns declared in `TAP_SCHEMA`

* `adqlName` = `TAP_SCHEMA.(schemas.schema_name|tables.table_name|columns.column_name)`
* `dbName` = `TAP_SCHEMA.(schemas|tables|columns).dbName` or if NULL `adqlName`

**Examples with `TAP_SCHEMA.tables`:**

| In TAP_SCHEMA.tables                           | In ADQL-Lib                                                  |
| ---------------------------------------------- | ------------------------------------------------------------ |
| table_name=`aTable`, dbName=_null_             | adqlName=`aTable`, adqlCaseSensitive=`false`, dbName=_null_  |
| table_name=`schema.aTable`, dbName=_null_      | adqlName=`aTable`, adqlCaseSensitive=`false`, dbName=_null_  |
| table_name=`"aTable"`, dbName=_null_           | adqlName=`aTable`, adqlCaseSensitive=`true`, dbName=_null_   |
| table_name=`schema."aTable"`, dbName=_null_    | adqlName=`aTable`, adqlCaseSensitive=`true`, dbName=_null_   |
| table_name=`aTable`, dbName=`DBTable`          | adqlName=`aTable`, adqlCaseSensitive=`false`, dbName=`DBTable` |
| table_name=`aTable`, dbName=`"DBTable"`        | adqlName=`aTable`, adqlCaseSensitive=`false`, dbName=`DBTable` |
| table_name=`aTable`, dbName=`schema.DBTable`   | adqlName=`aTable`, adqlCaseSensitive=`false`, dbName=`schema.DBTable` |
| table_name=`aTable`, dbName=`schema."DBTable"` | adqlName=`aTable`, adqlCaseSensitive=`false`, dbName=`schema."DBTable"` |

## Tables from subqueries (i.e. `FROM` and `WITH`)

_Reminder: in ADQL, a subquery declared as table in the `FROM` clause and a CTE declared in the `WITH` clause MUST always be aliased/named._

* `adqlName` = _subquery's alias/CTE's name_
* `dbName` = `adqlName`

If the alias/name is *delimited* (i.e. case sensitive), `adqlCaseSensitive` will be set to `true` and surrounding double quotes are removed from `adqlName` .

If the alias/name is *not delimited*, `adqlName` is set to the alias put into lower-case, and `adqlCaseSensitive` is `false`.

**Examples:**

```sql
--
-- Subqueries in FROM:
--
SELECT ... FROM (SELECT * FROM atable) AS t1
SELECT ... FROM (SELECT * FROM atable) AS T1
-- => adqlName=`t1`, adqlCaseSensitive=`false`, dbName=`t1`

SELECT ... FROM (SELECT * FROM atable) AS "T1"
-- => adqlName=`T1`, adqlCaseSensitive=`true`, dbName=`T1`

--
-- CTEs in WITH:
--
WITH t1 AS (SELECT * FROM atable) SELECT ... FROM t1
WITH T1 AS (SELECT * FROM atable) SELECT ... FROM t1
-- => adqlName=`t1`, adqlCaseSensitive=`false`, dbName=`t1`

WITH "T1" AS (SELECT * FROM atable) SELECT ... FROM t1
-- => adqlName=`T1`, adqlCaseSensitive=`true`, dbName=`T1`
```

## Columns of a (sub)query

* If _NOT aliased_:

  * `adqlName` = _original's `adqlName`_
  * `adqlCaseSensitive` = _original's `adqlCaseSensitive`_
  * `dbName` = _original's `dbName`_

  

* If _aliased_:

  * `adqlName` = _alias in lower-case if not delimited, exact same alias otherwise_
  * `adqlCaseSensitive` = `true` _if alias is delimited_, `false` _otherwise_
  * `dbName` = `adqlName`



**Examples:**

```sql
-- declared column: adqlName=`aColumn`, adqlCaseSensitive=`false`, dbName=`DBCol`
SELECT acolumn FROM atable
SELECT AColumn FROM atable
-- => the declared column

-- declared column: adqlName=`aColumn`, adqlCaseSensitive=`true`, dbName=`DBCol`
SELECT acolumn FROM atable
SELECT AColumn FROM atable
-- => the declared column

-- declared column: adqlName=`aColumn`, adqlCaseSensitive=`true`, dbName=`DBCol`
SELECT acolumn AS myColumn FROM atable
-- => adqlName=`mycolumn`, adqlCaseSensitive=`false`, dbName=`mycolumn`
SELECT acolumn AS "myColumn" FROM atable
-- => adqlName=`myColumn`, adqlCaseSensitive=`true`, dbName=`myColumn`

```

_**Note:** The new representation of an aliased column has different ADQL and DB names, but the other metadata (e.g. datatype, UCD, ...) of the original column are copied as such._

## Duplicated output columns

_The term 'output columns' means here the columns written in the output format (e.g. VOTable). They are not the columns represented as a 3-tuple in this document._

**TODO**



