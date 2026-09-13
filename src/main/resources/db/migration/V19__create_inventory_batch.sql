CREATE TYPE inventory_batch_status AS ENUM (
    'ACTIVE',
    'CLOSED'
);

CREATE TABLE inventory_batch
(
    id                  UUID PRIMARY KEY,

    batch_number        VARCHAR(50) NOT NULL UNIQUE,

    product_id          UUID NOT NULL,

    source              VARCHAR(20) NOT NULL,

    source_id           UUID NOT NULL,

    received_date       DATE NOT NULL,

    quantity_received   NUMERIC(12,2) NOT NULL,

    quantity_available  NUMERIC(12,2) NOT NULL,

    unit_cost           NUMERIC(12,2) NOT NULL,

    total_cost          NUMERIC(12,2) NOT NULL,

    status              inventory_batch_status NOT NULL,

    remarks             VARCHAR(500),

    created_at          TIMESTAMP,

    updated_at          TIMESTAMP,

    CONSTRAINT fk_inventory_batch_product
        FOREIGN KEY (product_id)
            REFERENCES product(id)
);