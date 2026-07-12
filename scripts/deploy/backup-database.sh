#!/usr/bin/env sh

set -eu

SCRIPT_DIR=$(CDPATH='' cd -- "$(dirname -- "$0")" && pwd)
# shellcheck disable=SC1091
. "$SCRIPT_DIR/lib.sh"

require_var APP_DIR
require_var BACKUP_DIR
require_var SOURCE_SHA
require_command docker
require_command sha256sum
printf '%s' "$SOURCE_SHA" | grep -Eq '^[0-9a-f]{40}$' || fail "SOURCE_SHA is invalid"

RELEASE_ENV=${RELEASE_ENV:-"$APP_DIR/.release/current.env"}
export RELEASE_ENV

mkdir -p "$BACKUP_DIR"
[ -w "$BACKUP_DIR" ] || fail "Backup directory is not writable"

available_kb=$(df -Pk "$BACKUP_DIR" | awk 'NR == 2 {print $4}')
minimum_kb=${BACKUP_MIN_FREE_KB:-1048576}
printf '%s' "$minimum_kb" | grep -Eq '^[0-9]+$' || fail "BACKUP_MIN_FREE_KB is invalid"
[ "$available_kb" -ge "$minimum_kb" ] \
  || fail "Backup directory has less than ${minimum_kb}KB free"

backup_id="$(date -u '+%Y%m%dT%H%M%SZ')-${SOURCE_SHA}"
partial="$BACKUP_DIR/.${backup_id}.dump.partial"
backup="$BACKUP_DIR/${backup_id}.dump"
metadata="$BACKUP_DIR/${backup_id}.sha256"
metadata_json="$BACKUP_DIR/${backup_id}.json"

cleanup() {
  rm -f "$partial"
}
trap cleanup EXIT INT TERM

log "Creating PostgreSQL backup $backup_id" >&2
# shellcheck disable=SC2016
if ! compose exec -T db sh -c \
  'exec pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Fc' >"$partial"; then
  log "ERROR: PostgreSQL backup failed" >&2
  exit 20
fi

[ -s "$partial" ] || {
  log "ERROR: PostgreSQL backup is empty" >&2
  exit 20
}

mv "$partial" "$backup"
sha256sum "$backup" >"$metadata"
sha256sum -c "$metadata" >/dev/null || {
  log "ERROR: PostgreSQL backup checksum validation failed" >&2
  exit 20
}

size_bytes=$(wc -c <"$backup" | tr -d ' ')
created_at=$(date -u '+%Y-%m-%dT%H:%M:%SZ')
retention_days=${BACKUP_RETENTION_DAYS:-14}
printf '%s' "$retention_days" | grep -Eq '^[0-9]+$' || fail "BACKUP_RETENTION_DAYS is invalid"
{
  printf '{\n'
  printf '  "backupId": "%s",\n' "$backup_id"
  printf '  "createdAt": "%s",\n' "$created_at"
  printf '  "sourceSha": "%s",\n' "$SOURCE_SHA"
  printf '  "sizeBytes": %s,\n' "$size_bytes"
  printf '  "sha256": "%s",\n' "$(awk '{print $1}' "$metadata")"
  printf '  "retentionDays": %s\n' "$retention_days"
  printf '}\n'
} >"$metadata_json"

find "$BACKUP_DIR" -type f \
  \( -name '*.dump' -o -name '*.sha256' -o -name '*.json' \) \
  -mtime "+$retention_days" -delete

trap - EXIT INT TERM
log "Backup created and verified: $backup" >&2
printf '%s\n' "$backup_id"
