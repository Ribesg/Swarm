#!/usr/bin/env bash

# Exit on error
set -e

# Edit these parameters
HOST="0.0.0.0"
PORT=80
SWARM_KEY=""
SLACK_HOOK=""

NOW=`date +%Y-%m-%dT%T`
ARGS="--host ${HOST} --port ${PORT} --key ${SWARM_KEY} --slack-hook ${SLACK_HOOK}"

# Starts screen
screen -S "swarm" -d -m bash -c "java -jar Swarm.jar $ARGS > swarm.$NOW.log 2>&1"
