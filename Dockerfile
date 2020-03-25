FROM openjdk:11-slim

WORKDIR /app
COPY docker /
ENV MICRONAUT_CONFIG_FILES=/app/application.yml
ENTRYPOINT ["docker-entrypoint.sh"]
CMD ["./akhq"]
