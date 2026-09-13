-- Per-garment serialized units for QR labelling & scan-to-sell.
CREATE TABLE product_unit
(
    id            UUID PRIMARY KEY,

    serial        VARCHAR(40) NOT NULL UNIQUE,
    batch_number  VARCHAR(40) NOT NULL,
    product_id    UUID        NOT NULL,

    printed_price NUMERIC(12,2) NOT NULL,

    status        VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',

    sale_order_id UUID,
    sold_at       TIMESTAMP,

    created_at    TIMESTAMP NOT NULL,
    updated_at    TIMESTAMP,

    CONSTRAINT fk_product_unit_product
        FOREIGN KEY (product_id) REFERENCES product(id),

    CONSTRAINT product_unit_status_check
        CHECK (status IN ('AVAILABLE', 'SOLD', 'VOID'))
);

CREATE INDEX idx_product_unit_batch  ON product_unit(batch_number);
CREATE INDEX idx_product_unit_status ON product_unit(status);

-- A scanned sale line records which batch it came from (typed lines stay NULL / FIFO).
ALTER TABLE sales_order_item
    ADD COLUMN IF NOT EXISTS batch_number VARCHAR(40);
