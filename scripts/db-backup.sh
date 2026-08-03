#!/usr/bin/env bash
set -euo pipefail

# Exports the autotrade app schema from a running Oracle XE container via
# Data Pump (expdp) and copies the resulting dump + a per-table row-count
# manifest out to ./backups/. See docs/runbooks/oracle-backup-restore.md.
#
# Usage: scripts/db-backup.sh [container_name]

CONTAINER_NAME="${1:-autotrade-oracle-xe}"
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="${ROOT_DIR}/.env"

if [ ! -f "$ENV_FILE" ]; then
  echo "Missing $ENV_FILE - copy .env.example and fill in real values first." >&2
  exit 1
fi
set -a
# shellcheck disable=SC1090
source "$ENV_FILE"
set +a

: "${ORACLE_PASSWORD:?ORACLE_PASSWORD not set in .env}"
: "${ORACLE_APP_USER:?ORACLE_APP_USER not set in .env}"
: "${ORACLE_APP_USER_PASSWORD:?ORACLE_APP_USER_PASSWORD not set in .env}"

if ! docker inspect -f '{{.State.Running}}' "$CONTAINER_NAME" >/dev/null 2>&1; then
  echo "Container '$CONTAINER_NAME' is not running." >&2
  exit 1
fi

TIMESTAMP="$(date +%Y%m%d_%H%M%S)"
DUMP_FILE="autotrade_${TIMESTAMP}.dmp"
LOG_FILE="autotrade_${TIMESTAMP}_export.log"
BACKUP_DIR_HOST="${BACKUP_DIR:-${ROOT_DIR}/backups}"

mkdir -p "$BACKUP_DIR_HOST"

echo "Looking up DATA_PUMP_DIR path in ${CONTAINER_NAME}..."
DATA_PUMP_DIR_CONTAINER="$(docker exec -i "$CONTAINER_NAME" sqlplus -s "system/${ORACLE_PASSWORD}@//localhost:1521/XEPDB1" <<'SQL'
SET PAGESIZE 0 FEEDBACK OFF VERIFY OFF HEADING OFF ECHO OFF LINESIZE 200
SELECT directory_path FROM dba_directories WHERE directory_name = 'DATA_PUMP_DIR';
EXIT
SQL
)"
DATA_PUMP_DIR_CONTAINER="$(echo "$DATA_PUMP_DIR_CONTAINER" | tr -d '[:space:]')"
: "${DATA_PUMP_DIR_CONTAINER:?Could not resolve DATA_PUMP_DIR path}"

echo "Running expdp for schema ${ORACLE_APP_USER}..."
docker exec "$CONTAINER_NAME" expdp "system/${ORACLE_PASSWORD}@//localhost:1521/XEPDB1" \
  schemas="${ORACLE_APP_USER}" \
  directory=DATA_PUMP_DIR \
  dumpfile="${DUMP_FILE}" \
  logfile="${LOG_FILE}"

echo "Copying dump + log out to ${BACKUP_DIR_HOST}..."
docker cp "${CONTAINER_NAME}:${DATA_PUMP_DIR_CONTAINER}/${DUMP_FILE}" "${BACKUP_DIR_HOST}/${DUMP_FILE}"
docker cp "${CONTAINER_NAME}:${DATA_PUMP_DIR_CONTAINER}/${LOG_FILE}" "${BACKUP_DIR_HOST}/${LOG_FILE}"
docker exec "$CONTAINER_NAME" rm -f "${DATA_PUMP_DIR_CONTAINER}/${DUMP_FILE}" "${DATA_PUMP_DIR_CONTAINER}/${LOG_FILE}"

MANIFEST_FILE="${BACKUP_DIR_HOST}/autotrade_${TIMESTAMP}.manifest.txt"
echo "Recording per-table row counts to ${MANIFEST_FILE}..."
docker exec -i "$CONTAINER_NAME" sqlplus -s "${ORACLE_APP_USER}/${ORACLE_APP_USER_PASSWORD}@//localhost:1521/XEPDB1" <<'SQL' > "$MANIFEST_FILE"
SET PAGESIZE 0 FEEDBACK OFF VERIFY OFF HEADING OFF ECHO OFF LINESIZE 200 SERVEROUTPUT ON
DECLARE
  v_cnt NUMBER;
BEGIN
  FOR t IN (SELECT table_name FROM user_tables ORDER BY table_name) LOOP
    EXECUTE IMMEDIATE 'SELECT COUNT(*) FROM "' || t.table_name || '"' INTO v_cnt;
    DBMS_OUTPUT.PUT_LINE(RPAD(t.table_name, 40) || v_cnt);
  END LOOP;
END;
/
EXIT
SQL

echo ""
echo "Backup complete:"
echo "  Dump:     ${BACKUP_DIR_HOST}/${DUMP_FILE}"
echo "  Log:      ${BACKUP_DIR_HOST}/${LOG_FILE}"
echo "  Manifest: ${MANIFEST_FILE}"
