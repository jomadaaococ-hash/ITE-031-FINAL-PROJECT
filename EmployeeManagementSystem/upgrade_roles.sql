-- Run this ONLY if you want to keep your existing data (instead of re-importing the full database).
USE sugbomacciato_db;
ALTER TABLE users ADD COLUMN role VARCHAR(10) NOT NULL DEFAULT 'ADMIN';
ALTER TABLE users ADD COLUMN emp_id INT NULL;
ALTER TABLE users ADD CONSTRAINT fk_user_emp FOREIGN KEY (emp_id) REFERENCES employees(id) ON DELETE CASCADE;
