FROM eclipse-temurin:21-jre
WORKDIR /app

# Create non-root user and prepare writable dirs
RUN addgroup --system app && adduser --system --ingroup app app \
    && mkdir -p /app/config /app/bin \
    && chown -R app:app /app

# Copy artifacts with correct ownership
COPY --chown=app:app target/*.jar /app/app.jar
COPY --chown=app:app entrypoint.sh /app/bin/entrypoint.sh
RUN chmod +x /app/bin/entrypoint.sh

# Sensible JVM defaults for containers
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -Djava.security.egd=file:/dev/./urandom"

# Allow overriding port via env
ENV PORT=8080
EXPOSE 8080

# Spring will import optional secrets file if present
ENV SPRING_CONFIG_IMPORT=optional:file:/app/config/secrets.yml

# Drop privileges
USER app

ENTRYPOINT ["/app/bin/entrypoint.sh"]
