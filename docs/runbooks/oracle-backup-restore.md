# Backing up and restoring the Oracle instance (E7-F3-S1)

The `autotrade` app schema (orders, the `order_audit_entries` decision trail,
`signal_call_entries`, `broker_credentials`, watchlist, etc.) lives entirely
in the Oracle XE container's `./oracle-data` volume. `scripts/db-backup.sh`
takes a portable, schema-scoped Data Pump (`expdp`) export so that data isn't
a single-disk-failure away from gone; `scripts/db-restore.sh` proves it can
actually come back by importing (`impdp`) into a target instance.

Data Pump was chosen over a raw volume-level copy (stopping the container and
tarring `./oracle-data`) because it's portable across containers/instances
rather than tied to one exact datafile layout, can run online against the
live dev database, and is verifiable at the row level — a volume copy only
proves the bytes moved, not that the data imports cleanly into a fresh
instance, which is what this story's acceptance criteria actually asks for.

## Backup procedure

1. Make sure the dev Oracle container is up: `docker compose up -d`, then
   `docker compose ps` (or `docker inspect -f '{{.State.Health.Status}}'
   autotrade-oracle-xe`) until it reports `healthy` — first boot takes
   ~60-90s.
2. Run `scripts/db-backup.sh` (defaults to the `autotrade-oracle-xe`
   container; pass a different container name as `$1` if needed). It:
   - looks up the path behind Oracle's own pre-existing `DATA_PUMP_DIR`
     directory object (under `/opt/oracle/admin/XE/dpdump/...` — created by
     the image itself, not the bind-mounted `/opt/oracle/oradata`, so dump
     files never mix into the live datafile volume),
   - runs `expdp` scoped to just the `autotrade` schema (`ORACLE_APP_USER`
     from `.env`) into that directory,
   - `docker cp`s the resulting `.dmp` and `.log` out to `./backups/`
     (override the destination with a `BACKUP_DIR` env var),
   - records a `.manifest.txt` sidecar: one row-count line per table in the
     schema, so a later restore has something concrete to diff against,
   - cleans up the in-container tmp copy.
3. `./backups/` is gitignored (same treatment as `oracle-data/`) — periodically
   copy its contents off this disk (external drive, cloud-synced folder,
   network share) so a backup actually survives the disk-failure scenario
   this story is about. This script does not do that copy itself.

## Restore-test procedure

Restoring is tested against a genuinely fresh, disposable second Oracle XE
instance — never against the live dev database — using
`docker-compose.restore-test.yml`, a separate compose file with its own
container name, host port, and data volume (`./restore-test-data`, also
gitignored).

1. `docker compose -f docker-compose.restore-test.yml up -d`, then wait for
   `autotrade-oracle-xe-restore-test` to report `healthy` the same way as
   step 1 above.
2. `scripts/db-restore.sh <path-to-dump> [container_name]` (container name
   defaults to `autotrade-oracle-xe-restore-test`). It looks up that
   instance's own `DATA_PUMP_DIR` path, copies the dump into it, runs
   `impdp` scoped to the `autotrade` schema, and prints the same per-table
   row-count query the backup's manifest used.
3. Diff the printed counts against the backup's `.manifest.txt`. Matching
   counts across every table is the restore's pass/fail signal.
4. Tear the disposable instance down so it doesn't linger as a second
   permanent Oracle instance on the machine:
   `docker compose -f docker-compose.restore-test.yml down -v`.

## Notes

- Both scripts connect as `system` (password from `ORACLE_PASSWORD` in
  `.env`) to look up `DATA_PUMP_DIR` and run `expdp`/`impdp` — `system` has
  DBA privileges in the CDB and its PDB (`XEPDB1`). A hand-created Data Pump
  directory object under `/tmp` was tried first and rejected: `expdp`
  reliably failed with `ORA-39070`/`ORA-29283` opening its log file there
  even though the path existed and was writable, so both scripts use
  Oracle's own pre-existing `DATA_PUMP_DIR` instead, which works out of the
  box. Per-table row counts are queried as the `autotrade` app user itself,
  so the comparison reflects exactly what the app schema owns.
- `docker-compose.restore-test.yml` intentionally reuses this repo's `.env`
  values for `ORACLE_PASSWORD`/`ORACLE_APP_USER`/`ORACLE_APP_USER_PASSWORD` —
  the restore target needs the same app username so `impdp`'s schema-scoped
  import lands in a matching (empty) schema rather than needing a
  `remap_schema`.
- If `impdp` reports `ORA-31684: Object type ... already exists`, the target
  schema wasn't actually empty (e.g. `restore-test-data/` had leftover state
  from a prior run that wasn't torn down with `-v`) — remove
  `restore-test-data/` and bring the restore-test instance up fresh.
- This is an on-demand procedure, not scheduled — no cron/Task Scheduler
  wiring is included. Re-run `scripts/db-backup.sh` manually whenever you
  want a fresh snapshot (e.g. before a schema migration or a risky change).
- Out of scope: whole-CDB/PDB backup (only the `autotrade` schema is backed
  up — that's the entirety of this app's data), and automated/scheduled
  backups.
