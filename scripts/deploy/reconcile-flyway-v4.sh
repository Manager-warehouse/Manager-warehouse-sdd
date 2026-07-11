#!/usr/bin/env sh

set -eu

SCRIPT_DIR=$(CDPATH='' cd -- "$(dirname -- "$0")" && pwd)
# shellcheck disable=SC1091
. "$SCRIPT_DIR/lib.sh"

require_var APP_DIR
require_var RELEASE_ENV

legacy_checksum=1730970908
resolved_checksum=-2044919289

history_table_exists=$(compose exec -T db sh -c \
  'psql -v ON_ERROR_STOP=1 -At -U "$POSTGRES_USER" -d "$POSTGRES_DB"' <<'SQL'
SELECT (to_regclass('public.flyway_schema_history') IS NOT NULL)::text;
SQL
)
history_table_exists=$(printf '%s' "$history_table_exists" | tr -d '\r')
if [ "$history_table_exists" != "true" ]; then
  log "Flyway history does not exist yet; V4 reconciliation is not required"
  exit 0
fi

read_v4_checksum() {
  compose exec -T db sh -c \
    'psql -v ON_ERROR_STOP=1 -At -U "$POSTGRES_USER" -d "$POSTGRES_DB"' <<'SQL'
SELECT COALESCE((
    SELECT checksum::text
    FROM flyway_schema_history
    WHERE version = '4' AND success = TRUE
    ORDER BY installed_rank DESC
    LIMIT 1
), '');
SQL
}

current_checksum=$(read_v4_checksum | tr -d '\r')
case "$current_checksum" in
  "$resolved_checksum")
    log "Flyway V4 history already matches the immutable migration"
    exit 0
    ;;
  "$legacy_checksum")
    log "Reconciling legacy Flyway V4 after verified database backup"
    ;;
  "")
    log "Flyway V4 has not been applied yet; reconciliation is not required"
    exit 0
    ;;
  *)
    fail "Unexpected Flyway V4 checksum: $current_checksum"
    ;;
esac

compose exec -T db sh -c \
  'psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB"' <<'SQL'
BEGIN;

ALTER TABLE delivery_order_item_allocations
    ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN IF NOT EXISTS version INTEGER NOT NULL DEFAULT 0;

UPDATE delivery_order_item_allocations SET status = 'ACTIVE' WHERE status IS NULL;
UPDATE delivery_order_item_allocations SET version = 0 WHERE version IS NULL;
ALTER TABLE delivery_order_item_allocations
    ALTER COLUMN status SET DEFAULT 'ACTIVE',
    ALTER COLUMN status SET NOT NULL,
    ALTER COLUMN version SET DEFAULT 0,
    ALTER COLUMN version SET NOT NULL;

DO $repair$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conrelid = 'delivery_order_item_allocations'::regclass
          AND conname = 'ck_delivery_order_item_allocations_status'
          AND (
              pg_get_constraintdef(oid) NOT LIKE '%ACTIVE%'
              OR pg_get_constraintdef(oid) NOT LIKE '%CANCELLED%'
          )
    ) THEN
        RAISE EXCEPTION 'Existing allocation status constraint differs from Flyway V4';
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conrelid = 'delivery_order_item_allocations'::regclass
          AND conname = 'ck_delivery_order_item_allocations_status'
    ) THEN
        ALTER TABLE delivery_order_item_allocations
            ADD CONSTRAINT ck_delivery_order_item_allocations_status
            CHECK (status IN ('ACTIVE', 'CANCELLED'));
    END IF;
END
$repair$;

ALTER TABLE delivery_orders
    ADD COLUMN IF NOT EXISTS version INTEGER NOT NULL DEFAULT 0;
UPDATE delivery_orders SET version = 0 WHERE version IS NULL;
ALTER TABLE delivery_orders
    ALTER COLUMN version SET DEFAULT 0,
    ALTER COLUMN version SET NOT NULL;

DO $repair$
DECLARE
    affected_rows INTEGER;
BEGIN
    UPDATE flyway_schema_history
    SET checksum = -2044919289,
        description = 'allocation status and version',
        type = 'SQL',
        script = 'V4__allocation_status_and_version.sql'
    WHERE version = '4'
      AND checksum = 1730970908
      AND success = TRUE;

    GET DIAGNOSTICS affected_rows = ROW_COUNT;
    IF affected_rows <> 1 THEN
        RAISE EXCEPTION 'Expected one legacy Flyway V4 row, updated %', affected_rows;
    END IF;
END
$repair$;

COMMIT;
SQL

current_checksum=$(read_v4_checksum | tr -d '\r')
[ "$current_checksum" = "$resolved_checksum" ] \
  || fail "Flyway V4 reconciliation verification failed"

log "Flyway V4 schema and history reconciliation completed"
