CREATE TABLE product_sale_price
(
    id              UUID PRIMARY KEY,

    product_id      UUID NOT NULL,

    price           NUMERIC(18,2) NOT NULL,

    effective_from  DATE NOT NULL,

    remarks         TEXT,

    created_at      TIMESTAMP NOT NULL,

    updated_at      TIMESTAMP,

    CONSTRAINT fk_product_sale_price_product
        FOREIGN KEY (product_id)
        REFERENCES product(id),

    CONSTRAINT uk_product_sale_price
        UNIQUE(product_id, effective_from)
);

CREATE INDEX idx_product_sale_price_product
ON product_sale_price(product_id);

CREATE INDEX idx_product_sale_price_effective
ON product_sale_price(effective_from);
