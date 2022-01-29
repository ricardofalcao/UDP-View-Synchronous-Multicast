#!/bin/bash

SCRIPTPATH="$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )"

docker run --rm -it -v $SCRIPTPATH/../build/libs:/data -v $SCRIPTPATH/data:/internal --network=udp-network --cap-add=NET_ADMIN --user $(id -u):$(id -g) udp-multicast "$@"