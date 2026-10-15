INSERT INTO employees (employee_id, full_name, email, phone, department, designation, date_of_joining, salary, status)
VALUES
('EMP-101', 'Ananya Rao', 'ananya.rao@example.com', '+91 98765 43210', 'Engineering', 'Backend Developer', '2025-08-12', 650000, 'ACTIVE'),
('EMP-102', 'Arjun Mehta', 'arjun.mehta@example.com', '+91 98765 43211', 'Design', 'Product Designer', '2025-06-23', 620000, 'ACTIVE'),
('EMP-103', 'Diya Sharma', 'diya.sharma@example.com', '+91 98765 43212', 'People', 'HR Executive', '2024-11-04', 480000, 'ACTIVE'),
('EMP-104', 'Ishaan Patel', 'ishaan.patel@example.com', '+91 98765 43213', 'Engineering', 'Frontend Developer', '2025-02-17', 600000, 'ACTIVE'),
('EMP-105', 'Kavya Nair', 'kavya.nair@example.com', '+91 98765 43214', 'Marketing', 'Content Writer', '2024-09-09', 450000, 'INACTIVE'),
('EMP-106', 'Neel Shah', 'neel.shah@example.com', '+91 98765 43215', 'Engineering', 'QA Engineer', '2025-05-06', 550000, 'ACTIVE'),
('EMP-107', 'Nisha Jain', 'nisha.jain@example.com', '+91 98765 43216', 'Product', 'Product Analyst', '2025-01-20', 580000, 'ACTIVE'),
('EMP-108', 'Rohan Das', 'rohan.das@example.com', '+91 98765 43217', 'Sales', 'Sales Executive', '2024-07-15', 420000, 'INACTIVE'),
('EMP-109', 'Sana Khan', 'sana.khan@example.com', '+91 98765 43218', 'Design', 'UI Designer', '2025-03-10', 590000, 'ACTIVE'),
('EMP-110', 'Tanvi Joshi', 'tanvi.joshi@example.com', '+91 98765 43219', 'Finance', 'Accounts Executive', '2024-12-02', 500000, 'ACTIVE'),
('EMP-111', 'Varun Singh', 'varun.singh@example.com', '+91 98765 43220', 'Engineering', 'DevOps Engineer', '2025-04-14', 700000, 'ACTIVE'),
('EMP-112', 'Zoya Ali', 'zoya.ali@example.com', '+91 98765 43221', 'Marketing', 'SEO Executive', '2025-07-07', 460000, 'INACTIVE')
ON CONFLICT DO NOTHING;
