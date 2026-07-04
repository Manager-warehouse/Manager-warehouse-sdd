#!/usr/bin/env sh

set -eu

SCRIPT_DIR=$(CDPATH='' cd -- "$(dirname -- "$0")" && pwd)
# shellcheck disable=SC1091
. "$SCRIPT_DIR/lib.sh"

require_var APP_DIR
require_var PREVIOUS_RELEASE_ENV
[ -f "$PREVIOUS_RELEASE_ENV" ] || {
  log "ERROR: Previous release manifest is unavailable" >&2
  exit 50
}

RELEASE_ENV="$PREVIOUS_RELEASE_ENV"
export RELEASE_ENV

log "Rolling application images back to the previous release"
compose up -d --remove-orphans backend frontend || {
  log "ERROR: Application rollback failed; database was not restored or downgraded" >&2
  exit 50
}

"$SCRIPT_DIR/smoke-test.sh" || {
  log "ERROR: Rolled-back application is unhealthy; incident response is required" >&2
  exit 50
}

expected_backend=$(awk -F= '$1 == "BACKEND_IMAGE" {print $2}' "$PREVIOUS_RELEASE_ENV")
expected_frontend=$(awk -F= '$1 == "FRONTEND_IMAGE" {print $2}' "$PREVIOUS_RELEASE_ENV")
validate_image_ref "$expected_backend"
validate_image_ref "$expected_frontend"

for service in backend frontend; do
  container_id=$(compose ps -q "$service")
  running_image=$(docker inspect --format '{{.Config.Image}}' "$container_id")
  if [ "$service" = backend ]; then
    expected_image=$expected_backend
  else
    expected_image=$expected_frontend
  fi
  [ "$running_image" = "$expected_image" ] || {
    log "ERROR: Rolled-back $service image differs from the previous digest" >&2
    exit 50
  }
done

log "Application rollback completed; database migration history was unchanged"
