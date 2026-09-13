CREATE TABLE production_entry
(
    id UUID PRIMARY KEY,

    employee_id UUID NOT NULL,

    production_date DATE NOT NULL,

    remarks VARCHAR(500),

    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_production_employee
        FOREIGN KEY (employee_id)
        REFERENCES employee(id)
);