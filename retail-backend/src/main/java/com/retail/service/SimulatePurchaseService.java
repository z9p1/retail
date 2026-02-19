package com.retail.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.retail.entity.Product;
import com.retail.entity.User;
import com.retail.mapper.ProductMapper;
import com.retail.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

/**
 * 模拟 customer1 每小时购买：创建订单并支付
 */
@Service
public class SimulatePurchaseService {

    private static final Logger log = LoggerFactory.getLogger(SimulatePurchaseService.class);
    private static final String CUSTOMER1_USERNAME = "customer1";

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private OrderService orderService;

    @Transactional(rollbackFor = Exception.class)
    public void runOnce() {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, CUSTOMER1_USERNAME));
        if (user == null) {
            log.warn("SimulatePurchase: customer1 not found, skip");
            return;
        }
        List<Product> products = productMapper.selectList(
                new LambdaQueryWrapper<Product>()
                        .eq(Product::getStatus, "ON_SALE")
                        .gt(Product::getStock, 0)
                        .last("LIMIT 1"));
        if (products == null || products.isEmpty()) {
            log.warn("SimulatePurchase: no product in stock, skip");
            return;
        }
        Product p = products.get(0);
        OrderService.OrderItemDto item = new OrderService.OrderItemDto();
        item.setProductId(p.getId());
        item.setQuantity(1);
        try {
            com.retail.entity.Order order = orderService.createOrder(user.getId(), Collections.singletonList(item), null);
            orderService.pay(user.getId(), order.getId());
            log.info("SimulatePurchase: order {} created and paid for customer1, product {}", order.getOrderNo(), p.getName());
        } catch (Exception e) {
            log.error("SimulatePurchase: failed", e);
        }
    }
}
