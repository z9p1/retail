package com.retail.controller;

import com.retail.common.Result;
import com.retail.entity.User;
import com.retail.mapper.UserMapper;
import com.retail.service.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 店家：用户监控 - 查看 Redis 在线用户，踢人下线。
 */
@RestController
@RequestMapping("/api/store")
public class StoreOnlineController {

    @Autowired
    private SessionService sessionService;
    @Autowired
    private UserMapper userMapper;

    @GetMapping("/online-users")
    public Result<List<OnlineUserDto>> listOnlineUsers() {
        Set<Long> userIds = sessionService.getOnlineUserIds();
        if (userIds == null || userIds.isEmpty()) {
            return Result.ok(java.util.Collections.emptyList());
        }
        List<OnlineUserDto> list = userIds.stream()
                .map(userMapper::selectById)
                .filter(u -> u != null)
                .map(u -> new OnlineUserDto(u.getId(), u.getUsername(), u.getNickname(), u.getRole()))
                .collect(Collectors.toList());
        return Result.ok(list);
    }

    @DeleteMapping("/online-users/{userId}")
    public Result<Void> kickUser(@PathVariable Long userId) {
        sessionService.kickUser(userId);
        return Result.ok();
    }

    public static class OnlineUserDto {
        private Long id;
        private String username;
        private String nickname;
        private String role;

        public OnlineUserDto(Long id, String username, String nickname, String role) {
            this.id = id;
            this.username = username;
            this.nickname = nickname;
            this.role = role;
        }

        public Long getId() { return id; }
        public String getUsername() { return username; }
        public String getNickname() { return nickname; }
        public String getRole() { return role; }
    }
}
