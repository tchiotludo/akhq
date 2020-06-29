FROM openjdk:11-jre-slim

WORKDIR /app
COPY docker /
ENV MICRONAUT_CONFIG_FILES=/app/application.yml
ENTRYPOINT ["docker-entrypoint.sh"]
CMD ["./akhq"]
