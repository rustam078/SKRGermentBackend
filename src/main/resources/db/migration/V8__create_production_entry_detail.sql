CREATE TABLE production_entry_detail
(
    id UUID PRIMARY KEY,

    production_entry_id UUID NOT NULL,

    product_id UUID NOT NULL,

    quantity INTEGER NOT NULL,

    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_production_detail_entry
        FOREIGN KEY (production_entry_id)
        REFERENCES production_entry(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_production_detail_product
        FOREIGN KEY (product_id)
        REFERENCES product(id)
);