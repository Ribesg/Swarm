#!/usr/bin/env bash

# Exit on error
set -e

# Find and kill the running screen
screen -ls | grep "swarm" | cut -d. -f1 | awk '{print $1}' | xargs kill
