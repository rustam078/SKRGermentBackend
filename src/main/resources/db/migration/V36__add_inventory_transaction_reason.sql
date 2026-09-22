-- Audit note on inventory transactions (used by RECONCILIATION adjustments from QR sales).
ALTER TABLE inventory_transaction ADD COLUMN IF NOT EXISTS reason VARCHAR(200);
