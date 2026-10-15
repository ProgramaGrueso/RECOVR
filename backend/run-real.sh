#!/usr/bin/env bash
# Arranca el backend en modo REAL: sin datos de demostración. Crea el catálogo oficial (servicios, cabinas y las 4 terapeutas)
# y, si todavía no existe ningún administrador, el primero a partir de ADMIN_EMAIL y ADMIN_PASSWORD (cambio de clave obligatorio al ingresar).
# Los demás accesos (recepción, terapeutas, otros admin) los crea ese administrador desde el panel: "Usuarios y accesos".
set -euo pipefail
cd "$(dirname "$0")"
docker compose up -d || docker start recovr-mysql
echo "Esperando a MySQL..."
until [ "$(docker inspect -f '{{.State.Health.Status}}' recovr-mysql 2>/dev/null)" = "healthy" ]; do sleep 2; done
unset SEED_DEMO_DATA
: "${JWT_SECRET:?Define JWT_SECRET (Base64, 32 bytes o más): openssl rand -base64 48}"
mvn spring-boot:run
