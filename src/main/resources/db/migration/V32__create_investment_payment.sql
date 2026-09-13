-- Payments made against investment invoices (partial payments allowed).
CREATE TABLE investment_payment
(
    id             UUID PRIMARY KEY,
    investment_id  UUID NOT NULL,
    payment_date   DATE NOT NULL,
    mode           VARCHAR(20) NOT NULL,
    amount         NUMERIC(18,2) NOT NULL,
    created_at     TIMESTAMP NOT NULL,
    updated_at     TIMESTAMP,

    CONSTRAINT fk_investment_payment_investment
        FOREIGN KEY (investment_id) REFERENCES investment(id) ON DELETE CASCADE
);

CREATE INDEX idx_investment_payment_investment ON investment_payment(investment_id);
