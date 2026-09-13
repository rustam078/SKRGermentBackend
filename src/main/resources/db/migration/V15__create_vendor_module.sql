CREATE TABLE vendor
(
    id UUID PRIMARY KEY,

    name VARCHAR(150) NOT NULL,

    contact_name VARCHAR(150),

    mobile VARCHAR(20) NOT NULL,

    email VARCHAR(150),

    gst_number VARCHAR(30),

    address TEXT,

    active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMP NOT NULL,

    updated_at TIMESTAMP,

    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE UNIQUE INDEX uk_vendor_name
    ON vendor (LOWER(name))
    WHERE is_deleted = FALSE;

CREATE UNIQUE INDEX uk_vendor_mobile
    ON vendor (mobile)
    WHERE is_deleted = FALSE;

CREATE UNIQUE INDEX uk_vendor_email
    ON vendor (email)
    WHERE email IS NOT NULL
      AND is_deleted = FALSE;

CREATE UNIQUE INDEX uk_vendor_gst
    ON vendor (gst_number)
    WHERE gst_number IS NOT NULL
      AND is_deleted = FALSE;

CREATE INDEX idx_vendor_active
    ON vendor (active);

CREATE INDEX idx_vendor_created_at
    ON vendor (created_at);