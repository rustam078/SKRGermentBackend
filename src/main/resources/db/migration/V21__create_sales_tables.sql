-- ==========================================
-- V21__create_sales_tables.sql
-- ==========================================

CREATE TABLE IF NOT EXISTS sales_order
(
    id UUID PRIMARY KEY,
    invoice_no VARCHAR(50) NOT NULL UNIQUE,
    customer_name VARCHAR(255) NOT NULL,
    customer_mobile VARCHAR(20) NOT NULL,
    customer_email VARCHAR(255),
    subtotal NUMERIC(19,2) NOT NULL,
    discount NUMERIC(19,2) NOT NULL DEFAULT 0,
    tax NUMERIC(19,2) NOT NULL DEFAULT 0,
    grand_total NUMERIC(19,2) NOT NULL,
    payment_mode VARCHAR(30) NOT NULL,
    payment_provider VARCHAR(100),
    payment_status VARCHAR(30) NOT NULL,
    remarks TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS sales_order_item
(
    id UUID PRIMARY KEY,
    sales_order_id UUID NOT NULL,
    product_id UUID NOT NULL,
    quantity NUMERIC(19,2) NOT NULL,
    unit_price NUMERIC(19,2) NOT NULL,
    selling_price NUMERIC(19,2) NOT NULL,
    discount NUMERIC(19,2) NOT NULL DEFAULT 0,
    line_total NUMERIC(19,2) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_sales_item_order
        FOREIGN KEY (sales_order_id) REFERENCES sales_order(id),
    CONSTRAINT fk_sales_item_product
        FOREIGN KEY (product_id) REFERENCES product(id)
);

CREATE TABLE IF NOT EXISTS inventory_transaction
(
    id UUID PRIMARY KEY,
    sales_order_item_id UUID NOT NULL,
    product_id UUID NOT NULL,
    batch_id UUID NOT NULL,
    transaction_type VARCHAR(30) NOT NULL,
    quantity NUMERIC(19,2) NOT NULL,
    unit_cost NUMERIC(19,2) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_inventory_transaction_item
        FOREIGN KEY (sales_order_item_id) REFERENCES sales_order_item(id),
    CONSTRAINT fk_inventory_transaction_product
        FOREIGN KEY (product_id) REFERENCES product(id),
    CONSTRAINT fk_inventory_transaction_batch
        FOREIGN KEY (batch_id) REFERENCES inventory_batch(id)
);

-- Indexes (IF NOT EXISTS - PostgreSQL 9.5+)
CREATE INDEX IF NOT EXISTS idx_sales_order_invoice    ON sales_order(invoice_no);
CREATE INDEX IF NOT EXISTS idx_sales_order_mobile     ON sales_order(customer_mobile);
CREATE INDEX IF NOT EXISTS idx_sales_item_order       ON sales_order_item(sales_order_id);
CREATE INDEX IF NOT EXISTS idx_sales_item_product     ON sales_order_item(product_id);
CREATE INDEX IF NOT EXISTS idx_inventory_tx_item      ON inventory_transaction(sales_order_item_id);
CREATE INDEX IF NOT EXISTS idx_inventory_tx_product   ON inventory_transaction(product_id);
CREATE INDEX IF NOT EXISTS idx_inventory_tx_batch     ON inventory_transaction(batch_id);