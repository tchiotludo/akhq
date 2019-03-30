FROM openjdk:8-jre-alpine
WORKDIR /app
COPY docker /
ENV MICRONAUT_CONFIG_FILES=/app/application.yml
ENTRYPOINT ["docker-entrypoint.sh"]
CMD ["./kafkahq"]