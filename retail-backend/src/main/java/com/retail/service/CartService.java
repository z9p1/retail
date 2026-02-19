package com.retail.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.retail.common.ResultCode;
import com.retail.entity.CartItem;
import com.retail.entity.Product;
import com.retail.exception.BusinessException;
import com.retail.mapper.CartItemMapper;
import com.retail.mapper.ProductMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户购物车持久化：按用户+商品唯一，增删改查
 */
@Service
public class CartService {

    @Autowired
    private CartItemMapper cartItemMapper;
    @Autowired
    private ProductMapper productMapper;

    /** 查询当前用户购物车列表（含商品名称、单价、图片） */
    public List<CartItem> listByUserId(Long userId) {
        List<CartItem> list = cartItemMapper.selectList(
                new LambdaQueryWrapper<CartItem>().eq(CartItem::getUserId, userId).orderByDesc(CartItem::getUpdateTime));
        if (list == null || list.isEmpty()) return list;
        List<Long> productIds = list.stream().map(CartItem::getProductId).distinct().collect(Collectors.toList());
        List<Product> products = productMapper.selectBatchIds(productIds);
        Map<Long, Product> productMap = products.stream().collect(Collectors.toMap(Product::getId, p -> p));
        for (CartItem item : list) {
            Product p = productMap.get(item.getProductId());
            if (p != null) {
                item.setProductName(p.getName());
                item.setPrice(p.getPrice());
                item.setImageUrl(p.getImageUrl());
            }
        }
        return list;
    }

    /** 加入或更新数量：同一用户同一商品仅一条，quantity 累加或覆盖 */
    @Transactional(rollbackFor = Exception.class)
    public CartItem addOrUpdate(Long userId, Long productId, int quantity) {
        if (quantity <= 0) {
            remove(userId, productId);
            return null;
        }
        Product p = productMapper.selectById(productId);
        if (p == null) throw new BusinessException(ResultCode.BAD_REQUEST, "商品不存在");
        if (!"ON_SALE".equals(p.getStatus())) throw new BusinessException(ResultCode.PRODUCT_OFF_SHELF, "商品已下架");
        CartItem exist = cartItemMapper.selectOne(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId).eq(CartItem::getProductId, productId));
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        if (exist != null) {
            exist.setQuantity(exist.getQuantity() + quantity);
            exist.setUpdateTime(now);
            cartItemMapper.updateById(exist);
            exist.setProductName(p.getName());
            exist.setPrice(p.getPrice());
            exist.setImageUrl(p.getImageUrl());
            return exist;
        }
        CartItem item = new CartItem();
        item.setUserId(userId);
        item.setProductId(productId);
        item.setQuantity(quantity);
        item.setCreateTime(now);
        item.setUpdateTime(now);
        cartItemMapper.insert(item);
        item.setProductName(p.getName());
        item.setPrice(p.getPrice());
        item.setImageUrl(p.getImageUrl());
        return item;
    }

    /** 修改数量（覆盖） */
    @Transactional(rollbackFor = Exception.class)
    public void updateQuantity(Long userId, Long productId, int quantity) {
        if (quantity <= 0) {
            remove(userId, productId);
            return;
        }
        CartItem exist = cartItemMapper.selectOne(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId).eq(CartItem::getProductId, productId));
        if (exist == null) return;
        exist.setQuantity(quantity);
        exist.setUpdateTime(java.time.LocalDateTime.now());
        cartItemMapper.updateById(exist);
    }

    /** 删除一项 */
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long userId, Long productId) {
        cartItemMapper.delete(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId).eq(CartItem::getProductId, productId));
    }

    /** 清空当前用户购物车 */
    @Transactional(rollbackFor = Exception.class)
    public void clear(Long userId) {
        cartItemMapper.delete(new LambdaQueryWrapper<CartItem>().eq(CartItem::getUserId, userId));
    }
}
