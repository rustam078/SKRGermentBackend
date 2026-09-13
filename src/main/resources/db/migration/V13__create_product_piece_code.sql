CREATE TABLE product_piece_code
(
    id UUID PRIMARY KEY,

    product_id UUID NOT NULL,

    code VARCHAR(100) NOT NULL UNIQUE,

    description VARCHAR(255),

    rate NUMERIC(19,2) NOT NULL,

    active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMP NOT NULL,

    updated_at TIMESTAMP,

    created_by VARCHAR(100),

    updated_by VARCHAR(100),

    CONSTRAINT fk_piece_code_product
        FOREIGN KEY (product_id)
            REFERENCES product(id)
);