ALTER TABLE production_entry_detail
ADD COLUMN rate_snapshot NUMERIC(12,2);

ALTER TABLE production_entry_detail
ADD COLUMN amount_snapshot NUMERIC(12,2);

ALTER TABLE production_entry_detail
ADD COLUMN product_name_snapshot VARCHAR(255);