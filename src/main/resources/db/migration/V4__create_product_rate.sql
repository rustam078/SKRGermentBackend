CREATE TABLE product_rate
(
    id UUID PRIMARY KEY,

    product_id UUID NOT NULL,

    rate DECIMAL(12,2) NOT NULL,

    effective_from DATE NOT NULL,

    created_at TIMESTAMP NOT NULL,

    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_product_rate_product
        FOREIGN KEY(product_id)
        REFERENCES product(id)
);