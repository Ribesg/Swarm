#!/usr/bin/env bash

# Exit on error
set -e

# Copy some scripts
find Swarm/scripts/ -type f ! -name install.sh -exec cp -t . {} +
chmod +x *.sh

# Notify user
echo "Done updating Swarm scripts"
echo "Please edit start.sh with the appropriate parameters"
