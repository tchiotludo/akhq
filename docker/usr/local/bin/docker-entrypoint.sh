#!/usr/bin/env sh

set -e

if [ "${AKHQ_CONFIGURATION}" ]; then
    echo "${AKHQ_CONFIGURATION}" > /app/application.yml
fi

# We currently override the base image ENTRYPOINT in our Dockerfile making it harder
# to get access to the CA certificates feature provided by the base image ENTRYPOINT:
# https://github.com/docker-library/docs/blob/master/eclipse-temurin/content.md#can-i-add-my-internal-ca-certificates-to-the-truststore
# To fix this issue, we invoke the base image ENTRYPOINT script (/__cacert_entrypoint.sh) before the arguments.
# Credits to https://superuser.com/a/1460890
exec /__cacert_entrypoint.sh "$@"
