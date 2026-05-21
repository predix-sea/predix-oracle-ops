#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
docker compose up -d postgres redis rabbitmq
echo "Infra ready. Run: mvn spring-boot:run"
