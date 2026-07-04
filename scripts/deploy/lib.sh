#!/usr/bin/env sh

set -eu

log() {
  printf '%s %s\n' "$(date -u '+%Y-%m-%dT%H:%M:%SZ')" "$*"
}

fail() {
  log "ERROR: $*" >&2
  exit 10
}

require_command() {
  command -v "$1" >/dev/null 2>&1 || fail "Required command is unavailable: $1"
}

require_var() {
  eval "value=\${$1:-}"
  [ -n "$value" ] || fail "Required variable is unset: $1"
}

validate_image_ref() {
  printf '%s' "$1" | grep -Eq '^.+@sha256:[0-9a-f]{64}$' \
    || fail "Production image must be pinned by sha256 digest"
}

compose() {
  docker compose --env-file "$APP_DIR/.env" --env-file "$RELEASE_ENV" \
    -f "$APP_DIR/compose.prod.yaml" "$@"
}

retry() {
  attempts="$1"
  delay="$2"
  shift 2
  current=1

  while ! "$@"; do
    [ "$current" -lt "$attempts" ] || return 1
    log "Attempt $current failed; retrying in ${delay}s"
    sleep "$delay"
    current=$((current + 1))
  done
}
