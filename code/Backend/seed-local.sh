#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

export LOCAL_SEED_RUN=true
export LOCAL_SEED_EMAIL="${LOCAL_SEED_EMAIL:-seed@example.co}"
export LOCAL_SEED_PASSWORD="${LOCAL_SEED_PASSWORD:-LocalSeed1}"

if command -v mvn >/dev/null 2>&1; then
    exec mvn spring-boot:run "$@"
fi

exec sh ./mvnw spring-boot:run "$@"