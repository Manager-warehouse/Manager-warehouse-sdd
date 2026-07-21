# Quickstart: CD Rehearsal And Release

## 1. Repository preparation

1. Work only on `chore/cd-production-hardening` or a later `chore/*` branch.
2. Configure branch protection so production deployment is reachable only after required checks and review.
3. Configure the GitHub `production` environment with a required reviewer.
4. Configure registry and VPS credentials as environment secrets; never copy `.env` into GitHub artifacts.

## 2. VPS preparation

1. Keep server `.env` outside source control with owner-only permissions.
2. Create durable release and backup directories outside the repository.
3. Verify Docker Compose, disk headroom, registry login and SSH host fingerprint.
4. Keep at least the current and previous healthy release manifests/images.

## 3. Rehearsal

1. Run all pull-request gates.
2. Publish images for a rehearsal commit and record their digests.
3. Deploy the manifest to a non-production host/environment.
4. Force a health-check failure and verify rollback completes within 10 minutes.
5. Verify no database downgrade or automatic restore occurs.
6. Scan captured logs/artifacts for secret leakage.

## 4. First production release

1. Confirm all checks and review are green.
2. Review source SHA, image digests, scan results and migration compatibility.
3. Approve the production environment deployment.
4. Observe backup, deployment, health and public smoke checks.
5. Compare running image digests with the approved release manifest.
6. Preserve release evidence and review it after completion.

## 5. Stop conditions

Stop and do not override automation when backup validation, image identity,
production drift, migration compatibility, health checks or rollback safety is
uncertain. Escalate database restore as a separate approved incident procedure.
