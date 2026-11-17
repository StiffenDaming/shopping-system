package com.demo.shopping.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

/**
 * 收货地址请求 DTO
 */
@Data
public class AddressDTO {

    @NotBlank(message = "收货人姓名不能为空")
    private String receiverName;

    @NotBlank(message = "联系电话不能为空")
    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String receiverPhone;

    @NotBlank(message = "详细地址不能为空")
    private String receiverAddress;

    /** 是否设为默认地址: 0-否, 1-是 */
    private Integer isDefault;
}
