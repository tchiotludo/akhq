FROM eclipse-temurin:11-jre-alpine
# install curl
RUN apk update && \
    apk upgrade --available && \
    apk add curl 

HEALTHCHECK --interval=1m --timeout=30s --retries=3 \
  CMD curl --fail http://localhost:28081/health || exit 1

WORKDIR /app
COPY docker /
ENV MICRONAUT_CONFIG_FILES=/app/application.yml
# Create user
RUN addgroup "akhq" \
    && adduser \
    --disabled-password \
    --shell "/sbin/nologin" \
    --gecos "akhq" \
    --ingroup "akhq" \
    --no-create-home \
    "akhq" 

# Chown to write configuration
RUN chown -R akhq:akhq /app
# Use the 'akhq' user
USER akhq
ENTRYPOINT ["docker-entrypoint.sh"]
CMD ["./akhq"]
