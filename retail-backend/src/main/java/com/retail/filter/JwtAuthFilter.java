package com.retail.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.retail.common.Result;
import com.retail.common.ResultCode;
import com.retail.service.SessionService;
import com.retail.service.TrafficService;
import com.retail.util.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * JWT 校验；店家专用接口校验角色 STORE
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();
    private static final List<String> WHITE = Arrays.asList(
            "/api/auth/login",
            "/api/auth/register",
            "/api/user/products/**"
    );
    /** 仅店家可访问前缀 */
    private static final String STORE_PREFIX = "/api/store/";
    private static final String TRAFFIC = "/api/traffic";
    private static final String USER_ANALYSIS = "/api/user-analysis";

    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private SessionService sessionService;
    @Autowired
    private TrafficService trafficService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        if (isWhite(path)) {
            chain.doFilter(request, response);
            return;
        }
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            writeFail(response, ResultCode.UNAUTHORIZED, "未登录或登录已过期", 401);
            return;
        }
        String token = auth.substring(7);
        if (!jwtUtil.validateToken(token)) {
            writeFail(response, ResultCode.UNAUTHORIZED, "未登录或登录已过期", 401);
            return;
        }
        Claims claims = jwtUtil.parseToken(token);
        Long userId = Long.parseLong(claims.getSubject());
        String role = claims.get("role", String.class);
        String sessionId = claims.get("sid", String.class);
        SessionService.SessionStatus status = SessionService.SessionStatus.EXPIRED;
        if (sessionId != null && !sessionId.isEmpty()) {
            status = sessionService.validateAndRefresh(userId, sessionId);
        }
        if (status != SessionService.SessionStatus.OK) {
            String msg = status == SessionService.SessionStatus.KICKED ? "您的账号已在别处登录" : "未登录或登录已过期";
            writeFail(response, ResultCode.UNAUTHORIZED, msg, 401);
            return;
        }
        request.setAttribute("userId", userId);
        request.setAttribute("role", role);

        if (requireStore(path) && !"STORE".equals(role)) {
            writeFail(response, ResultCode.FORBIDDEN, "无权限", 403);
            return;
        }
        try {
            trafficService.recordUv(userId);
        } catch (Exception ignored) { /* 埋点失败不影响主流程 */ }
        chain.doFilter(request, response);
    }

    private void writeFail(HttpServletResponse response, int code, String message, int httpStatus) throws IOException {
        response.setStatus(httpStatus);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(Result.fail(code, message)));
    }

    private boolean isWhite(String path) {
        for (String p : WHITE) {
            if (PATH_MATCHER.match(p, path)) return true;
        }
        return false;
    }

    private boolean requireStore(String path) {
        return path.startsWith(STORE_PREFIX) || path.startsWith(TRAFFIC) || path.startsWith(USER_ANALYSIS);
    }
}
