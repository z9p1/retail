package com.retail.controller;

import com.retail.common.Result;
import com.retail.service.AuthService;
import com.retail.util.WebUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/** 用户端：个人资料等（需登录，店家/用户均可） */
@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private AuthService authService;

    @PutMapping("/profile")
    public Result<Void> updateProfile(HttpServletRequest request, @RequestBody Map<String, String> body) {
        Long userId = WebUtil.getUserId(request);
        if (userId == null) return Result.fail(401, "请先登录");
        authService.updateProfile(userId, body.get("nickname"), body.get("phone"));
        return Result.ok();
    }
}
