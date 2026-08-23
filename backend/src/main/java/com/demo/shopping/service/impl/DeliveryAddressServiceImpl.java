package com.demo.shopping.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.demo.shopping.common.BusinessException;
import com.demo.shopping.dto.AddressDTO;
import com.demo.shopping.entity.DeliveryAddress;
import com.demo.shopping.mapper.DeliveryAddressMapper;
import com.demo.shopping.service.DeliveryAddressService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 收货地址服务实现
 */
@Service
public class DeliveryAddressServiceImpl extends ServiceImpl<DeliveryAddressMapper, DeliveryAddress>
        implements DeliveryAddressService {

    @Override
    public List<DeliveryAddress> listByUserId(Long userId) {
        LambdaQueryWrapper<DeliveryAddress> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DeliveryAddress::getUserId, userId)
               .orderByDesc(DeliveryAddress::getIsDefault)
               .orderByDesc(DeliveryAddress::getCreateTime);
        return baseMapper.selectList(wrapper);
    }

    @Override
    public DeliveryAddress getDefaultAddress(Long userId) {
        return baseMapper.selectOne(new LambdaQueryWrapper<DeliveryAddress>()
                .eq(DeliveryAddress::getUserId, userId)
                .eq(DeliveryAddress::getIsDefault, 1));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addAddress(Long userId, AddressDTO dto) {
        // 如果设为默认，先取消其他默认地址
        if (dto.getIsDefault() != null && dto.getIsDefault() == 1) {
            clearDefaultAddresses(userId);
        }

        DeliveryAddress address = new DeliveryAddress();
        address.setUserId(userId);
        address.setReceiverName(dto.getReceiverName());
        address.setReceiverPhone(dto.getReceiverPhone());
        address.setReceiverAddress(dto.getReceiverAddress());
        address.setIsDefault(dto.getIsDefault() != null ? dto.getIsDefault() : 0);
        baseMapper.insert(address);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAddress(Long userId, Long addressId, AddressDTO dto) {
        DeliveryAddress address = checkOwnership(userId, addressId);

        // 如果设为默认，先取消其他默认地址
        if (dto.getIsDefault() != null && dto.getIsDefault() == 1) {
            clearDefaultAddresses(userId);
        }

        address.setReceiverName(dto.getReceiverName());
        address.setReceiverPhone(dto.getReceiverPhone());
        address.setReceiverAddress(dto.getReceiverAddress());
        if (dto.getIsDefault() != null) {
            address.setIsDefault(dto.getIsDefault());
        }
        baseMapper.updateById(address);
    }

    @Override
    public void deleteAddress(Long userId, Long addressId) {
        checkOwnership(userId, addressId);
        baseMapper.deleteById(addressId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setDefault(Long userId, Long addressId) {
        checkOwnership(userId, addressId);

        // 先取消该用户所有默认地址
        clearDefaultAddresses(userId);

        // 设置新的默认地址
        baseMapper.update(null, new LambdaUpdateWrapper<DeliveryAddress>()
                .eq(DeliveryAddress::getId, addressId)
                .set(DeliveryAddress::getIsDefault, 1));
    }

    /**
     * 校验地址归属权
     */
    private DeliveryAddress checkOwnership(Long userId, Long addressId) {
        DeliveryAddress address = baseMapper.selectById(addressId);
        if (address == null) {
            throw new BusinessException("收货地址不存在");
        }
        if (!address.getUserId().equals(userId)) {
            throw new BusinessException("无权操作此收货地址");
        }
        return address;
    }

    /**
     * 清除用户所有默认地址标记
     */
    private void clearDefaultAddresses(Long userId) {
        baseMapper.update(null, new LambdaUpdateWrapper<DeliveryAddress>()
                .eq(DeliveryAddress::getUserId, userId)
                .eq(DeliveryAddress::getIsDefault, 1)
                .set(DeliveryAddress::getIsDefault, 0));
    }
}
