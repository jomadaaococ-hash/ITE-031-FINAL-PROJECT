-- NOTE: this recreates the database from scratch (old sample data is replaced).
DROP DATABASE IF EXISTS sugbomacciato_db;
CREATE DATABASE sugbomacciato_db;
USE sugbomacciato_db;

CREATE TABLE employees (
  id INT AUTO_INCREMENT PRIMARY KEY,
  full_name VARCHAR(100) NOT NULL,
  contact VARCHAR(30),
  address VARCHAR(150),
  department VARCHAR(40) NOT NULL,
  position VARCHAR(60) NOT NULL,
  employment VARCHAR(20) NOT NULL DEFAULT 'Regular',
  date_hired DATE NOT NULL,
  salary_type VARCHAR(10) NOT NULL DEFAULT 'Monthly',
  basic_salary DECIMAL(10,2) NOT NULL
);

CREATE TABLE users (
  id INT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(50) NOT NULL UNIQUE,
  password CHAR(64) NOT NULL,
  full_name VARCHAR(100) NOT NULL,
  role VARCHAR(10) NOT NULL DEFAULT 'ADMIN',      -- ADMIN or EMPLOYEE
  emp_id INT NULL,                                -- linked employee (for EMPLOYEE accounts)
  FOREIGN KEY (emp_id) REFERENCES employees(id) ON DELETE CASCADE
);
INSERT INTO users (username, password, full_name, role)
VALUES ('admin', SHA2('admin123',256), 'Administrator', 'ADMIN');

CREATE TABLE attendance (
  id INT AUTO_INCREMENT PRIMARY KEY,
  emp_id INT NOT NULL,
  att_date DATE NOT NULL,
  time_in TIME NULL,
  time_out TIME NULL,
  status VARCHAR(10) NOT NULL,
  UNIQUE KEY uq_emp_date (emp_id, att_date),
  FOREIGN KEY (emp_id) REFERENCES employees(id) ON DELETE CASCADE
);

CREATE TABLE payroll (
  id INT AUTO_INCREMENT PRIMARY KEY,
  emp_id INT NOT NULL,
  period_start DATE NOT NULL,
  period_end DATE NOT NULL,
  days_worked DECIMAL(5,2) NOT NULL,
  ot_hours DECIMAL(5,2) NOT NULL DEFAULT 0,
  basic_pay DECIMAL(10,2) NOT NULL,
  overtime_pay DECIMAL(10,2) NOT NULL,
  allowances DECIMAL(10,2) NOT NULL DEFAULT 0,
  gross_pay DECIMAL(10,2) NOT NULL,
  sss DECIMAL(10,2) NOT NULL,
  philhealth DECIMAL(10,2) NOT NULL,
  pagibig DECIMAL(10,2) NOT NULL,
  tax DECIMAL(10,2) NOT NULL,
  other_deduction DECIMAL(10,2) NOT NULL DEFAULT 0,
  total_deductions DECIMAL(10,2) NOT NULL,
  net_pay DECIMAL(10,2) NOT NULL,
  payment_status VARCHAR(10) NOT NULL DEFAULT 'UNPAID',
  payment_date DATE NULL,
  date_generated TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (emp_id) REFERENCES employees(id) ON DELETE CASCADE
);

INSERT INTO employees (full_name, contact, address, department, position, employment, date_hired, salary_type, basic_salary) VALUES
('Maria Santos','0917-111-2233','Mabolo, Cebu City','Barista','Head Barista','Regular','2024-02-01','Monthly',18000),
('Juan Dela Cruz','0918-222-3344','Lahug, Cebu City','Cashier','Cashier','Regular','2024-05-15','Daily',540),
('Ana Reyes','0920-333-4455','Talisay City, Cebu','Kitchen','Pastry Cook','Probationary','2025-01-10','Daily',580),
('Carlo Mendoza','0921-444-5566','Banilad, Cebu City','Management','Store Manager','Regular','2023-09-01','Monthly',30000);

INSERT INTO attendance (emp_id, att_date, time_in, time_out, status) VALUES
(1, CURDATE(), '08:00', '17:00', 'Present'),
(2, CURDATE(), '08:15', '17:00', 'Late');

-- Demo employee account (view-salary-only): maria / maria123
INSERT INTO users (username, password, full_name, role, emp_id)
VALUES ('maria', SHA2('maria123',256), 'Maria Santos', 'EMPLOYEE', 1);

INSERT INTO payroll (emp_id, period_start, period_end, days_worked, ot_hours, basic_pay, overtime_pay, allowances,
  gross_pay, sss, philhealth, pagibig, tax, other_deduction, total_deductions, net_pay, payment_status, payment_date)
VALUES (1, '2026-10-01', '2026-10-15', 13, 0, 9000, 0, 500, 9500, 427.50, 237.50, 100, 0, 0, 765, 8735, 'PAID', '2026-10-15');
