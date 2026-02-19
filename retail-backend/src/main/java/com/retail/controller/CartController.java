package com.retail.controller;

import com.retail.common.Result;
import com.retail.entity.CartItem;
import com.retail.service.CartService;
import com.retail.util.WebUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * 用户端：购物车持久化接口
 */
@RestController
@RequestMapping("/api/user/cart")
public class CartController {

    @Autowired
    private CartService cartService;

    @GetMapping
    public Result<List<CartItem>> list(HttpServletRequest request) {
        Long userId = WebUtil.getUserId(request);
        if (userId == null) return Result.fail(401, "请先登录");
        return Result.ok(cartService.listByUserId(userId));
    }

    /** 加入购物车：body productId, quantity（可选，默认1） */
    @PostMapping
    public Result<CartItem> add(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Long userId = WebUtil.getUserId(request);
        if (userId == null) return Result.fail(401, "请先登录");
        Long productId = body.get("productId") != null ? Long.valueOf(body.get("productId").toString()) : null;
        int quantity = body.get("quantity") != null ? Integer.parseInt(body.get("quantity").toString()) : 1;
        if (productId == null) return Result.fail(400, "商品不能为空");
        CartItem item = cartService.addOrUpdate(userId, productId, quantity);
        return Result.ok(item);
    }

    /** 修改数量：body productId, quantity */
    @PutMapping
    public Result<Void> updateQty(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Long userId = WebUtil.getUserId(request);
        if (userId == null) return Result.fail(401, "请先登录");
        Long productId = body.get("productId") != null ? Long.valueOf(body.get("productId").toString()) : null;
        int quantity = body.get("quantity") != null ? Integer.parseInt(body.get("quantity").toString()) : 0;
        if (productId == null) return Result.fail(400, "商品不能为空");
        cartService.updateQuantity(userId, productId, quantity);
        return Result.ok();
    }

    /** 删除一项 */
    @DeleteMapping("/item/{productId}")
    public Result<Void> remove(HttpServletRequest request, @PathVariable Long productId) {
        Long userId = WebUtil.getUserId(request);
        if (userId == null) return Result.fail(401, "请先登录");
        cartService.remove(userId, productId);
        return Result.ok();
    }

    /** 清空购物车 */
    @DeleteMapping
    public Result<Void> clear(HttpServletRequest request) {
        Long userId = WebUtil.getUserId(request);
        if (userId == null) return Result.fail(401, "请先登录");
        cartService.clear(userId);
        return Result.ok();
    }
}
