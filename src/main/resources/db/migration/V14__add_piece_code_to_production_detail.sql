ALTER TABLE production_entry_detail
ADD COLUMN piece_code_id UUID;

ALTER TABLE production_entry_detail
ADD COLUMN piece_code_snapshot VARCHAR(100);

ALTER TABLE production_entry_detail
ADD CONSTRAINT fk_production_piece_code
FOREIGN KEY (piece_code_id)
REFERENCES product_piece_code(id);