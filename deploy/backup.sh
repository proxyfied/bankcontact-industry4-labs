#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
umask 077
mkdir -p deploy/backups
output="deploy/backups/bankcontact-$(date -u +%Y%m%dT%H%M%SZ).dump"
docker compose --env-file deploy/.env -f docker-compose.yml exec -T db \
    pg_dump -U postgres -d bankcontact_prod -Fc > "$output"
echo "Backup saved to $output"
