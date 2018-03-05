#!/usr/bin/env bash

# Exit on error
set -e

# Clone Swarm
git clone https://github.com/Ribesg/Swarm.git

# Build Swarm
cd Swarm
mvn
cd ..

# Copy the resulting jar file
cp Swarm/target/Swarm.jar .

# Copy some scripts
find Swarm/scripts/ -type f ! -name install.sh -exec cp -t . {} +
chmod +x *.sh

# Notify user
echo "Done installing Swarm locally"
echo "Please edit start.sh with the appropriate parameters"
