ALTER TABLE product_rate
ADD CONSTRAINT uk_product_rate_effective_date
UNIQUE(product_id, effective_from);