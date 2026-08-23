package com.demo.shopping.dto;

import lombok.Data;

/**
 * 登录返回结果
 */
@Data
public class LoginVO {

    private Long userId;
    private String username;
    private String role;
    private String avatar;
    private String token;
}
