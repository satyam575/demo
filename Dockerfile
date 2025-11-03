FROM eclipse-temurin:21-jre
WORKDIR /app

# Optional: run as non-root user
RUN addgroup --system app && adduser --system --ingroup app app
USER app

# Copy the pre-built jar from local build output
# Build first: mvn -DskipTests package
COPY target/*.jar app.jar

# Sensible JVM defaults for containers
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -Djava.security.egd=file:/dev/./urandom"

# Allow overriding port via env
ENV PORT=8080
EXPOSE 8080

# Expand PORT at runtime and start the app
CMD ["sh","-c","exec java -Dserver.port=${PORT:-8080} -jar app.jar"]
