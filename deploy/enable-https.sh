#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
domain=$(awk -F= '$1=="DOMAIN" {print substr($0,index($0,"=")+1)}' deploy/.env)
email=$(awk -F= '$1=="LETSENCRYPT_EMAIL" {print substr($0,index($0,"=")+1)}' deploy/.env)
[[ "$domain" =~ ^[a-z0-9]([a-z0-9.-]*[a-z0-9])?$ && "$domain" == *.* && "$domain" != *example* ]] || { echo 'Set a real domain with DNS pointing to this server.' >&2; exit 1; }
compose=(docker compose --env-file deploy/.env -f docker-compose.yml)
"${compose[@]}" run --rm certbot certonly --webroot --webroot-path /var/www/certbot \
    --domain "$domain" --email "$email" --agree-tos --no-eff-email
backup=$(mktemp)
trap 'rm -f "$backup"' EXIT
cp deploy/runtime/nginx/default.conf.template "$backup"
cp deploy/nginx/https.conf.template deploy/runtime/nginx/default.conf.template
if ! "${compose[@]}" exec -T nginx sh -c '/docker-entrypoint.d/20-envsubst-on-templates.sh && nginx -t'; then
    cp "$backup" deploy/runtime/nginx/default.conf.template
    "${compose[@]}" restart nginx
    exit 1
fi
"${compose[@]}" exec -T nginx nginx -s reload
echo 'HTTPS enabled. Install the certificate renewal timer from deploy/systemd/.'
