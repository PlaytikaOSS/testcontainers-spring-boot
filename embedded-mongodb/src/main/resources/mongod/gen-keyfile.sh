#!/usr/bin/env bash

# Mongo requires a keyfile for replicasets, but the docker image does not generate one.
# The image does let you run this arbitrary script after db initialization.

echo "testcontainers" > /data/configdb/mongod.keyfile
chmod 400 /data/configdb/mongod.keyfile