#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
[[ -s deploy/.env && -s deploy/runtime/nginx/default.conf.template ]] || { echo 'Run deploy/init-env.sh first.' >&2; exit 1; }
docker compose --env-file deploy/.env -f docker-compose.yml up -d --build --wait
