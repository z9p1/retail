package com.retail.controller;

import com.retail.common.Result;
import com.retail.entity.UserAddress;
import com.retail.service.UserAddressService;
import com.retail.util.WebUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * 用户端：收货地址 CRUD（可为空，仅本人）
 */
@RestController
@RequestMapping("/api/user/addresses")
public class UserAddressController {

    @Autowired
    private UserAddressService userAddressService;

    @GetMapping
    public Result<List<UserAddress>> list(HttpServletRequest request) {
        Long userId = WebUtil.getUserId(request);
        if (userId == null) return Result.fail(401, "请先登录");
        return Result.ok(userAddressService.listByUserId(userId));
    }

    @PostMapping
    public Result<UserAddress> add(HttpServletRequest request, @RequestBody Map<String, String> body) {
        Long userId = WebUtil.getUserId(request);
        if (userId == null) return Result.fail(401, "请先登录");
        UserAddress a = userAddressService.add(
                userId,
                body.get("receiver"),
                body.get("phone"),
                body.get("address")
        );
        return Result.ok(a);
    }

    @PutMapping("/{id}")
    public Result<Void> update(HttpServletRequest request, @PathVariable Long id, @RequestBody Map<String, String> body) {
        Long userId = WebUtil.getUserId(request);
        if (userId == null) return Result.fail(401, "请先登录");
        userAddressService.update(userId, id, body.get("receiver"), body.get("phone"), body.get("address"));
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(HttpServletRequest request, @PathVariable Long id) {
        Long userId = WebUtil.getUserId(request);
        if (userId == null) return Result.fail(401, "请先登录");
        userAddressService.delete(userId, id);
        return Result.ok();
    }
}
