package com.demo.shopping.common;

/**
 * 当前登录用户上下文（基于 ThreadLocal）
 */
public class UserContext {

    private static final ThreadLocal<LoginUser> CURRENT_USER = new ThreadLocal<>();

    public static void set(LoginUser user) {
        CURRENT_USER.set(user);
    }

    public static LoginUser get() {
        return CURRENT_USER.get();
    }

    public static void remove() {
        CURRENT_USER.remove();
    }

    /**
     * 获取当前用户ID
     */
    public static Long getCurrentId() {
        LoginUser user = get();
        return user != null ? user.getUserId() : null;
    }

    /**
     * 获取当前用户角色
     */
    public static String getCurrentRole() {
        LoginUser user = get();
        return user != null ? user.getRole() : null;
    }

    /**
     * 判断当前是否为管理员
     */
    public static boolean isAdmin() {
        return "ADMIN".equals(getCurrentRole());
    }

    /**
     * 登录用户信息
     */
    public static class LoginUser {
        private Long userId;
        private String username;
        private String role;

        public LoginUser(Long userId, String username, String role) {
            this.userId = userId;
            this.username = username;
            this.role = role;
        }

        public Long getUserId() { return userId; }
        public String getUsername() { return username; }
        public String getRole() { return role; }
    }
}
