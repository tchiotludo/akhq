#!/bin/bash

set -e

if [ "${AKHQ_CONFIGURATION}" ]; then
    echo "${AKHQ_CONFIGURATION}" > /app/application.yml
fi

exec "$@"
