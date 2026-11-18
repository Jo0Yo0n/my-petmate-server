ALTER TABLE guardian_available_time_slot
    DROP CONSTRAINT ck_guardian_available_time_slot_time_order,
    DROP CONSTRAINT ck_guardian_available_time_slot_ten_minute_boundary,
    ALTER COLUMN start_time TYPE INTEGER USING (EXTRACT(EPOCH FROM start_time) / 60)::INTEGER,
    ALTER COLUMN end_time TYPE INTEGER USING (EXTRACT(EPOCH FROM end_time) / 60)::INTEGER;

ALTER TABLE guardian_available_time_slot RENAME COLUMN start_time TO start_minute;
ALTER TABLE guardian_available_time_slot RENAME COLUMN end_time TO end_minute;

ALTER TABLE guardian_available_time_slot
    ADD CONSTRAINT ck_guardian_available_time_slot_time_order
        CHECK (start_minute >= 0 AND start_minute < end_minute AND end_minute <= 1440),
    ADD CONSTRAINT ck_guardian_available_time_slot_ten_minute_boundary
        CHECK (MOD(start_minute, 10) = 0 AND MOD(end_minute, 10) = 0);
