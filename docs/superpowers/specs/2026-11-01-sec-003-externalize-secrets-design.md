# SEC-003 externalize secrets

Issue #7. The database password and the JWT signing secret are no longer written in application configuration or Compose.

## Decision

`application.yml` reads `DB_PASSWORD` and, where a service signs or checks tokens, `JWT_SECRET`. Neither placeholder has a default. The Postgres Compose file reads `DB_PASSWORD` and refuses to start when it is missing. `docker-all-up.sh` and `middleware-all-up.sh` stop with the same requirement. The scripts do not create or print a secret.

The previous committed password and signing secret are removed from the current tree. They are not written into this spec. Git history still contains the old values; this change does not rewrite history. Postgres keeps the password from the first time the data volume was created, so a local volume has to be recreated before the new password is the one Postgres accepts.

Tests already supply `security.jwt.secret` from the shared test properties. Maven Surefire also sets `DB_PASSWORD` and `JWT_SECRET` to the existing test values so an unresolved placeholder does not fail the suite. Those values are test fixtures.

The shop stores `heddle.session` in `sessionStorage`. A value left under that key in `localStorage` is removed and is not copied forward. The desk already uses `sessionStorage`.

Amazon Secrets Manager stays unwired. A root `.env.example` stays absent.
