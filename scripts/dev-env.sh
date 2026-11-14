#!/usr/bin/env sh
# Gera o arquivo .env de desenvolvimento com credenciais aleatórias (nunca versionado).
set -eu
cd "$(dirname "$0")/.."
if [ -f .env ]; then
  echo ".env já existe; nada a fazer." >&2
  exit 0
fi
rand() { openssl rand -hex "$1"; }
umask 077
cat > .env <<ENV
SPRING_PROFILES_ACTIVE=dev
DATABASE_URL=jdbc:postgresql://localhost:5432/hae
DATABASE_USERNAME=hae
DATABASE_PASSWORD=$(rand 16)
JWT_SECRET=$(rand 32)
INITIAL_ADMIN_EMAIL=admin@hae.local
INITIAL_ADMIN_PASSWORD=$(rand 8)
ENV
echo "Arquivo .env criado. Senha do admin local: veja INITIAL_ADMIN_PASSWORD em .env"
