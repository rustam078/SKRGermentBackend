ALTER TABLE employee
ADD COLUMN email VARCHAR(255);

ALTER TABLE employee
ADD CONSTRAINT uk_employee_email
UNIQUE (email);
