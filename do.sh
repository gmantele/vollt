export SERVLET_API="/home/bourgesl/apps/apache-tomcat-7.0.41/lib/servlet-api.jar"
export POSTGRES_JDBC="/usr/share/java/postgresql-jdbc4.jar"

echo "SERVLET-API: $SERVLET_API"
echo "POSTGRES-JDBC: $POSTGRES_JDBC"

#ant -DSERVLET-API=$SERVLET_API -f buildUWS.xml

#ant -DPOSTGRES-JDBC=$POSTGRES_JDBC -f buildADQL.xml

ant -DSERVLET-API=$SERVLET_API -DPOSTGRES-JDBC=$POSTGRES_JDBC -f buildTAP.xml buildLibAndSrc buildJavadoc

