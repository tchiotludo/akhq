FROM openjdk:11-jre-slim

WORKDIR /app
COPY docker /
ENV MICRONAUT_CONFIG_FILES=/app/application.yml
USER 10001
ENTRYPOINT ["docker-entrypoint.sh"]
CMD ["./akhq"]
