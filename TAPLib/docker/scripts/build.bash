#!/usr/bin/env bash

# Local port which the Tomcat application should be exported to.
TOMCAT_PORT=8080

# Name of the Docker image as it will appear in `docker image ls`.
IMAGE_NAME=vollt-tap

# Name of the Docker container as it will appear in `docker ps`.
CONTAINER_NAME=$IMAGE_NAME

SHORT_VERSION="0.1"
LONG_VERSION="$SHORT_VERSION--vollt-tap-2.4beta"

DIR_BIN="$(dirname $(readlink -f $0))"
DIR_DOCKER="$(dirname $(readlink -f $DIR_BIN))"


################################################################################
#
# Build the Docker image:
#

echo "Building the Docker image '$IMAGE_NAME'..."
docker build --build-arg HTTP_PORT=$TOMCAT_PORT -t $IMAGE_NAME:$LONG_VERSION $DIR_DOCKER
docker tag $IMAGE_NAME:$LONG_VERSION $IMAGE_NAME:$SHORT_VERSION
docker tag $IMAGE_NAME:$LONG_VERSION $IMAGE_NAME:latest


################################################################################
#
# Write the `run.bash` script:
#

FILE_RUN="$DIR_BIN/run.bash"
echo "Writing the script '$FILE_RUN'..."
echo "#!/usr/bin/env bash

# Remove a previous instance of this container:
if [ \$( docker ps -a | grep '$CONTAINER_NAME' | wc -l ) -gt 0 ]; then
    echo "INFO: Removing previous instance of this container..."
    docker rm '$CONTAINER_NAME'
fi

docker run -d \\
           -p 8080:$TOMCAT_PORT \\
           --env-file=db-pool.env \\
           -v "$(pwd)/tap-data:/data" \\
           --add-host host.docker.internal:host-gateway \\
           --name '$CONTAINER_NAME' \\
           '$IMAGE_NAME'

# Wait a bit before testing the container status:
sleep 1

# Check it status:
output=\$( docker ps -a -f name='$CONTAINER_NAME' | grep '$CONTAINER_NAME' 2> /dev/null )
if [ -z \"\$output\" ];
then 
    echo "FATAL: No container created!"
else
    output=\$( docker ps -f name='$CONTAINER_NAME' | grep '$CONTAINER_NAME' 2> /dev/null )
    if [ -z \"\$output\" ];
    then
        echo "ERROR: Container not started! See logs below:"
        docker logs '$CONTAINER_NAME'
    else
        echo "INFO: Container started!"
    fi
fi

" > $FILE_RUN


################################################################################
#
# Write the `stop.bash` script:
#

FILE_STOP="$DIR_BIN/stop.bash"
echo "Writing the script '$FILE_STOP'..."
echo "#!/usr/bin/env bash
docker stop '$CONTAINER_NAME'
" > $FILE_STOP


################################################################################
#
# Write the `clean.bash` script:
#

FILE_CLEAN="$DIR_BIN/clean.bash"
echo "Writing the script '$FILE_CLEAN'..."
echo "#!/usr/bin/env bash

# Stop the container, if running:
echo 'INFO: stopping container \"$CONTAINER_NAME\"...'
docker stop '$CONTAINER_NAME'

# Remove the container:
echo 'INFO: removing container \"$CONTAINER_NAME\"...'
docker rm '$CONTAINER_NAME'

# Remove the images:
echo 'INFO: removing all images of \"$IMAGE_NAME\"...'
docker image rm $IMAGE_NAME:latest $IMAGE_NAME:$SHORT_VERSION $IMAGE_NAME:$LONG_VERSION

# Remove the scripts:
echo 'INFO: removing the auto-generated start, stop and clean scripts...'
rm $FILE_RUN $FILE_STOP $FILE_CLEAN
" > $FILE_CLEAN

################################################################################
#
# Give execution permission to both scripts:
#

chmod u+x $FILE_RUN $FILE_STOP $FILE_CLEAN

