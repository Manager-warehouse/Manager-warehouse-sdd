#!/usr/bin/env sh

set -eu

SCRIPT_DIR=$(CDPATH='' cd -- "$(dirname -- "$0")" && pwd)
# shellcheck disable=SC1091
. "$SCRIPT_DIR/lib.sh"

require_var APP_DIR
require_command docker
require_command curl

RELEASE_ENV=${RELEASE_ENV:-"$APP_DIR/.release/current.env"}
export RELEASE_ENV

deadline=$(( $(date +%s) + ${HEALTH_TIMEOUT_SECONDS:-180} ))

while [ "$(date +%s)" -lt "$deadline" ]; do
  healthy=true
  for service in db backend frontend; do
    container_id=$(compose ps -q "$service")
    if [ -z "$container_id" ]; then
      healthy=false
      break
    fi
    status=$(docker inspect --format \
      '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' \
      "$container_id")
    [ "$status" = healthy ] || {
      healthy=false
      break
    }
  done
  [ "$healthy" = true ] && break
  sleep 5
done

[ "${healthy:-false}" = true ] || {
  log "ERROR: Container health verification timed out" >&2
  exit 40
}

if [ -n "${PUBLIC_URL:-}" ]; then
  retry 3 5 curl --fail --silent --show-error --max-time 15 \
    "${PUBLIC_URL%/}/" >/dev/null || {
      log "ERROR: Public smoke test failed" >&2
      exit 40
    }
fi

log "Container health and public smoke checks passed"
