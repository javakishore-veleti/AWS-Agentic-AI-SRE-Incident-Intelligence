# SEC-003 plan

1. Replace the database password and JWT secret literals in the five `application.yml` files with `${DB_PASSWORD}` and `${JWT_SECRET}`. Inventory has no JWT setting.
2. Point Postgres Compose at `${DB_PASSWORD}` and stop `docker-all-up.sh` and `middleware-all-up.sh` when the required variables are empty.
3. Set the existing test database password and test JWT value on Surefire so the suite can resolve the placeholders.
4. Add a test that reads the five application files and the Compose file, rejects the old literals, and resolves the placeholders from a supplied environment.
5. Move the shop session from `localStorage` to `sessionStorage`, and delete a leftover `localStorage` value.
6. Update ARCHITECTURE, DESIGN, and CURRENT_STATE so they describe environment loading and `sessionStorage`.
