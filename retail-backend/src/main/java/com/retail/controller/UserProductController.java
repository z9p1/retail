package com.retail.controller;

import com.retail.common.Result;
import com.retail.entity.Product;
import com.retail.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户：在售商品列表（白名单，无需登录可浏览）
 */
@RestController
@RequestMapping("/api/user/products")
public class UserProductController {

    @Autowired
    private ProductService productService;

    @GetMapping
    public Result<List<Product>> list(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String orderBy) {
        return Result.ok(productService.listForUser(name, orderBy));
    }

    @GetMapping("/{id}")
    public Result<Product> get(@PathVariable Long id) {
        Product p = productService.getById(id);
        if (p == null || !"ON_SALE".equals(p.getStatus())) {
            return Result.fail(4002, "商品不存在或已下架");
        }
        return Result.ok(p);
    }
}
