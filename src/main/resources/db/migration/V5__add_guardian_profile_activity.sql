ALTER TABLE guardian
    ADD COLUMN nickname VARCHAR(20) NOT NULL,
    ADD CONSTRAINT ck_guardian_nickname_not_blank CHECK (nickname ~ '[^[:space:]]'),
    ADD CONSTRAINT uk_guardian_nickname UNIQUE (nickname);

CREATE TABLE region
(
    id        UUID PRIMARY KEY,
    name      VARCHAR(100) NOT NULL,
    level     VARCHAR(16)  NOT NULL,
    parent_id UUID,
    CONSTRAINT ck_region_level CHECK (level IN ('province', 'district', 'locality')),
    CONSTRAINT ck_region_parent
        CHECK (
            (level = 'province' AND parent_id IS NULL)
                OR (level IN ('district', 'locality') AND parent_id IS NOT NULL AND parent_id <> id)
            ),
    CONSTRAINT fk_region_parent FOREIGN KEY (parent_id) REFERENCES region (id)
);

CREATE INDEX idx_region_parent_id ON region (parent_id);

CREATE TABLE guardian_activity_region
(
    id          UUID PRIMARY KEY,
    guardian_id UUID NOT NULL,
    region_id   UUID NOT NULL,
    CONSTRAINT uk_guardian_activity_region_guardian_region UNIQUE (guardian_id, region_id),
    CONSTRAINT fk_guardian_activity_region_guardian
        FOREIGN KEY (guardian_id) REFERENCES guardian (id) ON DELETE CASCADE,
    CONSTRAINT fk_guardian_activity_region_region
        FOREIGN KEY (region_id) REFERENCES region (id)
);

CREATE TABLE guardian_available_time_slot
(
    id          UUID PRIMARY KEY,
    guardian_id UUID                   NOT NULL,
    day_of_week VARCHAR(3)             NOT NULL,
    start_time  TIME WITHOUT TIME ZONE NOT NULL,
    end_time    TIME WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT fk_guardian_available_time_slot_guardian
        FOREIGN KEY (guardian_id) REFERENCES guardian (id) ON DELETE CASCADE,
    CONSTRAINT ck_guardian_available_time_slot_day_of_week
        CHECK (day_of_week IN ('mon', 'tue', 'wed', 'thu', 'fri', 'sat', 'sun')),
    CONSTRAINT ck_guardian_available_time_slot_time_order
        CHECK (start_time < end_time AND end_time < TIME '24:00'),
    CONSTRAINT ck_guardian_available_time_slot_ten_minute_boundary
        CHECK (
            MOD(EXTRACT(EPOCH FROM start_time), 600) = 0
                AND MOD(EXTRACT(EPOCH FROM end_time), 600) = 0
            ),
    CONSTRAINT uk_guardian_available_time_slot
        UNIQUE (guardian_id, day_of_week, start_time, end_time)
);
