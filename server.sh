#!/bin/bash

# Compile the server code

# Start the RMI registry (runs in the background)
rmiregistry &

# Give the registry a couple of seconds to start
sleep 1

java Replica 1 &

sleep 1

java Replica 2 &

sleep 1

java Replica 3 &

sleep 1

java FrontEndServer




