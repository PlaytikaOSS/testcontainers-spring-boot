#!/usr/bin/env bash

set -eo pipefail

if ping="$(redis-cli -a $REDIS_PASSWORD ping)" && [ "$ping" = 'PONG' ]; then
	exit 0
fi

exit 1