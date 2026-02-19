package com.retail.controller;

import com.retail.common.Result;
import com.retail.service.AuthService;
import com.retail.util.WebUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/change-password")
    public Result<Void> changePassword(HttpServletRequest request, @RequestBody Map<String, String> body) {
        Long userId = WebUtil.getUserId(request);
        if (userId == null) return Result.fail(401, "请先登录");
        String oldPassword = body.get("oldPassword");
        String newPassword = body.get("newPassword");
        authService.changePassword(userId, oldPassword, newPassword);
        return Result.ok();
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        if (username == null || password == null) {
            return Result.fail(400, "账号和密码不能为空");
        }
        return Result.ok(authService.login(username, password));
    }

    @PostMapping("/register")
    public Result<Void> register(@RequestBody Map<String, String> body) {
        authService.register(
                body.get("username"),
                body.get("password"),
                body.get("role"),
                body.get("nickname"),
                body.get("phone")
        );
        return Result.ok();
    }
}
