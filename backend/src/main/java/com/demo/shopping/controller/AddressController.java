package com.demo.shopping.controller;

import com.demo.shopping.common.Result;
import com.demo.shopping.common.UserContext;
import com.demo.shopping.dto.AddressDTO;
import com.demo.shopping.entity.DeliveryAddress;
import com.demo.shopping.service.DeliveryAddressService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

/**
 * 收货地址控制器
 */
@RestController
@RequestMapping("/api/addresses")
public class AddressController {

    @Resource
    private DeliveryAddressService deliveryAddressService;

    /**
     * 查询当前用户所有收货地址
     */
    @GetMapping
    public Result<List<DeliveryAddress>> list() {
        return Result.success(deliveryAddressService.listByUserId(UserContext.getCurrentId()));
    }

    /**
     * 查询默认收货地址
     */
    @GetMapping("/default")
    public Result<DeliveryAddress> getDefault() {
        return Result.success(deliveryAddressService.getDefaultAddress(UserContext.getCurrentId()));
    }

    /**
     * 新增收货地址
     */
    @PostMapping
    public Result<Void> add(@Valid @RequestBody AddressDTO dto) {
        deliveryAddressService.addAddress(UserContext.getCurrentId(), dto);
        return Result.success("地址添加成功", null);
    }

    /**
     * 修改收货地址
     */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody AddressDTO dto) {
        deliveryAddressService.updateAddress(UserContext.getCurrentId(), id, dto);
        return Result.success("地址修改成功", null);
    }

    /**
     * 删除收货地址
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        deliveryAddressService.deleteAddress(UserContext.getCurrentId(), id);
        return Result.success("地址已删除", null);
    }

    /**
     * 设为默认地址
     */
    @PutMapping("/{id}/default")
    public Result<Void> setDefault(@PathVariable Long id) {
        deliveryAddressService.setDefault(UserContext.getCurrentId(), id);
        return Result.success("已设为默认地址", null);
    }
}
