-- ============================================================
-- 店家线上零售系统 DDL（MySQL 8.x）先删后增
-- ============================================================
CREATE DATABASE IF NOT EXISTS retail DEFAULT CHARACTER SET utf8mb4;
USE retail;

-- ----------------------------------------
-- 先删：按子表 -> 父表顺序 DROP
-- ----------------------------------------
DROP TABLE IF EXISTS order_item;
DROP TABLE IF EXISTS `order`;
DROP TABLE IF EXISTS cart_item;
DROP TABLE IF EXISTS user_address;
DROP TABLE IF EXISTS access_log;
DROP TABLE IF EXISTS product;
DROP TABLE IF EXISTS sys_user;

-- ----------------------------------------
-- 后增：建表
-- ----------------------------------------
-- 用户（店家/用户）
CREATE TABLE sys_user (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(64) NOT NULL UNIQUE,
  password VARCHAR(64) NOT NULL,
  role VARCHAR(16) NOT NULL DEFAULT 'USER',
  nickname VARCHAR(64),
  phone VARCHAR(32),
  create_time DATETIME,
  update_time DATETIME
);

-- 商品（含 version 乐观锁）
CREATE TABLE product (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(128) NOT NULL,
  price DECIMAL(12,2) NOT NULL,
  stock INT NOT NULL DEFAULT 0,
  status VARCHAR(16) NOT NULL DEFAULT 'ON_SALE',
  description VARCHAR(512),
  image_url VARCHAR(256),
  version INT NOT NULL DEFAULT 0,
  create_time DATETIME,
  update_time DATETIME
);

-- 订单
CREATE TABLE `order` (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_no VARCHAR(32) NOT NULL,
  user_id BIGINT NOT NULL,
  total_amount DECIMAL(12,2) NOT NULL,
  status VARCHAR(24) NOT NULL,
  shipping_address VARCHAR(512),
  create_time DATETIME,
  pay_time DATETIME,
  update_time DATETIME
);

-- 订单明细
CREATE TABLE order_item (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  product_name VARCHAR(128),
  quantity INT NOT NULL,
  price DECIMAL(12,2) NOT NULL,
  subtotal DECIMAL(12,2) NOT NULL
);

-- 购物车明细（持久化）
CREATE TABLE cart_item (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  quantity INT NOT NULL DEFAULT 1,
  create_time DATETIME,
  update_time DATETIME,
  UNIQUE KEY uk_user_product (user_id, product_id)
);

-- 用户收货地址（可为空，下单时可选）
CREATE TABLE user_address (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  receiver VARCHAR(64),
  phone VARCHAR(32),
  address VARCHAR(512) NOT NULL,
  create_time DATETIME,
  update_time DATETIME
);

-- 访问/行为日志
CREATE TABLE access_log (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT,
  type VARCHAR(16),
  create_time DATETIME,
  ref_id BIGINT
);
