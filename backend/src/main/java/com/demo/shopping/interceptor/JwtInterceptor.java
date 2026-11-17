package com.demo.shopping.interceptor;

import com.demo.shopping.common.JwtUtils;
import com.demo.shopping.common.UserContext;
import com.demo.shopping.entity.User;
import com.demo.shopping.mapper.UserMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Resource;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 认证拦截器
 */
@Component
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtils jwtUtils;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Resource
    private UserMapper userMapper;

    public JwtInterceptor(JwtUtils jwtUtils) {
        this.jwtUtils = jwtUtils;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // OPTIONS 预检请求直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        if (token == null || token.isEmpty()) {
            writeError(response, 401, "未登录，请先登录");
            return false;
        }

        if (!jwtUtils.validateToken(token)) {
            writeError(response, 401, "登录已过期，请重新登录");
            return false;
        }

        try {
            Claims claims = jwtUtils.parseToken(token);
            Long userId = claims.get("userId", Long.class);
            String username = claims.getSubject();
            String role = claims.get("role", String.class);

            // JWT 是无状态的，签发后无法主动撤销；因此每次请求额外校验：
            // 1) 用户是否仍存在；2) 是否被禁用；3) 令牌版本是否与库中一致（改密/禁用会 +1）
            User user = userMapper.selectById(userId);
            if (user == null) {
                writeError(response, 401, "用户不存在，请重新登录");
                return false;
            }
            if (user.getStatus() != null && user.getStatus() == 0) {
                writeError(response, 401, "账号已被禁用，请联系管理员");
                return false;
            }
            Integer tokenVersion = claims.get("tv", Integer.class);
            int currentVersion = user.getTokenVersion() == null ? 1 : user.getTokenVersion();
            if (tokenVersion != null && tokenVersion != currentVersion) {
                writeError(response, 401, "登录状态已失效，请重新登录");
                return false;
            }

            UserContext.set(new UserContext.LoginUser(userId, username, role));
            return true;
        } catch (Exception e) {
            writeError(response, 401, "无效的Token");
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.remove();
    }

    private void writeError(HttpServletResponse response, int code, String message) throws Exception {
        response.setStatus(code);
        response.setContentType("application/json;charset=UTF-8");
        Map<String, Object> result = new HashMap<>();
        result.put("code", code);
        result.put("message", message);
        result.put("data", null);
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
