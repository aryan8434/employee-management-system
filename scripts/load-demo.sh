#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
docker compose exec -T db sh -c 'psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB"' < database/demo-data.sql
echo 'Demo records are ready. Refresh the employee list.'
