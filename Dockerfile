FROM openjdk:11-jre-slim

# install curl
RUN apt-get update && \
    apt-get install -y \
      curl && \
    apt-get upgrade -y &&\ 
    rm -rf /var/lib/apt/lists/* && \
    apt-get clean

WORKDIR /app
COPY docker /
ENV MICRONAUT_CONFIG_FILES=/app/application.yml
# Create user
RUN useradd -ms /bin/bash akhq
# Chown to write configuration
RUN chown -R akhq /app
# Use the 'akhq' user
USER akhq
ENTRYPOINT ["docker-entrypoint.sh"]
CMD ["./akhq"]
