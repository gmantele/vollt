#!/bin/bash

WEBAPPS_DIR=/usr/local/tomcat/webapps


################################################################################
#
# CHECK THAT ALL REQUIRED VARIABLES ARE SET
#

# Database type:
if [ -z "$DB_TYPE" ];
then
    echo "ERROR: Missing database type!" >&2
    echo "       You must set the environment variable 'DB_TYPE'." >&2
    echo "       Accepted values: 'postgres', 'pgsphere', 'sqlserver', 'mysql'." >&2
    exit 1;
elif [ "$DB_TYPE" != "postgres" \
         -a "$DB_TYPE" != "pgsphere" \
         -a "$DB_TYPE" != "sqlserver" \
         -a "$DB_TYPE" != "mysql" ];
then
    echo "ERROR: Incorrect database type: '$DB_TYPE'!" >&2
    echo "       Accepted values: 'postgres', 'pgsphere', 'sqlserver', 'mysql'." >&2
    exit 1;
fi

# Database name:
if [ -z "$DB_NAME" ];
then
    echo "ERROR: Missing database name!" >&2
    echo "       You must sete the environment variable 'DB_NAME'." >&2
    exit 2;
fi


################################################################################
#
# WRITE META-INF/context.xml:
# (i.e. configure the database access and the connection pool)
#

# Set the output file:
CONTEXT_FILE=$WEBAPPS_DIR/tap/META-INF/context.xml

# Start the Context and Resource definitions:
echo -e '<Context>\n\t<Resource name="jdbc/tapdb" auth="Container"' > $CONTEXT_FILE
# Set the Resource type (SQL's Datasource):
echo -e '\t\ttype="javax.sql.DataSource"' >> $CONTEXT_FILE
# Set the JDBC driver:
case "$DB_TYPE" in
    postgres|pgsphere)
        echo -e '\t\tdriverClassName="org.postgresql.Driver"' >> $CONTEXT_FILE
        ;;
    sqlserver)
        echo -e '\t\tdriverClassName="com.microsoft.sqlserver.jdbc.SQLServerDriver"' >> $CONTEXT_FILE
        ;;
    mysql)
        echo -e '\t\tdriverClassName="com.mysql.jdbc.Driver"' >> $CONTEXT_FILE
        ;;
    *)
        echo "ERROR: Incorrect database type: '$DB_TYPE'!" >&2
        echo "       Accepted values: 'postgres', 'pgsphere', 'sqlserver', 'mysql'." >&2
        exit 1;
esac
# Set the JDBC URL to access to the TAP database:
if [ -z "$DB_URL" ];
then
    echo -ne '\t\turl="jdbc:' >> $CONTEXT_FILE
    if [ "$DB_TYPE" = 'postgres' -o "$DB_TYPE" = 'pgsphere' ];
    then
        echo -ne "postgresql:" >> $CONTEXT_FILE
    else
        echo -ne "$DB_TYPE:" >> $CONTEXT_FILE
    fi
    if [ -z "$DB_HOST" ];
    then
        echo -e "//localhost/$DB_NAME\"" >> $CONTEXT_FILE
    else
        echo -ne "//$DB_HOST" >> $CONTEXT_FILE
        if [ ! -z "$DB_PORT" ];
        then
            echo -ne ":$DB_PORT" >> $CONTEXT_FILE
        fi
        echo -e "/$DB_NAME\"" >> $CONTEXT_FILE
    fi
else
    echo -e "\t\turl=\"$DB_URL\"" >> $CONTEXT_FILE
fi
# [optional] Set the database User and Password to use:
if [ ! -z "$DB_USER" ];
then
	echo -e "\t\tusername=\"$DB_USER\" password=\"$DB_PASSWORD\"" >> $CONTEXT_FILE
fi
# [optional] Configure the DB connection pool (see all variables `DB_POOL_*`):
env | grep "^DB_POOL_" | while read -r line;
do
    propname=`echo "$line" | sed 's/^DB_POOL_\([^=]*\)=.*/\1/' \
              | tr '[:upper:]' '[:lower:]' \
              | sed 's/_\([a-z]\)/\U\1/g'`
    propvalue=`echo "$line" | sed 's/[^=]*=\(.*\)/\1/'`
    echo -e "\t\t$propname=\"$propvalue\"" >> $CONTEXT_FILE
done
# Close the Resource:
echo -e "\t/>" >> $CONTEXT_FILE
# Close the Context...and finish the META-INF/context.xml file:
echo -e "</Context>" >> $CONTEXT_FILE


################################################################################
#
# UPDATE WEB-INF/tap.properties WITH DB CONNECTION INFORMATION
#

TAP_CONF_FILE=$WEBAPPS_DIR/tap/WEB-INF/tap.properties

# Set the database access type:
sed -i -e "s/^[[:space:]]*database_access[[:space:]]*=.*$/database_access = jndi/" $TAP_CONF_FILE

# Set the JNDI datasource name:
sed -i -e "s/^[[:space:]]*database_access[[:space:]]*=.*$/datasource_jndi_name = java:/comp/env/jdbc/tapdb/" $TAP_CONF_FILE

# Set the ADQL->SQL translator:
sed -i -e "s/^[[:space:]]*sql_translator[[:space:]]*=.*$/sql_translator = ${DB_TYPE}/" $TAP_CONF_FILE


################################################################################
#
# FINALLY START TOMCAT:
#
catalina.sh run

