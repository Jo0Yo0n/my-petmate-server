ALTER TABLE guardian DROP CONSTRAINT uk_guardian_email;

CREATE UNIQUE INDEX uk_guardian_email_lower ON guardian (LOWER(email));
