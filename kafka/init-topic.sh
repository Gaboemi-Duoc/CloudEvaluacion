#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
docker compose exec -T kafka kafka-topics.sh --bootstrap-server kafka:9092 \
  --create --if-not-exists --topic cloud-commands \
  --partitions 3 --replication-factor 1
docker compose exec -T kafka kafka-topics.sh --bootstrap-server kafka:9092 \
  --create --if-not-exists --topic cloud-notifications \
  --partitions 3 --replication-factor 1