#!/bin/bash

SCRIPTPATH="$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )"

"$SCRIPTPATH"/../gradlew build > /dev/null

docker network create -d bridge udp-network &> /dev/null
docker build -t udp-multicast "$SCRIPTPATH"/.. &> /dev/null

docker run --rm -it -v $SCRIPTPATH/../build/libs:/data --network=udp-network udp-multicast