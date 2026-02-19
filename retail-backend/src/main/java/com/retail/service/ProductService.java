package com.retail.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.retail.common.ResultCode;
import com.retail.entity.Product;
import com.retail.exception.BusinessException;
import com.retail.mapper.ProductMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ProductService {

    private static final String ON_SALE = "ON_SALE";
    private static final String OFF_SHELF = "OFF_SHELF";

    @Autowired
    private ProductMapper productMapper;

    /** 店家：全部商品分页 */
    public IPage<Product> listForStore(int page, int size, String name, String status) {
        LambdaQueryWrapper<Product> q = new LambdaQueryWrapper<>();
        if (name != null && !name.trim().isEmpty()) q.like(Product::getName, name.trim());
        if (status != null && !status.isEmpty()) q.eq(Product::getStatus, status);
        q.orderByDesc(Product::getCreateTime);
        return productMapper.selectPage(new Page<>(page, size), q);
    }

    /** 用户：在售且库存>0 */
    public List<Product> listForUser(String name, String orderBy) {
        LambdaQueryWrapper<Product> q = new LambdaQueryWrapper<>();
        q.eq(Product::getStatus, ON_SALE).gt(Product::getStock, 0);
        if (name != null && !name.trim().isEmpty()) q.like(Product::getName, name.trim());
        if ("price_asc".equals(orderBy)) q.orderByAsc(Product::getPrice);
        else if ("price_desc".equals(orderBy)) q.orderByDesc(Product::getPrice);
        else q.orderByDesc(Product::getCreateTime);
        return productMapper.selectList(q);
    }

    public Product getById(Long id) {
        return productMapper.selectById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void add(String name, BigDecimal price, Integer stock, String description, String imageUrl) {
        Product p = new Product();
        p.setName(name);
        p.setPrice(price);
        p.setStock(stock == null ? 0 : stock);
        p.setStatus(ON_SALE);
        p.setDescription(description);
        p.setImageUrl(imageUrl);
        p.setVersion(0);
        p.setCreateTime(LocalDateTime.now());
        p.setUpdateTime(LocalDateTime.now());
        productMapper.insert(p);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, String status) {
        Product p = productMapper.selectById(id);
        if (p == null) throw new BusinessException(ResultCode.BAD_REQUEST, "商品不存在");
        p.setStatus(OFF_SHELF.equals(status) ? OFF_SHELF : ON_SALE);
        p.setUpdateTime(LocalDateTime.now());
        productMapper.updateById(p);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateStock(Long id, Integer stock) {
        Product p = productMapper.selectById(id);
        if (p == null) throw new BusinessException(ResultCode.BAD_REQUEST, "商品不存在");
        if (stock == null || stock < 0) throw new BusinessException(ResultCode.BAD_REQUEST, "库存不能为负");
        p.setStock(stock);
        p.setUpdateTime(LocalDateTime.now());
        productMapper.updateById(p);
    }

    /** 乐观锁扣减，返回是否成功 */
    public boolean deductStock(Long productId, Integer version, int quantity) {
        return productMapper.deductStock(productId, version, quantity) > 0;
    }

    /** 回滚库存 */
    public void restoreStock(Long productId, int quantity) {
        productMapper.restoreStock(productId, quantity);
    }

    public long countLowStock(int threshold) {
        return productMapper.selectCount(new LambdaQueryWrapper<Product>()
                .eq(Product::getStatus, ON_SALE).lt(Product::getStock, threshold));
    }

    /** 库存为 0 的在售商品（用于补货提醒） */
    public List<Product> listZeroStock() {
        return productMapper.selectList(new LambdaQueryWrapper<Product>()
                .eq(Product::getStatus, ON_SALE).eq(Product::getStock, 0));
    }
}
