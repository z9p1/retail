package com.retail.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.retail.common.Result;
import com.retail.entity.Product;
import com.retail.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * 店家：商品管理（全部、上下架、库存、新增）
 */
@RestController
@RequestMapping("/api/store/products")
public class StoreProductController {

    @Autowired
    private ProductService productService;

    @GetMapping
    public Result<IPage<Product>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String status) {
        return Result.ok(productService.listForStore(page, size, name, status));
    }

    @PostMapping
    public Result<Void> add(@RequestBody MapBody body) {
        productService.add(body.name, body.price, body.stock, body.description, body.imageUrl);
        return Result.ok();
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody MapBody body) {
        productService.updateStatus(id, body.status);
        return Result.ok();
    }

    @PutMapping("/{id}/stock")
    public Result<Void> updateStock(@PathVariable Long id, @RequestBody MapBody body) {
        productService.updateStock(id, body.stock);
        return Result.ok();
    }

    @GetMapping("/low-stock")
    public Result<Long> lowStock(@RequestParam(defaultValue = "5") int threshold) {
        return Result.ok(productService.countLowStock(threshold));
    }

    public static class MapBody {
        public String name;
        public BigDecimal price;
        public Integer stock;
        public String description;
        public String imageUrl;
        public String status;
    }
}
