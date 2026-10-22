ALTER TABLE guardian
    DROP CONSTRAINT ck_guardian_identity_visibility;

ALTER TABLE guardian
    RENAME COLUMN identity_visibility TO discovery_status;

ALTER TABLE guardian
    ALTER COLUMN discovery_status TYPE VARCHAR(16);

UPDATE guardian
SET discovery_status = CASE discovery_status
    WHEN 'public' THEN 'searching'
    WHEN 'private' THEN 'paused'
END;

ALTER TABLE guardian
    ADD CONSTRAINT ck_guardian_discovery_status
        CHECK (discovery_status IN ('searching', 'paused'));
