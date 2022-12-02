FROM eclipse-temurin:11-jre

# install curl
RUN apt-get update && \
    apt-get install -y \
      curl && \
    apt-get upgrade -y &&\
    rm -rf /var/lib/apt/lists/* && \
    apt-get clean && \
    wget https://github.com/mikefarah/yq/releases/latest/download/yq_linux_amd64 -O /usr/bin/yq && \
    chmod +x /usr/bin/yq

ENV MICRONAUT_CONFIG_FILES=/app/application.yml

HEALTHCHECK --interval=1m --timeout=30s --retries=3 \
  CMD curl --fail http://localhost:$(yq '.micronaut.server.port' $MICRONAUT_CONFIG_FILES)/health || exit 1

WORKDIR /app
COPY docker /
# Create user
RUN useradd -ms /bin/bash akhq
# Chown to write configuration
RUN chown -R akhq /app
# Use the 'akhq' user
USER akhq
ENTRYPOINT ["docker-entrypoint.sh"]
CMD ["./akhq"]
