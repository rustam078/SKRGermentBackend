-- Allow the new SOLD status (the column had a CHECK limited to ACTIVE/CLOSED).
ALTER TABLE inventory_batch DROP CONSTRAINT IF EXISTS inventory_batch_status_check;
ALTER TABLE inventory_batch ADD CONSTRAINT inventory_batch_status_check
    CHECK (status IN ('ACTIVE', 'SOLD', 'CLOSED'));

-- Batches that are fully consumed should read SOLD, not ACTIVE.
-- Backfill existing depleted batches (sales previously left them ACTIVE).
UPDATE inventory_batch
SET status = 'SOLD'
WHERE quantity_available = 0
  AND status = 'ACTIVE';
