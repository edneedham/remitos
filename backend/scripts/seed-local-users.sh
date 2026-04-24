#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SQL_FILE="${ROOT_DIR}/scripts/seed-local-users.sql"

if ! command -v psql >/dev/null 2>&1; then
  echo "psql is required (install Postgres client tools)." >&2
  exit 1
fi

if [[ -f "${ROOT_DIR}/.env" ]]; then
  set -a
  # shellcheck disable=SC1090
  source "${ROOT_DIR}/.env"
  set +a
fi

DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5433}"
DB_USER="${DB_USER:-postgres}"
DB_PASSWORD="${DB_PASSWORD:-postgres}"
DB_NAME="${DB_NAME:-server}"
DB_SSLMODE="${DB_SSLMODE:-disable}"

export PGPASSWORD="${DB_PASSWORD}"

echo "Seeding local users into ${DB_USER}@${DB_HOST}:${DB_PORT}/${DB_NAME} (sslmode=${DB_SSLMODE})"
psql "postgresql://${DB_USER}@${DB_HOST}:${DB_PORT}/${DB_NAME}?sslmode=${DB_SSLMODE}" -v ON_ERROR_STOP=1 -f "${SQL_FILE}"

echo
echo "Done. Logins (password for all): LocalSeed123!"
echo "- SEEDTRIAL / seed-trial-owner@local.test   (company_owner)"
echo "- SEEDTRIAL / trial_wh_admin                (username; warehouse_admin)"
echo "- SEEDTRIAL / trial_operator                (username; operator)"
echo "- SEEDTRIAL / trial_readonly                (username; read_only)"
echo "- SEEDPAID  / seed-paid-owner@local.test   (company_owner; premium)"
echo "- SEEDFREE  / seed-free-owner@local.test   (company_owner; free)"
echo "- SEEDEXPIRED / seed-expired-owner@local.test (expired trial)"
echo "- SEEDARCH  / seed-archived-owner@local.test (archived company)"
