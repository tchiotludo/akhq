#!/usr/bin/env sh

set -e

if [ "${KAFKAHQ_CONFIGURATION}" ]; then
    echo ${KAFKAHQ_CONFIGURATION} > /app/application.conf
fi

if [ "${KAFKAHQ_CONFIGURATION_FILE}" ]; then
    ln -s ${KAFKAHQ_CONFIGURATION_FILE} /app/application.conf
fi

exec "$@"
