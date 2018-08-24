FROM openjdk:8-jdk as builder
COPY . /app
WORKDIR /app
RUN echo -e 'http://dl-cdn.alpinelinux.org/alpine/edge/main\nhttp://dl-cdn.alpinelinux.org/alpine/edge/community\nhttp://dl-cdn.alpinelinux.org/alpine/edge/testing' > /etc/apk/repositories && \
    apk add --no-cache yarn && \
    yarn install && \
    ./gradlew jar


FROM openjdk:8-jre
WORKDIR /app
COPY --from=builder /app/build/libs/kafkahq-*.jar /app/kafkahq.jar
CMD ["/usr/bin/java", "-jar", "/app/kafkahq.jar", "prod"]