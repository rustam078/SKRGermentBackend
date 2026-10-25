-- Optional payment reference (transaction id / UTR / cheque no) on an investment payment,
-- captured for non-cash modes on the Add Investment form.
ALTER TABLE investment_payment ADD COLUMN IF NOT EXISTS reference VARCHAR(120);
