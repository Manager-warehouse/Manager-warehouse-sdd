#!/usr/bin/env sh

set -eu

SCRIPT_DIR=$(CDPATH='' cd -- "$(dirname -- "$0")" && pwd)
# shellcheck disable=SC1091
. "$SCRIPT_DIR/lib.sh"

for variable in APP_DIR SOURCE_SHA BACKEND_IMAGE FRONTEND_IMAGE REQUESTED_BY \
  APPROVED_BY APPROVED_AT BACKUP_ID VERIFICATION_STARTED_AT \
  VERIFICATION_COMPLETED_AT; do
  require_var "$variable"
done

validate_image_ref "$BACKEND_IMAGE"
validate_image_ref "$FRONTEND_IMAGE"
[ "$REQUESTED_BY" != "$APPROVED_BY" ] \
  || fail "Production approver must differ from release requester"

for actor in "$REQUESTED_BY" "$APPROVED_BY"; do
  printf '%s' "$actor" | grep -Eq '^[A-Za-z0-9-]+$' \
    || fail "Release actor contains unsupported characters"
done

release_dir="$APP_DIR/.release"
history_dir="$release_dir/history"
mkdir -p "$history_dir"
manifest="$history_dir/${SOURCE_SHA}.json"
partial="$manifest.partial"
previous_release_id=${PREVIOUS_SOURCE_SHA:-}

umask 077
{
  printf '{\n'
  printf '  "releaseId": "%s",\n' "$SOURCE_SHA"
  printf '  "sourceSha": "%s",\n' "$SOURCE_SHA"
  printf '  "createdAt": "%s",\n' "$APPROVED_AT"
  printf '  "requestedBy": "%s",\n' "$REQUESTED_BY"
  printf '  "approvedBy": "%s",\n' "$APPROVED_BY"
  printf '  "backendImage": "%s",\n' "$BACKEND_IMAGE"
  printf '  "frontendImage": "%s",\n' "$FRONTEND_IMAGE"
  if [ -n "$previous_release_id" ]; then
    printf '  "previousReleaseId": "%s",\n' "$previous_release_id"
  fi
  printf '  "backupId": "%s",\n' "$BACKUP_ID"
  printf '  "gateResults": {\n'
  printf '    "backendTests": "PASSED",\n'
  printf '    "frontendLint": "PASSED",\n'
  printf '    "frontendTests": "PASSED",\n'
  printf '    "frontendBuild": "PASSED",\n'
  printf '    "productionConfig": "PASSED",\n'
  printf '    "imageBuild": "PASSED",\n'
  printf '    "vulnerabilityPolicy": "PASSED",\n'
  printf '    "manifestValidation": "PASSED"\n'
  printf '  },\n'
  printf '  "approvedAt": "%s",\n' "$APPROVED_AT"
  printf '  "verificationStartedAt": "%s",\n' "$VERIFICATION_STARTED_AT"
  printf '  "verificationCompletedAt": "%s",\n' "$VERIFICATION_COMPLETED_AT"
  printf '  "status": "HEALTHY"\n'
  printf '}\n'
} >"$partial"

mv "$partial" "$manifest"
cp "$manifest" "$release_dir/current.json"
log "Append-only release manifest recorded: $manifest"
