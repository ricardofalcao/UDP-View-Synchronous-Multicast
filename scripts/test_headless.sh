#!/bin/bash
SCRIPTPATH="$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )"

echo "Compiling java program..."
"$SCRIPTPATH"/../gradlew shadowJar > /dev/null

echo "Creating docker network..."
docker network create udp-network &> /dev/null

echo "Building docker image..."
docker build -t udp-multicast "$SCRIPTPATH"/.. &> /dev/null

echo "Creating leader..."
/bin/bash "${SCRIPTPATH}"/run_headless.sh --leader "$@"
sleep 2

for i in {1..10}
do
  echo "Creating node ${i}..."
  /bin/bash "${SCRIPTPATH}"/run_headless.sh "$@"
  sleep 2
done