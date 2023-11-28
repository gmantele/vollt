
## Default features set in ADQLParser

At its initialization, ADQLParser sets a list of supported optional language
features. By default, this list is as permissive as possible: everything
compatible with the implemented ADQL version is supported, and any UDF (even if
not declared) is allowed.

Of course, this list can be customized after creation of the parser.
_See the javadoc of ADQLParser for more technical details._ 

## Supported features provided by an ADQLTranslator

Databases are not supported guaranteed to support all optional features
introduced by ADQL.

For this reason, an appropriate list of supported optional features is provided 
in each implementation of ADQLTranslator: use the function
ADQLTranslator.getSupportedFeatures() to discover these features.

Here is a sum-up of supported features for each implemented translator:

|       Feature        | MySQL | MS-SQL Server | PostgreSQL | PgSphere |
| LOWER                |   X   |       X       |      X     |     X    |
| UPPER                |   X   |       X       |      X     |     X    |
| geometries           |       |               |            |     X    |
| ILIKE                |       |               |      X     |     X    |
| IN_UNIT              |       |               |            |          |


**Note about `ILIKE`:** In MySQL and MS-SQLServer, the case sensitiveness of
string comparison is determined by the collation of the compared strings.
ADQL-Lib could translate the ADQL's `o1 ILIKE o2` into
`LOWER(o1) LIKE LOWER(o2)` but in such case, the use of index on the column o1
would fail. That's why nothing is done by default in ADQL-Lib. Whether or not
search in a column must be case sensitive or not is completely DB dependant.
