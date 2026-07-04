#!/usr/bin/env sh

set -eu

SCRIPT_DIR=$(CDPATH='' cd -- "$(dirname -- "$0")" && pwd)
# shellcheck disable=SC1091
. "$SCRIPT_DIR/lib.sh"

for variable in APP_DIR BACKUP_DIR SOURCE_SHA BACKEND_IMAGE FRONTEND_IMAGE \
  REQUESTED_BY APPROVED_BY APPROVED_AT; do
  require_var "$variable"
done
require_command docker
require_command git

validate_image_ref "$BACKEND_IMAGE"
validate_image_ref "$FRONTEND_IMAGE"
printf '%s' "$SOURCE_SHA" | grep -Eq '^[0-9a-f]{40}$' || fail "SOURCE_SHA is invalid"

release_dir="$APP_DIR/.release"
mkdir -p "$release_dir"
RELEASE_ENV="$release_dir/current.env"
PREVIOUS_RELEASE_ENV="$release_dir/previous.env"
export RELEASE_ENV PREVIOUS_RELEASE_ENV

docker_config=$(mktemp -d "$release_dir/docker-config.XXXXXX")
cleanup() {
  rm -rf "$docker_config"
}
trap cleanup EXIT INT TERM
DOCKER_CONFIG="$docker_config"
export DOCKER_CONFIG

if command -v flock >/dev/null 2>&1; then
  exec 9>"$release_dir/deploy.lock"
  flock -n 9 || fail "Another production deployment is already running"
fi

cd "$APP_DIR"
test -f .env || fail "Server-side .env is missing"
test -f compose.prod.yaml || fail "compose.prod.yaml is missing"

if ! git diff --quiet || ! git diff --cached --quiet; then
  fail "Tracked files on the VPS contain unapproved drift"
fi

retry 3 5 git fetch origin main
git checkout main
git pull --ff-only origin main
[ "$(git rev-parse HEAD)" = "$SOURCE_SHA" ] \
  || fail "VPS source does not match approved source SHA"

if [ -f "$RELEASE_ENV" ]; then
  cp "$RELEASE_ENV" "$PREVIOUS_RELEASE_ENV"
fi

PREVIOUS_SOURCE_SHA=
if [ -f "$PREVIOUS_RELEASE_ENV" ]; then
  PREVIOUS_SOURCE_SHA=$(awk -F= '$1 == "SOURCE_SHA" {print $2}' "$PREVIOUS_RELEASE_ENV")
fi
export PREVIOUS_SOURCE_SHA

if [ -n "$PREVIOUS_SOURCE_SHA" ]; then
  git merge-base --is-ancestor "$PREVIOUS_SOURCE_SHA" "$SOURCE_SHA" \
    || fail "Approved source is not a forward release from the previous version"
  migration_changes=$(git diff --name-status "$PREVIOUS_SOURCE_SHA..$SOURCE_SHA" \
    -- backend/src/main/resources/db/migration/)
  if printf '%s\n' "$migration_changes" | awk 'NF && $1 != "A" {exit 1}'; then
    :
  else
    fail "Applied Flyway migration files were modified, deleted, or renamed"
  fi
fi

umask 077
{
  printf 'BACKEND_IMAGE=%s\n' "$BACKEND_IMAGE"
  printf 'FRONTEND_IMAGE=%s\n' "$FRONTEND_IMAGE"
  printf 'SOURCE_SHA=%s\n' "$SOURCE_SHA"
} >"$RELEASE_ENV"

compose config --quiet
backup_id=$("$SCRIPT_DIR/backup-database.sh") || exit $?
log "Backup gate passed for $backup_id"

if [ -n "${GHCR_USERNAME:-}" ] && [ -n "${GHCR_TOKEN:-}" ]; then
  printf '%s' "$GHCR_TOKEN" | docker login ghcr.io -u "$GHCR_USERNAME" --password-stdin >/dev/null
fi

retry 3 5 compose pull backend frontend
if ! compose up -d --remove-orphans; then
  log "Deployment failed; attempting application rollback"
  [ -f "$PREVIOUS_RELEASE_ENV" ] \
    && "$SCRIPT_DIR/rollback-release.sh" \
    || exit 30
  exit 30
fi

verification_started_at=$(date -u '+%Y-%m-%dT%H:%M:%SZ')
if ! "$SCRIPT_DIR/smoke-test.sh"; then
  log "Verification failed; attempting application rollback"
  [ -f "$PREVIOUS_RELEASE_ENV" ] \
    && "$SCRIPT_DIR/rollback-release.sh" \
    || exit 40
  exit 40
fi

for service in backend frontend; do
  container_id=$(compose ps -q "$service")
  running_image=$(docker inspect --format '{{.Config.Image}}' "$container_id")
  if [ "$service" = backend ]; then
    expected_image=$BACKEND_IMAGE
  else
    expected_image=$FRONTEND_IMAGE
  fi
  [ "$running_image" = "$expected_image" ] \
    || fail "Running $service image differs from approved digest"
done

VERIFICATION_STARTED_AT="$verification_started_at"
VERIFICATION_COMPLETED_AT=$(date -u '+%Y-%m-%dT%H:%M:%SZ')
BACKUP_ID="$backup_id"
export VERIFICATION_STARTED_AT VERIFICATION_COMPLETED_AT BACKUP_ID
"$SCRIPT_DIR/release-manifest.sh"

log "Production release $SOURCE_SHA is healthy"
docker image prune -f >/dev/null
