package com.demo.shopping.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.demo.shopping.dto.AddressDTO;
import com.demo.shopping.entity.DeliveryAddress;

import java.util.List;

public interface DeliveryAddressService extends IService<DeliveryAddress> {

    /**
     * 查询用户所有收货地址
     */
    List<DeliveryAddress> listByUserId(Long userId);

    /**
     * 查询用户默认收货地址
     */
    DeliveryAddress getDefaultAddress(Long userId);

    /**
     * 新增收货地址
     */
    void addAddress(Long userId, AddressDTO dto);

    /**
     * 修改收货地址
     */
    void updateAddress(Long userId, Long addressId, AddressDTO dto);

    /**
     * 删除收货地址
     */
    void deleteAddress(Long userId, Long addressId);

    /**
     * 设置默认地址
     */
    void setDefault(Long userId, Long addressId);
}
