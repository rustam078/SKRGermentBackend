UPDATE production_entry_detail d
SET product_name_snapshot = p.name
FROM product p
WHERE d.product_id = p.id;

UPDATE production_entry_detail d
SET rate_snapshot = 0,
    amount_snapshot = 0
WHERE rate_snapshot IS NULL;