-- Optional single image per product, stored in DB (one product -> at most one image).
CREATE TABLE product_image (
    id           UUID PRIMARY KEY,
    product_id   UUID NOT NULL UNIQUE REFERENCES product(id) ON DELETE CASCADE,
    file_name    VARCHAR(255),
    content_type VARCHAR(100),
    size_bytes   BIGINT,
    data         BYTEA NOT NULL,
    created_at   TIMESTAMP,
    updated_at   TIMESTAMP
);
