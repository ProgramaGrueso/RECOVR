#!/usr/bin/env bash
# Levanta MySQL (Docker) y el backend con los datos demo (90 días de historial y agenda hasta el 31 de diciembre).
set -euo pipefail
cd "$(dirname "$0")"
# Variables de backend/.env (copia .env.example). Nada de contraseñas escritas en el script.
if [ -f .env ]; then set -a; . ./.env; set +a; else echo "Falta backend/.env (cp .env.example .env y complétalo)"; exit 1; fi
docker compose up -d || docker start recovr-mysql
echo "Esperando a MySQL..."
until [ "$(docker inspect -f '{{.State.Health.Status}}' recovr-mysql 2>/dev/null)" = "healthy" ]; do sleep 2; done
export SPRING_PROFILES_ACTIVE=demo
mvn spring-boot:run
