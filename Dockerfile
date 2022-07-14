FROM openjdk:11-jre-slim

ENV MICRONAUT_CONFIG_FILES=/app/application.yml
RUN adduser --system --uid 1000 --group appuser \
  && usermod -a -G 0,appuser appuser

WORKDIR /app

COPY --chown=appuser:appuser docker /

RUN chmod +x /app/entrypoint

USER appuser

ENTRYPOINT ["/app/entrypoint"]
