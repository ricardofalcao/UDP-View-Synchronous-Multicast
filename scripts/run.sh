#!/bin/bash

SCRIPTPATH="$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )"

"$SCRIPTPATH"/../gradlew shadowJar > /dev/null

docker network create udp-network &> /dev/null
docker build -t udp-multicast "$SCRIPTPATH"/.. &> /dev/null

docker run --rm -it -v $SCRIPTPATH/../build/libs:/data --network=udp-network --cap-add=NET_ADMIN udp-multicast "$@"