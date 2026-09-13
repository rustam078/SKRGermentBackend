CREATE TABLE product_material_cost
(
    id              UUID PRIMARY KEY,

    product_id      UUID NOT NULL,

    cost            NUMERIC(18,2) NOT NULL,

    effective_from  DATE NOT NULL,

    remarks         TEXT,

    created_at      TIMESTAMP NOT NULL,

    updated_at      TIMESTAMP,

    CONSTRAINT fk_product_material_cost_product
        FOREIGN KEY (product_id)
        REFERENCES product(id),

    CONSTRAINT uk_product_material_cost
        UNIQUE(product_id, effective_from)
);

CREATE INDEX idx_product_material_cost_product
ON product_material_cost(product_id);

CREATE INDEX idx_product_material_cost_effective
ON product_material_cost(effective_from);