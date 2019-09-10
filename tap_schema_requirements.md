
# TAP_SCHEMA.schemas

`db_name`

:  - optional
   - never qualified nor delimited
   - if `NULL`, exactly equals to undelimited `schema_name`

# TAP_SCHEMA.tables

`schema_name`

:  exactly equals to `TAP_SCHEMA.schemas.schema_name`

`table_name`

:  - may be qualified (by schema name which can be be delimited or
     not ; it does not have to be exactly equals to
   `TAP_SCHEMA.schemas.schema_name`)
   - delimited if case sensitive (or not an    ADQL regular identifier)

`db_name`

:  - optional
   - never qualified nor delimited
   - if `NULL`, exactly equals to undelimited `table_name`

# TAP_SCHEMA.columns

`table_name`

:  exactly equals to `TAP_SCHEMA.tables.table_name`

`column_name`

:  - delimited if case sensitive (or if not an ADQL regular identifier)
   - never qualified

`db_name`

:  - optional
   - never qualified nor delimited
   - if `NULL`, exactly equals to undelimited `column_name`

# TAP_SCHEMA.keys

`from_table`

:  exactly equals to `TAP_SCHEMA.tables.table_name`

`target_table`

:  exactly equals to `TAP_SCHEMA.tables.table_name`

