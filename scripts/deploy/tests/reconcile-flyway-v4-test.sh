#!/usr/bin/env sh

set -eu

SCRIPT_DIR=$(CDPATH='' cd -- "$(dirname -- "$0")" && pwd)
DEPLOY_DIR=$(CDPATH='' cd -- "$SCRIPT_DIR/.." && pwd)
fixture_dir=$(mktemp -d)
export APP_DIR="$fixture_dir"
export RELEASE_ENV="$fixture_dir/release.env"
export COMPOSE_PROJECT_NAME="wms-flyway-v4-test-$$"

cleanup() {
  docker compose --env-file "$APP_DIR/.env" --env-file "$RELEASE_ENV" \
    -f "$APP_DIR/compose.prod.yaml" down -v >/dev/null 2>&1 || true
  rm -rf "$fixture_dir"
}
trap cleanup EXIT INT TERM

cat >"$APP_DIR/.env" <<'EOF'
DB_USER=wms_user
DB_PASSWORD=test-password
EOF

: >"$RELEASE_ENV"

cat >"$APP_DIR/compose.prod.yaml" <<'EOF'
services:
  db:
    image: postgres:18-alpine
    environment:
      POSTGRES_DB: wms
      POSTGRES_USER: ${DB_USER}
      POSTGRES_PASSWORD: ${DB_PASSWORD}
EOF

docker compose --env-file "$APP_DIR/.env" --env-file "$RELEASE_ENV" \
  -f "$APP_DIR/compose.prod.yaml" up -d db >/dev/null

for attempt in 1 2 3 4 5 6 7 8 9 10; do
  if docker compose --env-file "$APP_DIR/.env" --env-file "$RELEASE_ENV" \
    -f "$APP_DIR/compose.prod.yaml" exec -T db \
    sh -c 'pg_isready -U "$POSTGRES_USER" -d "$POSTGRES_DB"' >/dev/null 2>&1; then
    break
  fi
  sleep 1
done

"$DEPLOY_DIR/reconcile-flyway-v4.sh"

docker compose --env-file "$APP_DIR/.env" --env-file "$RELEASE_ENV" \
  -f "$APP_DIR/compose.prod.yaml" exec -T db \
  sh -c 'psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB"' <<'SQL'
CREATE TABLE delivery_order_item_allocations (id BIGINT PRIMARY KEY);
CREATE TABLE delivery_orders (id BIGINT PRIMARY KEY);
CREATE TABLE flyway_schema_history (
    installed_rank INTEGER NOT NULL,
    version VARCHAR(50),
    description VARCHAR(200) NOT NULL,
    type VARCHAR(20) NOT NULL,
    script VARCHAR(1000) NOT NULL,
    checksum INTEGER,
    success BOOLEAN NOT NULL
);
INSERT INTO flyway_schema_history VALUES (
    4, '4', 'legacy migration', 'SQL', 'V4__legacy.sql', 1730970908, TRUE
);
SQL

"$DEPLOY_DIR/reconcile-flyway-v4.sh"
"$DEPLOY_DIR/reconcile-flyway-v4.sh"

result=$(docker compose --env-file "$APP_DIR/.env" --env-file "$RELEASE_ENV" \
  -f "$APP_DIR/compose.prod.yaml" exec -T db \
  sh -c 'psql -v ON_ERROR_STOP=1 -At -U "$POSTGRES_USER" -d "$POSTGRES_DB"' <<'SQL'
SELECT checksum || '|' || description || '|' || script
FROM flyway_schema_history WHERE version = '4';
SELECT count(*)
FROM information_schema.columns
WHERE table_schema = 'public'
  AND table_name = 'delivery_order_item_allocations'
  AND column_name IN ('status', 'version');
SELECT count(*)
FROM information_schema.columns
WHERE table_schema = 'public'
  AND table_name = 'delivery_orders'
  AND column_name = 'version';
SELECT count(*)
FROM pg_constraint
WHERE conrelid = 'delivery_order_item_allocations'::regclass
  AND conname = 'ck_delivery_order_item_allocations_status';
SQL
)
result=$(printf '%s' "$result" | tr -d '\r')
expected='-2044919289|allocation status and version|V4__allocation_status_and_version.sql
2
1
1'

[ "$result" = "$expected" ] || {
  printf 'Unexpected reconciliation result:\n%s\n' "$result" >&2
  exit 1
}

printf '%s\n' "Flyway V4 reconciliation test passed"
