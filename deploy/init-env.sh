#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
domain=${1:?Usage: bash deploy/init-env.sh domain email}
email=${2:?Usage: bash deploy/init-env.sh domain email}
[[ "$domain" =~ ^[a-z0-9]([a-z0-9.-]*[a-z0-9])?$ ]] || { echo 'Invalid domain.' >&2; exit 1; }
[[ "$email" =~ ^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+$ ]] || { echo 'Invalid email.' >&2; exit 1; }
[[ ! -e deploy/.env ]] || { echo 'deploy/.env already exists; credentials were preserved.' >&2; exit 1; }
umask 077
mkdir -p deploy/runtime/{nginx,acme,letsencrypt}
{
    printf 'DOMAIN=%s\nLETSENCRYPT_EMAIL=%s\nHTTP_PORT=80\nHTTPS_PORT=443\n' "$domain" "$email"
    printf 'DB_ADMIN_PASSWORD=%s\n' "$(openssl rand -hex 32)"
    printf 'APP_DB_PASSWORD=%s\n' "$(openssl rand -hex 32)"
} > deploy/.env
cp deploy/nginx/http.conf.template deploy/runtime/nginx/default.conf.template
echo 'Created deploy/.env with random passwords. Keep this file private.'
