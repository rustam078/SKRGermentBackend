-- Single pricing-history table now carries BOTH the production/material cost
-- and the selling price per effective-from date (profit % is derived from the two).
ALTER TABLE product_material_cost
    ADD COLUMN IF NOT EXISTS sale_price NUMERIC(18,2);

-- Remove the short-lived separate sale-price table (superseded by the column above).
DROP TABLE IF EXISTS product_sale_price CASCADE;
