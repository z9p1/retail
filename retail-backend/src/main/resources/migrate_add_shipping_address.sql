-- 若数据库已存在 order 表且无 shipping_address 字段，执行此句增加收货地址（可为空）
-- MySQL: ALTER TABLE `order` ADD COLUMN shipping_address VARCHAR(512) NULL AFTER status;
USE retail;
ALTER TABLE `order` ADD COLUMN shipping_address VARCHAR(512) NULL AFTER status;
