ALTER TABLE inventory_batch
ALTER COLUMN status TYPE VARCHAR(20)
USING status::text;

DROP TYPE inventory_batch_status;