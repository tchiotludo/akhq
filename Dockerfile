FROM openjdk:8-jre-alpine
WORKDIR /app
COPY docker /
ENTRYPOINT ["docker-entrypoint.sh"]
CMD ["./kafkahq"]