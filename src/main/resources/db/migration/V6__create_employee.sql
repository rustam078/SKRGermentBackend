CREATE TABLE employee
(
    id UUID PRIMARY KEY,

    employee_code VARCHAR(50) NOT NULL UNIQUE,

    full_name VARCHAR(200) NOT NULL,

    mobile_number VARCHAR(20),

    address TEXT,

    joining_date DATE NOT NULL,

    active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_employee_name
ON employee(full_name);

CREATE INDEX idx_employee_code
ON employee(employee_code);