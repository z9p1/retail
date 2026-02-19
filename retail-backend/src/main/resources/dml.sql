-- ============================================================
-- 店家线上零售系统 DML（先删后增，可重复执行）
-- 执行前请先执行 ddl.sql；客户密码 123456，店家 store/admin123
-- ============================================================
USE retail;

-- ----------------------------------------
-- 先删：按子表 -> 父表顺序清空
-- ----------------------------------------
DELETE FROM order_item;
DELETE FROM `order`;
DELETE FROM access_log;
DELETE FROM product;
DELETE FROM sys_user;

-- ----------------------------------------
-- 后增：商品 -> 用户 -> 订单 -> 订单明细
-- ----------------------------------------
-- 1. 5 个商品 (id 1~5)
-- ----------------------------------------
INSERT INTO product (id, name, price, stock, status, description, version, create_time, update_time) VALUES
(1, 'Organic Apple', 12.80, 100, 'ON_SALE', 'Fresh organic apple 500g/bag', 0, NOW(), NOW()),
(2, 'Pure Milk', 8.50, 200, 'ON_SALE', 'Pure milk 250ml*12', 0, NOW(), NOW()),
(3, 'Rice', 45.00, 80, 'ON_SALE', 'Rice 5kg/bag', 0, NOW(), NOW()),
(4, 'Mineral Water', 2.00, 500, 'ON_SALE', 'Drinking water 550ml*24', 0, NOW(), NOW()),
(5, 'Tissue', 18.00, 150, 'ON_SALE', '3-ply tissue 130 sheets*18 packs', 0, NOW(), NOW());

-- ----------------------------------------
-- 2. 5 customers (USER, password 123456)
-- MD5(raw + 'retail_salt') same as AuthService
-- ----------------------------------------
SET @pwd = MD5(CONCAT('123456', 'retail_salt'));

INSERT INTO sys_user (id, username, password, role, nickname, phone, create_time, update_time) VALUES
(1, 'customer1', @pwd, 'USER', 'Zhang San', '13800001001', NOW(), NOW()),
(2, 'customer2', @pwd, 'USER', 'Li Si', '13800001002', NOW(), NOW()),
(3, 'customer3', @pwd, 'USER', 'Wang Wu', '13800001003', NOW(), NOW()),
(4, 'customer4', @pwd, 'USER', 'Zhao Liu', '13800001004', NOW(), NOW()),
(5, 'customer5', @pwd, 'USER', 'Qian Qi', '13800001005', NOW(), NOW());

-- ----------------------------------------
-- 2.1 店家账号（STORE, 密码 admin123）
-- ----------------------------------------
SET @store_pwd = MD5(CONCAT('admin123', 'retail_salt'));
INSERT INTO sys_user (id, username, password, role, nickname, phone, create_time, update_time) VALUES
(6, 'store', @store_pwd, 'STORE', 'store', '13900000000', NOW(), NOW());

-- ----------------------------------------
-- 3. 10 orders
-- ----------------------------------------
INSERT INTO `order` (id, order_no, user_id, total_amount, status, create_time, pay_time, update_time) VALUES
(1,  'O202502170001', 1, 20.80, 'COMPLETED', DATE_SUB(NOW(), INTERVAL 5 DAY),  DATE_SUB(NOW(), INTERVAL 5 DAY), NOW()),
(2,  'O202502170002', 1, 45.00, 'COMPLETED', DATE_SUB(NOW(), INTERVAL 4 DAY),  DATE_SUB(NOW(), INTERVAL 4 DAY), NOW()),
(3,  'O202502170003', 2, 27.00, 'SHIPPED',   DATE_SUB(NOW(), INTERVAL 3 DAY),  DATE_SUB(NOW(), INTERVAL 3 DAY), NOW()),
(4,  'O202502170004', 2, 18.00, 'PENDING_SHIP', DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), NOW()),
(5,  'O202502170005', 3, 70.60, 'COMPLETED', DATE_SUB(NOW(), INTERVAL 4 DAY),  DATE_SUB(NOW(), INTERVAL 4 DAY), NOW()),
(6,  'O202502170006', 3, 10.00, 'CANCELLED',  DATE_SUB(NOW(), INTERVAL 2 DAY),  NULL, NOW()),
(7,  'O202502170007', 4, 8.50,  'COMPLETED', DATE_SUB(NOW(), INTERVAL 6 DAY),  DATE_SUB(NOW(), INTERVAL 6 DAY), NOW()),
(8,  'O202502170008', 4, 63.00, 'PENDING_PAY', DATE_SUB(NOW(), INTERVAL 1 HOUR), NULL, NOW()),
(9,  'O202502170009', 5, 30.80, 'COMPLETED', DATE_SUB(NOW(), INTERVAL 3 DAY),  DATE_SUB(NOW(), INTERVAL 3 DAY), NOW()),
(10, 'O202502170010', 5, 4.00,  'PENDING_SHIP', DATE_SUB(NOW(), INTERVAL 12 HOUR), DATE_SUB(NOW(), INTERVAL 11 HOUR), NOW());

-- ----------------------------------------
-- 4. Order items
-- ----------------------------------------
INSERT INTO order_item (order_id, product_id, product_name, quantity, price, subtotal) VALUES
(1, 1, 'Organic Apple', 1, 12.80, 12.80),
(1, 4, 'Mineral Water', 4, 2.00, 8.00);

INSERT INTO order_item (order_id, product_id, product_name, quantity, price, subtotal) VALUES
(2, 3, 'Rice', 1, 45.00, 45.00);

INSERT INTO order_item (order_id, product_id, product_name, quantity, price, subtotal) VALUES
(3, 2, 'Pure Milk', 2, 8.50, 17.00),
(3, 4, 'Mineral Water', 5, 2.00, 10.00);

INSERT INTO order_item (order_id, product_id, product_name, quantity, price, subtotal) VALUES
(4, 5, 'Tissue', 1, 18.00, 18.00);

INSERT INTO order_item (order_id, product_id, product_name, quantity, price, subtotal) VALUES
(5, 1, 'Organic Apple', 2, 12.80, 25.60),
(5, 3, 'Rice', 1, 45.00, 45.00);

INSERT INTO order_item (order_id, product_id, product_name, quantity, price, subtotal) VALUES
(6, 4, 'Mineral Water', 5, 2.00, 10.00);

INSERT INTO order_item (order_id, product_id, product_name, quantity, price, subtotal) VALUES
(7, 2, 'Pure Milk', 1, 8.50, 8.50);

INSERT INTO order_item (order_id, product_id, product_name, quantity, price, subtotal) VALUES
(8, 3, 'Rice', 1, 45.00, 45.00),
(8, 5, 'Tissue', 1, 18.00, 18.00);

INSERT INTO order_item (order_id, product_id, product_name, quantity, price, subtotal) VALUES
(9, 1, 'Organic Apple', 1, 12.80, 12.80),
(9, 5, 'Tissue', 1, 18.00, 18.00);

INSERT INTO order_item (order_id, product_id, product_name, quantity, price, subtotal) VALUES
(10, 4, 'Mineral Water', 2, 2.00, 4.00);

-- ----------------------------------------
-- Notes: customer1~5 pwd 123456; 店家账号 store / admin123
-- ----------------------------------------
