FROM eclipse-temurin:17-jre

# install curl
RUN apt-get update && \
    apt-get install -y \
      curl && \
    apt-get upgrade -y &&\
    rm -rf /var/lib/apt/lists/* && \
    apt-get clean

HEALTHCHECK --interval=1m --timeout=30s --retries=3 \
  CMD curl --fail http://localhost:28081/health || exit 1

WORKDIR /app
# Create user
RUN useradd -ms /bin/bash akhq
COPY --chown=akhq:akhq docker /
ENV MICRONAUT_CONFIG_FILES=/app/application.yml
# Use the 'akhq' user
USER akhq
ENTRYPOINT ["docker-entrypoint.sh"]
CMD ["./akhq"]
