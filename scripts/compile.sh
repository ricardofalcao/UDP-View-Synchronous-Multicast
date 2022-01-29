#!/bin/bash
SCRIPTPATH="$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )"

echo "Compiling java program..."
"$SCRIPTPATH"/../gradlew shadowJar > /dev/null

echo "Creating docker network..."
docker network create udp-network &> /dev/null

echo "Building docker image..."
docker build -t udp-multicast "$SCRIPTPATH"/.. &> /dev/null

/bin/bash "${SCRIPTPATH}"/run.sh "$@"