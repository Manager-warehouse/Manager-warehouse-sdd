# Flyway Migration Notes

This project is still in early design/development, and some existing migration
files currently share the same Flyway version number.

Do not delete, squash, or renumber migration files as part of normal feature
work. Migration cleanup must be a dedicated task because it can break any
database that has already applied the old history.

Rules:

- Before a database is shared/deployed, a dedicated cleanup task may squash or
  renumber migrations and reset local databases.
- After a migration is applied in a shared/deployed environment, it is
  immutable. Add a new migration for further changes.
- Runnable Flyway history must not contain duplicate versions.
- Never delete business data, `/data`, or `/uploads` during migration cleanup.

Current known cleanup target:

- Duplicate versions exist for `V3`, `V4`, and `V5`; resolve them before using
  Flyway against a durable shared database.
