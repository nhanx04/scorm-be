CREATE TABLE IF NOT EXISTS organization_member (
    id BIGSERIAL PRIMARY KEY,
    orgid BIGINT NOT NULL,
    userid BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL,
    joined_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_org_member_org_user UNIQUE (orgid, userid),
    CONSTRAINT fk_org_member_org FOREIGN KEY (orgid) REFERENCES organization (orgid) ON DELETE CASCADE,
    CONSTRAINT fk_org_member_user FOREIGN KEY (userid) REFERENCES users (userid) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_org_member_orgid ON organization_member (orgid);
CREATE INDEX IF NOT EXISTS idx_org_member_userid ON organization_member (userid);

CREATE TABLE IF NOT EXISTS organization_resource (
    id BIGSERIAL PRIMARY KEY,
    orgid BIGINT NOT NULL,
    shared_by_userid BIGINT NOT NULL,
    type VARCHAR(20) NOT NULL,
    mediaid BIGINT NULL,
    courseid BIGINT NULL,
    libraryid BIGINT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_org_resource_org FOREIGN KEY (orgid) REFERENCES organization (orgid) ON DELETE CASCADE,
    CONSTRAINT fk_org_resource_shared_by FOREIGN KEY (shared_by_userid) REFERENCES users (userid) ON DELETE CASCADE,
    CONSTRAINT fk_org_resource_media FOREIGN KEY (mediaid) REFERENCES media_asset (mediaid) ON DELETE SET NULL,
    CONSTRAINT fk_org_resource_course FOREIGN KEY (courseid) REFERENCES course (courseid) ON DELETE SET NULL,
    CONSTRAINT fk_org_resource_library FOREIGN KEY (libraryid) REFERENCES my_library (libraryid) ON DELETE SET NULL,
    CONSTRAINT ck_org_resource_exactly_one_target CHECK (
        ((CASE WHEN mediaid IS NOT NULL THEN 1 ELSE 0 END) +
         (CASE WHEN courseid IS NOT NULL THEN 1 ELSE 0 END) +
         (CASE WHEN libraryid IS NOT NULL THEN 1 ELSE 0 END)) = 1
    )
);

CREATE INDEX IF NOT EXISTS idx_org_resource_orgid ON organization_resource (orgid);
CREATE INDEX IF NOT EXISTS idx_org_resource_type ON organization_resource (type);
CREATE INDEX IF NOT EXISTS idx_org_resource_created_at ON organization_resource (created_at DESC);

CREATE TABLE IF NOT EXISTS organization_activity (
    id BIGSERIAL PRIMARY KEY,
    orgid BIGINT NOT NULL,
    userid BIGINT NOT NULL,
    action VARCHAR(30) NOT NULL,
    target_id BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_org_activity_org FOREIGN KEY (orgid) REFERENCES organization (orgid) ON DELETE CASCADE,
    CONSTRAINT fk_org_activity_user FOREIGN KEY (userid) REFERENCES users (userid) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_org_activity_orgid_created_at ON organization_activity (orgid, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_org_activity_userid ON organization_activity (userid);

