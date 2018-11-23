#!/usr/bin/env sh

set -e

if [ "${KAFKAHQ_CONFIGURATION}" ]; then
    echo ${KAFKAHQ_CONFIGURATION} > /app/application.conf
fi

exec "$@"
