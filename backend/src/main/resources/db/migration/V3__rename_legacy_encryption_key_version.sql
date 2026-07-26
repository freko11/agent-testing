-- E1-F3-S1: align the existing encryption_key_version default with the
-- CREDENTIAL_ENC_KEY_<ID> env-var naming convention used by key rotation
-- (CredentialEncryptionService). No real broker credentials exist yet, so
-- renaming the default id is a safe no-op for any already-inserted rows too.

-- No NOT NULL clause here on purpose: the column is already NOT NULL from
-- V1, and real Oracle rejects re-declaring an existing NOT NULL (ORA-01442)
-- even though H2's Oracle-compatibility mode silently accepts it.
ALTER TABLE broker_credentials MODIFY (encryption_key_version VARCHAR2(20) DEFAULT 'v1');
UPDATE broker_credentials SET encryption_key_version = 'v1' WHERE encryption_key_version = 'v1-basic';
