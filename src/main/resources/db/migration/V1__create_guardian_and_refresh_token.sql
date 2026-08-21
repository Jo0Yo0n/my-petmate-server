CREATE TABLE guardian (
    id UUID PRIMARY KEY,
    email VARCHAR(254) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    profile_type VARCHAR(16) NOT NULL,
    gender VARCHAR(8),
    identity_visibility VARCHAR(8) NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'active',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_guardian_email UNIQUE (email),
    CONSTRAINT ck_guardian_profile_type
        CHECK (profile_type IN ('individual', 'couple', 'family')),
    CONSTRAINT ck_guardian_profile_type_gender
        CHECK (
            (
                profile_type = 'individual'
                AND gender IS NOT NULL
                AND gender IN ('female', 'male')
            )
            OR (profile_type IN ('couple', 'family') AND gender IS NULL)
        ),
    CONSTRAINT ck_guardian_identity_visibility
        CHECK (identity_visibility IN ('public', 'private')),
    CONSTRAINT ck_guardian_status
        CHECK (status IN ('active', 'temporarily_restricted', 'withdrawn'))
);

CREATE TABLE refresh_token (
    id UUID PRIMARY KEY,
    guardian_id UUID NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_refresh_token_token_hash UNIQUE (token_hash),
    CONSTRAINT ck_refresh_token_token_hash
        CHECK (token_hash ~ '^[0-9a-f]{64}$'),
    CONSTRAINT fk_refresh_token_guardian
        FOREIGN KEY (guardian_id) REFERENCES guardian (id) ON DELETE CASCADE,
    CONSTRAINT ck_refresh_token_expires_after_creation
        CHECK (expires_at > created_at),
    CONSTRAINT ck_refresh_token_revoked_after_creation
        CHECK (revoked_at IS NULL OR revoked_at >= created_at)
);

CREATE INDEX idx_refresh_token_guardian_id ON refresh_token (guardian_id);
