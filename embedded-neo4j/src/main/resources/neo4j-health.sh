#!/usr/bin/env bash
# https://neo4j.com/docs/operations-manual/current/configuration/wait-for-start/
set -eo pipefail

apk add --no-cache --quiet \
    bash \
    curl

if [ "200" = "$(curl --silent --write-out %{http_code} --output /dev/null http://localhost:7474)" ]; then
  curl -v -u neo4j:neo4j -X POST localhost:7474/user/neo4j/password -H "Content-type:application/json" -d "{\"password\":\"letmein\"}"
  exit 0
fi

exit 1