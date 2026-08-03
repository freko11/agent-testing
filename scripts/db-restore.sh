#!/usr/bin/env bash
set -euo pipefail

# Imports a Data Pump dump (produced by db-backup.sh) into a target Oracle XE
# container via impdp, then prints per-table row counts to diff against the
# backup's .manifest.txt sidecar. See docs/runbooks/oracle-backup-restore.md.
#
# Usage: scripts/db-restore.sh <dump-file> [container_name]

DUMP_FILE_HOST="${1:?Usage: $0 <dump-file> [container_name]}"
CONTAINER_NAME="${2:-autotrade-oracle-xe-restore-test}"
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="${ROOT_DIR}/.env"

if [ ! -f "$DUMP_FILE_HOST" ]; then
  echo "Dump file not found: $DUMP_FILE_HOST" >&2
  exit 1
fi
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

DUMP_FILE_NAME="$(basename "$DUMP_FILE_HOST")"

echo "Looking up DATA_PUMP_DIR path in ${CONTAINER_NAME}..."
DATA_PUMP_DIR_CONTAINER="$(docker exec -i "$CONTAINER_NAME" sqlplus -s "system/${ORACLE_PASSWORD}@//localhost:1521/XEPDB1" <<'SQL'
SET PAGESIZE 0 FEEDBACK OFF VERIFY OFF HEADING OFF ECHO OFF LINESIZE 200
SELECT directory_path FROM dba_directories WHERE directory_name = 'DATA_PUMP_DIR';
EXIT
SQL
)"
DATA_PUMP_DIR_CONTAINER="$(echo "$DATA_PUMP_DIR_CONTAINER" | tr -d '[:space:]')"
: "${DATA_PUMP_DIR_CONTAINER:?Could not resolve DATA_PUMP_DIR path}"

docker cp "$DUMP_FILE_HOST" "${CONTAINER_NAME}:${DATA_PUMP_DIR_CONTAINER}/${DUMP_FILE_NAME}"

echo "Running impdp into schema ${ORACLE_APP_USER}..."
# exclude=user: the target's APP_USER already exists (created by the
# container's own init from ORACLE_APP_USER/ORACLE_APP_USER_PASSWORD in
# .env) - only the schema's objects/data need importing into it.
docker exec "$CONTAINER_NAME" impdp "system/${ORACLE_PASSWORD}@//localhost:1521/XEPDB1" \
  directory=DATA_PUMP_DIR \
  dumpfile="${DUMP_FILE_NAME}" \
  logfile="import_$(date +%Y%m%d_%H%M%S).log" \
  schemas="${ORACLE_APP_USER}" \
  exclude=user

docker exec "$CONTAINER_NAME" rm -f "${DATA_PUMP_DIR_CONTAINER}/${DUMP_FILE_NAME}"

echo ""
echo "Restored. Per-table row counts in ${CONTAINER_NAME}:"
docker exec -i "$CONTAINER_NAME" sqlplus -s "${ORACLE_APP_USER}/${ORACLE_APP_USER_PASSWORD}@//localhost:1521/XEPDB1" <<'SQL'
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
echo "Compare the counts above against the backup's .manifest.txt to confirm the restore is complete."
