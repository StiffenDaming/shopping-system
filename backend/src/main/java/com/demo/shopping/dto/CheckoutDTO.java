package com.demo.shopping.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.util.List;

/**
 * 结算下单请求
 */
@Data
public class CheckoutDTO {

    @NotBlank(message = "收货人姓名不能为空")
    private String receiverName;

    @NotBlank(message = "收货人电话不能为空")
    private String receiverPhone;

    @NotBlank(message = "收货地址不能为空")
    private String receiverAddress;

    /** 指定购物车项ID列表，为空则结算全部购物车 */
    private List<Long> cartItemIds;
}
