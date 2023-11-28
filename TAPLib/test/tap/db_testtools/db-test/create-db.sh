#!/bin/bash

DB_NAME='db-test'
DB_FILE="$DB_NAME.mv.db"

DB_USER='junit'
DB_PWD='super-pwd'

LIB_DIR='../../../../lib'

H2_CP="$LIB_DIR/h2-1.4.193.jar:$LIB_DIR/jts-core-1.14.0.jar:$LIB_DIR/spatial4j-0.6.jar:$LIB_DIR/astroh2-0.3.jar"

if [ ! -f "$DB_FILE" ]
then
	echo -ne "Creating the test database ($DB_FILE)..."
	sed "s/CSVREAD('.\/test\/tap\/db_testtools\/db-test\/hipparcos_subset.csv');/CSVREAD('hipparcos_subset.csv');/" create-db.sql > temp_create-db.sql
	java -cp "$H2_CP" org.h2.tools.RunScript -url "jdbc:h2:./$DB_NAME" -user "$DB_USER" -password "$DB_PWD" -script temp_create-db.sql
	rm temp_create-db.sql
	echo -e "OK"
else
	echo "The test database already exists! To re-create it, please delete it (rm \"$DB_FILE\") and re-execute this script."
fi
