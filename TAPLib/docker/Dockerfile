FROM tomcat:7-jre8-alpine
LABEL maintainer="gregory.mantelet@astro.unistra.fr" \
      description="Container running a TAP service with a given configuration." \
      taplib.version="2.4beta"

# Set the correct timezone:
RUN apk add --no-cache tzdata
#ENV TZ=Europe/Paris # already declared in the file `.env` of docker-compose.yml

# Install GNU-sed (required by start-tap.sh):
RUN apk add --no-cache sed

# Install wget (to download the required libraries):
RUN apk add --no-cache wget

################################################################################
#
# CONFIGURE TOMCAT
#

# Add the user `admin` able to access to the Tomcat's Web GUI:
COPY tomcat/tomcat-users.xml /usr/local/tomcat/conf/tomcat-users.xml

# Load all Tomcat shared libraries (JDBC drivers + SLF4J):
COPY tomcat/shared-libs/* /usr/local/tomcat/lib/

# Remove the default webapps (docs, examples, ...):
RUN rm -rf /usr/local/tomcat/webapps/examples/ \
           /usr/local/tomcat/webapps/docs/ \
           /usr/local/tomcat/webapps/host-manager

# Get the HTTP port:
ARG HTTP_PORT=8080

# Open some ports inside the Docker network:
EXPOSE 8009
EXPOSE $HTTP_PORT

################################################################################
#
# SET UP THE TAP SERVICE
#

# Load all Web applications to deploy:
COPY tomcat/tap-webapp /usr/local/tomcat/webapps/tap/

# Download all required libraries:
RUN wget -O /usr/local/tomcat/webapps/tap/WEB-INF/lib/commons-fileupload.jar \
            'https://repo1.maven.org/maven2/commons-fileupload/commons-fileupload/1.3.3/commons-fileupload-1.3.3.jar'
RUN wget -O /usr/local/tomcat/webapps/tap/WEB-INF/lib/commons-io.jar \
            'https://repo1.maven.org/maven2/commons-io/commons-io/2.6/commons-io-2.6.jar'
RUN wget -O /usr/local/tomcat/webapps/tap/WEB-INF/lib/javax-servlet-api.jar \
            'https://repo1.maven.org/maven2/javax/servlet/javax.servlet-api/3.0.1/javax.servlet-api-3.0.1.jar'
RUN wget -O /usr/local/tomcat/webapps/tap/WEB-INF/lib/json.jar \
            'https://repo1.maven.org/maven2/org/json/json/20180813/json-20180813.jar'
RUN wget -O /usr/local/tomcat/webapps/tap/WEB-INF/lib/slf4j-api.jar \
            'https://repo1.maven.org/maven2/org/slf4j/slf4j-api/1.7.25/slf4j-api-1.7.25.jar'

# Load the TAP configuration file:
COPY tap.properties /usr/local/tomcat/webapps/tap/WEB-INF/

# Create the directory in which all backups, logs and results will be written:
RUN mkdir /data

# Import the starting script:
COPY ./scripts/tap-starter.sh ./tap-starter.sh
RUN chmod a+x ./tap-starter.sh

EXPOSE $HTTP_PORT

CMD ["./tap-starter.sh"]
