ALTER TABLE guardian
    DROP CONSTRAINT ck_guardian_profile_type_gender,
    ADD CONSTRAINT ck_guardian_profile_type_gender
        CHECK (
            (
                profile_type = 'individual'
                AND gender IS NOT NULL
                AND gender IN ('female', 'male')
            )
            OR (
                profile_type IN ('couple', 'family')
                AND (
                    gender IS NULL
                    OR gender IN ('female', 'male')
                )
            )
        );
