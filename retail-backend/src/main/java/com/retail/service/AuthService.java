package com.retail.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.retail.common.ResultCode;
import com.retail.entity.User;
import com.retail.exception.BusinessException;
import com.retail.mapper.UserMapper;
import com.retail.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class AuthService {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private SessionService sessionService;

    private static String encrypt(String raw) {
        return DigestUtils.md5DigestAsHex((raw + "retail_salt").getBytes());
    }

    public Map<String, Object> login(String username, String password) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user == null || !user.getPassword().equals(encrypt(password))) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "账号或密码错误");
        }
        String sessionId = UUID.randomUUID().toString();
        sessionService.saveSession(user.getId(), sessionId);
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole(), sessionId);
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userId", user.getId());
        data.put("username", user.getUsername());
        data.put("role", user.getRole());
        data.put("nickname", user.getNickname());
        data.put("phone", user.getPhone());
        return data;
    }

    /** 账号：4-32 位，仅字母、数字、下划线 */
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{4,32}$");
    /** 密码：8-20 位，须含大写、小写、数字 */
    private static final Pattern PASSWORD_UPPER = Pattern.compile("[A-Z]");
    private static final Pattern PASSWORD_LOWER = Pattern.compile("[a-z]");
    private static final Pattern PASSWORD_DIGIT = Pattern.compile("[0-9]");
    /** 手机：11 位数字 */
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");

    public void register(String username, String password, String role, String nickname, String phone) {
        if (!StringUtils.hasText(username)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "账号不能为空");
        }
        username = username.trim();
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "账号须 4-32 位，仅限字母、数字、下划线");
        }
        if (userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username)) != null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "该账号已存在，请更换");
        }
        if (!StringUtils.hasText(password)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "密码不能为空");
        }
        if (password.length() < 8 || password.length() > 20) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "密码须 8-20 位");
        }
        if (!PASSWORD_UPPER.matcher(password).find()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "密码须包含至少一个大写字母");
        }
        if (!PASSWORD_LOWER.matcher(password).find()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "密码须包含至少一个小写字母");
        }
        if (!PASSWORD_DIGIT.matcher(password).find()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "密码须包含至少一个数字");
        }
        if (StringUtils.hasText(nickname) && (nickname.trim().length() < 2 || nickname.trim().length() > 20)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "昵称须 2-20 个字符");
        }
        if (StringUtils.hasText(phone)) {
            phone = phone.trim();
            if (!PHONE_PATTERN.matcher(phone).matches()) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "手机号须为 11 位有效号码");
            }
        }
        if ("STORE".equals(role)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "店家账号不支持注册");
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(encrypt(password));
        user.setRole("USER");
        user.setNickname(StringUtils.hasText(nickname) ? nickname.trim() : username);
        user.setPhone(StringUtils.hasText(phone) ? phone.trim() : null);
        user.setCreateTime(java.time.LocalDateTime.now());
        user.setUpdateTime(java.time.LocalDateTime.now());
        userMapper.insert(user);
    }

    /** 修改密码：校验旧密码后更新为新密码（加密存储） */
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        if (userId == null) throw new BusinessException(ResultCode.UNAUTHORIZED, "未登录");
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException(ResultCode.BAD_REQUEST, "用户不存在");
        if (!StringUtils.hasText(oldPassword) || !user.getPassword().equals(encrypt(oldPassword))) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "原密码错误");
        }
        if (!StringUtils.hasText(newPassword)) throw new BusinessException(ResultCode.BAD_REQUEST, "新密码不能为空");
        if (newPassword.length() < 8 || newPassword.length() > 20) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "新密码须 8-20 位");
        }
        if (!PASSWORD_UPPER.matcher(newPassword).find()) throw new BusinessException(ResultCode.BAD_REQUEST, "新密码须包含至少一个大写字母");
        if (!PASSWORD_LOWER.matcher(newPassword).find()) throw new BusinessException(ResultCode.BAD_REQUEST, "新密码须包含至少一个小写字母");
        if (!PASSWORD_DIGIT.matcher(newPassword).find()) throw new BusinessException(ResultCode.BAD_REQUEST, "新密码须包含至少一个数字");
        user.setPassword(encrypt(newPassword));
        user.setUpdateTime(java.time.LocalDateTime.now());
        userMapper.updateById(user);
    }

    /** 更新当前用户资料：昵称、手机号（仅允许改自己） */
    public void updateProfile(Long userId, String nickname, String phone) {
        if (userId == null) throw new BusinessException(ResultCode.UNAUTHORIZED, "未登录");
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException(ResultCode.BAD_REQUEST, "用户不存在");
        if (StringUtils.hasText(nickname)) {
            nickname = nickname.trim();
            if (nickname.length() < 2 || nickname.length() > 20) throw new BusinessException(ResultCode.BAD_REQUEST, "昵称须 2-20 个字符");
            user.setNickname(nickname);
        }
        if (phone != null) {
            phone = phone.trim();
            if (phone.isEmpty()) user.setPhone(null);
            else {
                if (!PHONE_PATTERN.matcher(phone).matches()) throw new BusinessException(ResultCode.BAD_REQUEST, "手机号须为 11 位有效号码");
                user.setPhone(phone);
            }
        }
        user.setUpdateTime(java.time.LocalDateTime.now());
        userMapper.updateById(user);
    }
}
