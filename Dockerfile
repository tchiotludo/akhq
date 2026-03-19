FROM acrlvfsshareduksouth001.azurecr.io/baseimages/openjdk/jdk:25-distroless

ARG JAR_FILE=build/libs/akhq-*-all.jar

WORKDIR /app
COPY ${JAR_FILE} /app/akhq.jar
ENV MICRONAUT_CONFIG_FILES=/app/application.yml
ENTRYPOINT ["java","-jar","/app/akhq.jar"]
