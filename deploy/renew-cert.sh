#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
compose=(docker compose --env-file deploy/.env -f docker-compose.yml)
"${compose[@]}" run --rm certbot renew --quiet --webroot --webroot-path /var/www/certbot
"${compose[@]}" exec -T nginx nginx -t
"${compose[@]}" exec -T nginx nginx -s reload
