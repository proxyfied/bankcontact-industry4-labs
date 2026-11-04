#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
username=${1:?Usage: bash deploy/assign-role.sh username ROLE_ADMIN|ROLE_MANAGER}
role=${2:?Usage: bash deploy/assign-role.sh username ROLE_ADMIN|ROLE_MANAGER}
[[ "$username" =~ ^[a-z0-9._-]{3,40}$ && "$role" =~ ^ROLE_(ADMIN|MANAGER)$ ]] || { echo 'Invalid username or role.' >&2; exit 1; }
docker compose --env-file deploy/.env -f docker-compose.yml exec -T db \
    psql -U postgres -d bankcontact_prod --set ON_ERROR_STOP=1 --set username="$username" --set role="$role" <<'SQL'
SET search_path TO security;
INSERT INTO sec_user_roles(user_id,role_id)
SELECT u.id,r.id FROM sec_users u CROSS JOIN sec_roles r
WHERE u.username=:'username' AND r.name=:'role'
ON CONFLICT DO NOTHING;
SELECT u.username,r.name FROM sec_users u
JOIN sec_user_roles ur ON ur.user_id=u.id JOIN sec_roles r ON r.id=ur.role_id
WHERE u.username=:'username' ORDER BY r.name;
SQL
