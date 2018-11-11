FROM openjdk:8-jdk-alpine as builder
COPY . /app
WORKDIR /app
RUN apk update && \
    apk add --no-cache nodejs-npm && \
    npm install && \
    ./gradlew test && \
    ./gradlew jar

FROM openjdk:8-jre-alpine
WORKDIR /app

COPY docker /
COPY --from=builder /app/build/libs/kafkahq-*.jar /app/kafkahq.jar
ENTRYPOINT ["docker-entrypoint.sh"]
CMD ["./kafkahq"]