# Rotating the broker-credential encryption key (E1-F3-S1)

`broker_credentials.api_key_ciphertext` / `api_secret_ciphertext` are AES/GCM
encrypted by `CredentialEncryptionService`, keyed by the row's
`encryption_key_version`. Rotation re-keys existing rows without downtime —
old and new keys coexist in the environment for the duration of the rotation.

## Procedure

1. Generate a new key: `openssl rand -base64 32`. Pick the next id (e.g. `v2`).
2. Add `CREDENTIAL_ENC_KEY_V2=<new value>` to the environment **alongside**
   the still-present `CREDENTIAL_ENC_KEY_V1` — do not remove `V1` yet.
3. Set `CREDENTIAL_ENC_ACTIVE_KEY_ID=v2` and restart the app. New writes now
   use `v2`; existing rows still decrypt fine via `v1` (both keys are loaded).
4. Trigger `BrokerCredentialService.rotateAll()` (e.g. via a one-off test/
   runner, or a future admin endpoint) until every row's
   `encryption_key_version` is `v2`. It's idempotent — safe to run more than
   once.
5. Verify in Oracle SQL Developer / sqlplus:
   ```sql
   SELECT DISTINCT encryption_key_version FROM broker_credentials;
   ```
   should return only `v2`.
6. Only then remove `CREDENTIAL_ENC_KEY_V1` from the environment. Restart and
   confirm the app still boots and broker adapter calls still succeed.

## Notes

- If `CREDENTIAL_ENC_ACTIVE_KEY_ID` names a key with no matching
  `CREDENTIAL_ENC_KEY_<ID>` env var, the app fails fast at startup rather than
  silently falling back.
- If more than one `CREDENTIAL_ENC_KEY_*` var is set, `CREDENTIAL_ENC_ACTIVE_KEY_ID`
  is required — the app refuses to guess which key new writes should use.
- Never remove a key id from the environment while any row still has that
  `encryption_key_version` — decrypting it becomes impossible.
