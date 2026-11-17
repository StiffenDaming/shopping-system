package com.demo.shopping.recommend;

import com.demo.shopping.common.JwtUtils;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

/**
 * Token 解析辅助器。
 *
 * 埋点接口与公开推荐接口在拦截器白名单中（允许游客访问），
 * 因此这些接口拿不到 UserContext，需要自行尝试从请求头解析登录态：
 * 解析成功按登录用户处理，失败则按游客（userId=0）处理，不阻断请求。
 */
@Component
public class TokenResolver {

    private final JwtUtils jwtUtils;

    public TokenResolver(JwtUtils jwtUtils) {
        this.jwtUtils = jwtUtils;
    }

    /**
     * @return 登录用户ID，游客或解析失败返回 null
     */
    public Long resolveUserId(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            return null;
        }
        token = token.substring(7).trim();
        if (token.isEmpty() || !jwtUtils.validateToken(token)) {
            return null;
        }
        try {
            return jwtUtils.getUserId(token);
        } catch (Exception e) {
            return null;
        }
    }
}
