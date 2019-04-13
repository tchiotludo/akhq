FROM openjdk:8-jre-alpine

RUN apk add --update \
    libc6-compat \
    java-snappy-native \
  && rm -rf /var/cache/apk/*

WORKDIR /app
COPY docker /
ENV MICRONAUT_CONFIG_FILES=/app/application.yml
ENTRYPOINT ["docker-entrypoint.sh"]
CMD ["./kafkahq"]