package com.demo.shopping.dto;

import lombok.Data;

/**
 * 图形验证码返回体
 */
@Data
public class CaptchaVO {

    /** 验证码标识，注册时回传 */
    private String captchaId;

    /** PNG 图片的 Base64 Data URL，可直接用于 img src */
    private String image;
}
