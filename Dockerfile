FROM 173672169127.dkr.ecr.us-east-1.amazonaws.com/devrev/base-java-dev:stable

HEALTHCHECK --interval=1m --timeout=30s --retries=3 \
  CMD curl --fail http://localhost:28081/health || exit 1

COPY docker /
ENV MICRONAUT_CONFIG_FILES=/app/application.yml
ENTRYPOINT ["/bin/bash", "/usr/local/bin/docker-entrypoint.sh"]
CMD ["/app/akhq"]
