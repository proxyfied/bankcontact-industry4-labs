#!/usr/bin/env bash
set -euo pipefail
psql --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" \
  --set ON_ERROR_STOP=1 --set app_user="$APP_DB_USER" \
  --set app_password="$APP_DB_PASSWORD" --set db_name="$POSTGRES_DB" <<'SQL'
CREATE ROLE :"app_user" LOGIN PASSWORD :'app_password' NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION;
GRANT CONNECT ON DATABASE :"db_name" TO :"app_user";
REVOKE CREATE ON SCHEMA public FROM PUBLIC;
CREATE SCHEMA security AUTHORIZATION :"app_user";
ALTER ROLE :"app_user" IN DATABASE :"db_name" SET search_path TO security;
SQL
