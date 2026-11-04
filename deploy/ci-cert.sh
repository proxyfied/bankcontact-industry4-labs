#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
bash deploy/init-env.sh localhost ci@example.invalid
sed -i 's/HTTP_PORT=80/HTTP_PORT=8088/; s/HTTPS_PORT=443/HTTPS_PORT=8443/' deploy/.env
mkdir -p deploy/runtime/letsencrypt/live/localhost
openssl req -x509 -newkey rsa:2048 -nodes -days 2 -subj '/CN=localhost' \
  -addext 'subjectAltName=DNS:localhost' \
  -keyout deploy/runtime/letsencrypt/live/localhost/privkey.pem \
  -out deploy/runtime/letsencrypt/live/localhost/fullchain.pem > /dev/null 2>&1
cp deploy/nginx/https.conf.template deploy/runtime/nginx/default.conf.template
echo 'Created a self-signed certificate for CI only.'
