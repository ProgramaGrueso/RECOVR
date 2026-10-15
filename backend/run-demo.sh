#!/usr/bin/env bash
# Levanta MySQL (Docker) y el backend con los datos demo de octubre a diciembre.
set -euo pipefail
cd "$(dirname "$0")"
docker compose up -d || docker start recovr-mysql
echo "Esperando a MySQL..."
until [ "$(docker inspect -f '{{.State.Health.Status}}' recovr-mysql 2>/dev/null)" = "healthy" ]; do sleep 2; done
export SEED_DEMO_DATA=true
export DEMO_PASSWORD="${DEMO_PASSWORD:-Demo12345!}"
mvn spring-boot:run
