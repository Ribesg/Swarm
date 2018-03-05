#!/usr/bin/env bash

# Exit on error
set -e

# Update Swarm
cd Swarm
git pull
mvn
cd ..

# Copy the resulting jar file
cp Swarm/target/Swarm.jar .


# Notify user
echo "Done updating Swarm locally"
