-- ============================================================
-- INVESTMENT
-- ============================================================

CREATE TABLE investment
(
    id                  UUID PRIMARY KEY,

    invoice_number      VARCHAR(100) NOT NULL,

    vendor_id           UUID,

    investment_type VARCHAR(30) NOT NULL,

    purchase_date       DATE NOT NULL,

    sub_total           NUMERIC(18,2) NOT NULL DEFAULT 0,

    gst_amount          NUMERIC(18,2) NOT NULL DEFAULT 0,

    discount_amount     NUMERIC(18,2) NOT NULL DEFAULT 0,

    other_charge        NUMERIC(18,2) NOT NULL DEFAULT 0,

    grand_total         NUMERIC(18,2) NOT NULL DEFAULT 0,

    created_at          TIMESTAMP NOT NULL,

    updated_at          TIMESTAMP,

    is_deleted          BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT uk_investment_invoice_number
        UNIQUE (invoice_number),

    CONSTRAINT fk_investment_vendor
        FOREIGN KEY (vendor_id)
        REFERENCES vendor(id)
);

-- ============================================================
-- INVESTMENT ITEM
-- ============================================================

CREATE TABLE investment_item
(
    id                  UUID PRIMARY KEY,

    investment_id       UUID NOT NULL,

    item_type VARCHAR(30) NOT NULL,

    product_id          UUID,

    item_name           VARCHAR(200) NOT NULL,

    quantity            NUMERIC(18,2) NOT NULL,

    unit VARCHAR(30) NOT NULL,

    rate                NUMERIC(18,2) NOT NULL,

    total_amount        NUMERIC(18,2) NOT NULL,

    created_at          TIMESTAMP NOT NULL,

    updated_at          TIMESTAMP,

    is_deleted          BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT fk_investment_item_investment
        FOREIGN KEY (investment_id)
        REFERENCES investment(id),

    CONSTRAINT fk_investment_item_product
        FOREIGN KEY (product_id)
        REFERENCES product(id)
);

-- ============================================================
-- INDEXES
-- ============================================================

CREATE INDEX idx_investment_vendor
ON investment(vendor_id);

CREATE INDEX idx_investment_purchase_date
ON investment(purchase_date);

CREATE INDEX idx_investment_type
ON investment(investment_type);



CREATE INDEX idx_investment_item_product
ON investment_item(product_id);

CREATE INDEX idx_investment_item_type
ON investment_item(item_type);