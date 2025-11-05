#!/usr/bin/env sh
set -eu

# Ensure config directory exists and is writable
mkdir -p /app/config

# If secrets are provided via env, materialize them to a file for Spring to import
if [ -n "${SECRETS_YAML_B64:-}" ]; then
  echo "$SECRETS_YAML_B64" | base64 -d > /app/config/secrets.yml
elif [ -n "${SECRETS_YAML:-}" ]; then
  # Write raw YAML (ensure newline at EOF)
  printf "%s\n" "$SECRETS_YAML" > /app/config/secrets.yml
fi

# Launch application
exec java -Djava.security.egd=file:/dev/./urandom -Dserver.port=${PORT:-8080} -jar /app/app.jar

