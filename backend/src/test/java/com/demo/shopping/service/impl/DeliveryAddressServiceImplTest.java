package com.demo.shopping.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.demo.shopping.common.BusinessException;
import com.demo.shopping.dto.AddressDTO;
import com.demo.shopping.entity.DeliveryAddress;
import com.demo.shopping.mapper.DeliveryAddressMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 收货地址服务单元测试
 * 覆盖：增删改查、设置默认地址、权限校验
 */
@DisplayName("收货地址服务测试")
@ExtendWith(MockitoExtension.class)
class DeliveryAddressServiceImplTest {

    @Mock
    private DeliveryAddressMapper addressMapper;

    @InjectMocks
    private DeliveryAddressServiceImpl addressService;

    private static final Long USER_ID = 1L;
    private static final Long ADDRESS_ID = 100L;

    @BeforeAll
    static void initTableInfo() {
        // MyBatis-Plus 在纯单元测试中需要手动初始化实体类的 TableInfo，否则 LambdaUpdateWrapper 无法解析
        Configuration configuration = new Configuration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, DeliveryAddress.class);
    }

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(addressService, "baseMapper", addressMapper);
    }

    // ==================== 查询地址列表 ====================

    @Test
    @DisplayName("查询地址列表 - 返回用户所有地址，默认地址排在前面")
    void listByUserId_Success() {
        DeliveryAddress addr1 = createAddress(1L, USER_ID, "地址A", 1);
        DeliveryAddress addr2 = createAddress(2L, USER_ID, "地址B", 0);
        when(addressMapper.selectList(any())).thenReturn(Arrays.asList(addr1, addr2));

        List<DeliveryAddress> result = addressService.listByUserId(USER_ID);

        assertEquals(2, result.size());
        assertEquals(1, result.get(0).getIsDefault()); // 默认地址排在前面
    }

    @Test
    @DisplayName("查询默认地址 - 返回用户的默认地址")
    void getDefaultAddress_Success() {
        DeliveryAddress addr = createAddress(ADDRESS_ID, USER_ID, "默认地址", 1);
        when(addressMapper.selectOne(any())).thenReturn(addr);

        DeliveryAddress result = addressService.getDefaultAddress(USER_ID);

        assertNotNull(result);
        assertEquals(1, result.getIsDefault());
    }

    @Test
    @DisplayName("查询默认地址 - 用户没有默认地址时返回null")
    void getDefaultAddress_None() {
        when(addressMapper.selectOne(any())).thenReturn(null);

        DeliveryAddress result = addressService.getDefaultAddress(USER_ID);

        assertNull(result);
    }

    // ==================== 新增地址 ====================

    @Nested
    @DisplayName("新增地址")
    class AddAddressTest {

        @Test
        @DisplayName("新增成功 - 普通地址")
        void addAddress_Normal() {
            AddressDTO dto = new AddressDTO();
            dto.setReceiverName("张三");
            dto.setReceiverPhone("13800138000");
            dto.setReceiverAddress("北京市海淀区xxx路");
            dto.setIsDefault(0);

            addressService.addAddress(USER_ID, dto);

            verify(addressMapper, times(1)).insert(any(DeliveryAddress.class));
            verify(addressMapper, never()).update(any(), any()); // 不需要清除其他默认
        }

        @Test
        @DisplayName("新增成功 - 设为默认地址时，自动取消旧默认地址")
        void addAddress_WithDefault() {
            AddressDTO dto = new AddressDTO();
            dto.setReceiverName("张三");
            dto.setReceiverPhone("13800138000");
            dto.setReceiverAddress("北京市海淀区xxx路");
            dto.setIsDefault(1);

            addressService.addAddress(USER_ID, dto);

            // 验证先清除了旧的默认地址
            verify(addressMapper, times(1)).update(isNull(), any());
            // 验证插入了新地址
            verify(addressMapper, times(1)).insert(any(DeliveryAddress.class));
        }

        @Test
        @DisplayName("新增成功 - isDefault为null时默认设为0")
        void addAddress_NullDefault() {
            AddressDTO dto = new AddressDTO();
            dto.setReceiverName("张三");
            dto.setReceiverPhone("13800138000");
            dto.setReceiverAddress("北京市海淀区xxx路");
            dto.setIsDefault(null);

            addressService.addAddress(USER_ID, dto);

            verify(addressMapper, times(1)).insert(argThat(addr -> addr.getIsDefault() == 0));
        }
    }

    // ==================== 修改地址 ====================

    @Nested
    @DisplayName("修改地址")
    class UpdateAddressTest {

        @Test
        @DisplayName("修改成功")
        void updateAddress_Success() {
            DeliveryAddress existing = createAddress(ADDRESS_ID, USER_ID, "旧地址", 0);
            AddressDTO dto = new AddressDTO();
            dto.setReceiverName("李四");
            dto.setReceiverPhone("13900139000");
            dto.setReceiverAddress("上海市浦东新区");
            dto.setIsDefault(0);

            when(addressMapper.selectById(ADDRESS_ID)).thenReturn(existing);

            addressService.updateAddress(USER_ID, ADDRESS_ID, dto);

            assertEquals("李四", existing.getReceiverName());
            assertEquals("13900139000", existing.getReceiverPhone());
            assertEquals("上海市浦东新区", existing.getReceiverAddress());
            verify(addressMapper).updateById(existing);
        }

        @Test
        @DisplayName("修改成功 - 同时设为默认，自动取消旧默认")
        void updateAddress_SetDefault() {
            DeliveryAddress existing = createAddress(ADDRESS_ID, USER_ID, "旧地址", 0);
            AddressDTO dto = new AddressDTO();
            dto.setReceiverName("李四");
            dto.setReceiverPhone("13900139000");
            dto.setReceiverAddress("上海市浦东新区");
            dto.setIsDefault(1);

            when(addressMapper.selectById(ADDRESS_ID)).thenReturn(existing);

            addressService.updateAddress(USER_ID, ADDRESS_ID, dto);

            // 验证清除了旧的默认地址
            verify(addressMapper, times(1)).update(isNull(), any());
            verify(addressMapper).updateById(existing);
            assertEquals(1, existing.getIsDefault());
        }

        @Test
        @DisplayName("修改失败 - 地址不存在")
        void updateAddress_NotFound() {
            AddressDTO dto = new AddressDTO();
            dto.setReceiverName("李四");
            dto.setReceiverPhone("13900139000");
            dto.setReceiverAddress("上海市浦东新区");

            when(addressMapper.selectById(ADDRESS_ID)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> addressService.updateAddress(USER_ID, ADDRESS_ID, dto));
            assertEquals("收货地址不存在", ex.getMessage());
        }

        @Test
        @DisplayName("修改失败 - 非本人地址")
        void updateAddress_NotOwner() {
            DeliveryAddress existing = createAddress(ADDRESS_ID, 999L, "他人地址", 0);
            AddressDTO dto = new AddressDTO();
            dto.setReceiverName("李四");
            dto.setReceiverPhone("13900139000");
            dto.setReceiverAddress("上海市浦东新区");

            when(addressMapper.selectById(ADDRESS_ID)).thenReturn(existing);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> addressService.updateAddress(USER_ID, ADDRESS_ID, dto));
            assertEquals("无权操作此收货地址", ex.getMessage());
        }
    }

    // ==================== 删除地址 ====================

    @Nested
    @DisplayName("删除地址")
    class DeleteAddressTest {

        @Test
        @DisplayName("删除成功")
        void deleteAddress_Success() {
            DeliveryAddress existing = createAddress(ADDRESS_ID, USER_ID, "测试地址", 0);
            when(addressMapper.selectById(ADDRESS_ID)).thenReturn(existing);

            addressService.deleteAddress(USER_ID, ADDRESS_ID);

            verify(addressMapper).deleteById(ADDRESS_ID);
        }

        @Test
        @DisplayName("删除失败 - 地址不存在")
        void deleteAddress_NotFound() {
            when(addressMapper.selectById(ADDRESS_ID)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> addressService.deleteAddress(USER_ID, ADDRESS_ID));
            assertEquals("收货地址不存在", ex.getMessage());
        }

        @Test
        @DisplayName("删除失败 - 非本人地址")
        void deleteAddress_NotOwner() {
            DeliveryAddress existing = createAddress(ADDRESS_ID, 999L, "他人地址", 0);
            when(addressMapper.selectById(ADDRESS_ID)).thenReturn(existing);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> addressService.deleteAddress(USER_ID, ADDRESS_ID));
            assertEquals("无权操作此收货地址", ex.getMessage());
        }
    }

    // ==================== 设置默认地址 ====================

    @Nested
    @DisplayName("设置默认地址")
    class SetDefaultTest {

        @Test
        @DisplayName("设置默认成功 - 先清除旧默认，再设置新默认")
        void setDefault_Success() {
            DeliveryAddress existing = createAddress(ADDRESS_ID, USER_ID, "测试地址", 0);
            when(addressMapper.selectById(ADDRESS_ID)).thenReturn(existing);

            addressService.setDefault(USER_ID, ADDRESS_ID);

            // clearDefaultAddresses 调用1次 update，setDefault 再调用1次，共2次
            verify(addressMapper, times(2)).update(isNull(), any());
        }

        @Test
        @DisplayName("设置默认失败 - 地址不存在")
        void setDefault_NotFound() {
            when(addressMapper.selectById(ADDRESS_ID)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> addressService.setDefault(USER_ID, ADDRESS_ID));
            assertEquals("收货地址不存在", ex.getMessage());
        }

        @Test
        @DisplayName("设置默认失败 - 非本人地址")
        void setDefault_NotOwner() {
            DeliveryAddress existing = createAddress(ADDRESS_ID, 999L, "他人地址", 0);
            when(addressMapper.selectById(ADDRESS_ID)).thenReturn(existing);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> addressService.setDefault(USER_ID, ADDRESS_ID));
            assertEquals("无权操作此收货地址", ex.getMessage());
        }
    }

    // ==================== 测试数据工厂方法 ====================

    private DeliveryAddress createAddress(Long id, Long userId, String name, Integer isDefault) {
        DeliveryAddress address = new DeliveryAddress();
        address.setId(id);
        address.setUserId(userId);
        address.setReceiverName(name);
        address.setReceiverPhone("13800138000");
        address.setReceiverAddress("测试地址" + id);
        address.setIsDefault(isDefault);
        return address;
    }
}
