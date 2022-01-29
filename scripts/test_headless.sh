#!/bin/bash
SCRIPTPATH="$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )"

if [ "$1" = "" ]
then
  echo "Usage: $0 <processes> [args...]"
  exit
fi

echo "Compiling java program..."
"$SCRIPTPATH"/../gradlew shadowJar > /dev/null

echo "Creating docker network..."
docker network create udp-network &> /dev/null

echo "Building docker image..."
docker build -t udp-multicast "$SCRIPTPATH"/.. &> /dev/null

echo "Creating leader..."
RES=$(/bin/bash "${SCRIPTPATH}"/run_headless.sh --leader)
sleep 2

PIDS=($RES)

for i in $(seq $1)
do
  echo "Creating node ${i}..."
  RES=$(/bin/bash "${SCRIPTPATH}"/run_headless.sh "${@:2}")
  PIDS+=($RES)
  sleep 1
done

read -n 1 -s -r -p "Press any key to continue: "
echo ""

echo "Destroying containers..."
docker container rm --force ${PIDS[*]} > /dev/null
