package com.demo.shopping.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.demo.shopping.common.BusinessException;
import com.demo.shopping.dto.CheckoutDTO;
import com.demo.shopping.dto.StatsVO;
import com.demo.shopping.entity.*;
import com.demo.shopping.mapper.*;
import com.demo.shopping.service.OrderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 订单服务实现
 */
@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    @Resource
    private CartItemMapper cartItemMapper;
    @Resource
    private ProductMapper productMapper;
    @Resource
    private OrderItemMapper orderItemMapper;
    @Resource
    private UserMapper userMapper;
    @Resource
    private CategoryMapper categoryMapper;
    @Resource
    private ProductMapper productMapperForStats;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String checkout(Long userId, CheckoutDTO dto) {
        List<CartItem> cartItems;

        if (dto.getCartItemIds() != null && !dto.getCartItemIds().isEmpty()) {
            cartItems = cartItemMapper.selectCartWithProduct(userId).stream()
                    .filter(c -> dto.getCartItemIds().contains(c.getId()))
                    .collect(Collectors.toList());
        } else {
            cartItems = cartItemMapper.selectCartWithProduct(userId);
        }

        if (CollectionUtils.isEmpty(cartItems)) {
            throw new BusinessException("购物车为空，无法下单");
        }

        // 校验库存并计算总额
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (CartItem item : cartItems) {
            Product product = productMapper.selectById(item.getProductId());
            if (product == null || product.getStatus() == 0) {
                throw new BusinessException("商品「" + item.getProductName() + "」不存在或已下架");
            }
            if (item.getQuantity() > product.getStock()) {
                throw new BusinessException("商品「" + product.getName() + "」库存不足");
            }
            totalAmount = totalAmount.add(product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }

        // 创建订单
        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setTotalAmount(totalAmount);
        order.setStatus("PENDING");
        order.setReceiverName(dto.getReceiverName());
        order.setReceiverPhone(dto.getReceiverPhone());
        order.setReceiverAddress(dto.getReceiverAddress());
        baseMapper.insert(order);

        // 创建订单明细 & 扣减库存
        for (CartItem item : cartItems) {
            Product product = productMapper.selectById(item.getProductId());

            OrderItem orderItem = new OrderItem();
            orderItem.setOrderId(order.getId());
            orderItem.setProductId(product.getId());
            orderItem.setProductName(product.getName());
            orderItem.setProductPrice(product.getPrice());
            orderItem.setProductImage(product.getImageUrl());
            orderItem.setQuantity(item.getQuantity());
            orderItemMapper.insert(orderItem);

            // 扣减库存
            product.setStock(product.getStock() - item.getQuantity());
            productMapper.updateById(product);
        }

        // 清除已结算的购物车项
        List<Long> cartIds = cartItems.stream().map(CartItem::getId).collect(Collectors.toList());
        cartItemMapper.deleteBatchIds(cartIds);

        return order.getOrderNo();
    }

    @Override
    public IPage<Order> getUserOrders(Long userId, Integer page, Integer size, String status) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getUserId, userId);
        if (status != null && !status.isEmpty()) {
            wrapper.eq(Order::getStatus, status);
        }
        wrapper.orderByDesc(Order::getCreateTime);
        IPage<Order> result = baseMapper.selectPage(new Page<>(page, size), wrapper);
        // 填充订单明细
        result.getRecords().forEach(this::fillOrderItems);
        return result;
    }

    @Override
    public Order getOrderDetail(Long userId, Long orderId) {
        Order order = baseMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (userId != null && !order.getUserId().equals(userId)) {
            throw new BusinessException("无权查看此订单");
        }
        fillOrderItems(order);
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Long userId, Long orderId) {
        Order order = baseMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (userId != null && !order.getUserId().equals(userId)) {
            throw new BusinessException("无权操作此订单");
        }
        if (!"PENDING".equals(order.getStatus())) {
            throw new BusinessException("只能取消待发货订单");
        }

        // 恢复库存
        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, orderId));
        for (OrderItem item : items) {
            Product product = productMapper.selectById(item.getProductId());
            if (product != null) {
                product.setStock(product.getStock() + item.getQuantity());
                productMapper.updateById(product);
            }
        }

        order.setStatus("CANCELLED");
        baseMapper.updateById(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmReceipt(Long userId, Long orderId) {
        Order order = baseMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (userId != null && !order.getUserId().equals(userId)) {
            throw new BusinessException("无权操作此订单");
        }
        if (!"SHIPPED".equals(order.getStatus())) {
            throw new BusinessException("只能确认已发货的订单");
        }
        order.setStatus("COMPLETED");
        baseMapper.updateById(order);
    }

    @Override
    public Integer getUnreadOrderCount(Long userId) {
        // 查询用户上次查看订单的时间
        User user = userMapper.selectById(userId);
        if (user == null || user.getLastViewOrdersTime() == null) {
            // 从未查看过，统计所有已发货订单
            return Math.toIntExact(baseMapper.selectCount(new LambdaQueryWrapper<Order>()
                    .eq(Order::getUserId, userId)
                    .eq(Order::getStatus, "SHIPPED")));
        }
        // 统计管理员发货后（update_time > lastViewOrdersTime）且状态为已发货的订单数
        return Math.toIntExact(baseMapper.selectCount(new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
                .eq(Order::getStatus, "SHIPPED")
                .gt(Order::getUpdateTime, user.getLastViewOrdersTime())));
    }

    @Override
    public void markOrdersViewed(Long userId) {
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .set(User::getLastViewOrdersTime, LocalDateTime.now()));
    }

    // ===== 管理员功能 =====

    @Override
    public IPage<Order> getAdminOrders(Integer page, Integer size, String status, String keyword) {
        IPage<Order> result = baseMapper.selectOrderPage(new Page<>(page, size), status, keyword);
        result.getRecords().forEach(this::fillOrderItems);
        return result;
    }

    @Override
    public void shipOrder(Long orderId) {
        Order order = baseMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (!"PENDING".equals(order.getStatus())) {
            throw new BusinessException("只能对待发货订单进行发货操作");
        }
        order.setStatus("SHIPPED");
        baseMapper.updateById(order);
    }

    @Override
    public void adminCompleteOrder(Long orderId) {
        Order order = baseMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (!"SHIPPED".equals(order.getStatus())) {
            throw new BusinessException("只能对已发货订单进行强制完成操作");
        }
        order.setStatus("COMPLETED");
        baseMapper.updateById(order);
    }

    @Override
    public StatsVO getStatistics() {
        StatsVO vo = new StatsVO();
        vo.setTotalUsers(userMapper.selectCount(null));
        vo.setTotalProducts(productMapper.selectCount(null));

        LambdaQueryWrapper<Order> allOrders = new LambdaQueryWrapper<>();
        vo.setTotalOrders(baseMapper.selectCount(allOrders));

        vo.setPendingOrders(baseMapper.selectCount(new LambdaQueryWrapper<Order>()
                .eq(Order::getStatus, "PENDING")));

        // 计算总销售额（已完成订单）
        List<Order> completed = baseMapper.selectList(new LambdaQueryWrapper<Order>()
                .in(Order::getStatus, "COMPLETED", "SHIPPED"));
        BigDecimal revenue = completed.stream()
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        vo.setTotalRevenue(revenue);

        // 今日数据
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        vo.setTodayOrders(baseMapper.selectCount(new LambdaQueryWrapper<Order>()
                .ge(Order::getCreateTime, todayStart)));
        vo.setTodayUsers(userMapper.selectCount(new LambdaQueryWrapper<User>()
                .ge(User::getCreateTime, todayStart)));

        // 各状态订单数
        Map<String, Long> statusCount = new LinkedHashMap<>();
        for (String s : Arrays.asList("PENDING", "SHIPPED", "COMPLETED", "CANCELLED")) {
            statusCount.put(s, baseMapper.selectCount(new LambdaQueryWrapper<Order>().eq(Order::getStatus, s)));
        }
        vo.setOrderStatusCount(statusCount);

        // 近7天每日订单数
        List<Map<String, Object>> dailyOrders = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDateTime dayStart = LocalDate.now().minusDays(i).atStartOfDay();
            LocalDateTime dayEnd = dayStart.plusDays(1);
            Long count = baseMapper.selectCount(new LambdaQueryWrapper<Order>()
                    .ge(Order::getCreateTime, dayStart)
                    .lt(Order::getCreateTime, dayEnd));
            Map<String, Object> day = new LinkedHashMap<>();
            day.put("date", dayStart.toLocalDate().toString());
            day.put("count", count);
            dailyOrders.add(day);
        }
        vo.setDailyOrders(dailyOrders);

        // 各分类商品数
        List<Category> categories = categoryMapper.selectList(null);
        List<Map<String, Object>> categoryCount = new ArrayList<>();
        for (Category cat : categories) {
            Long count = productMapper.selectCount(new LambdaQueryWrapper<Product>()
                    .eq(Product::getCategoryId, cat.getId()));
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", cat.getName());
            m.put("count", count);
            categoryCount.add(m);
        }
        vo.setCategoryProductCount(categoryCount);

        return vo;
    }

    private void fillOrderItems(Order order) {
        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, order.getId()));
        order.setItems(items);
    }

    private String generateOrderNo() {
        return "ORD" + System.currentTimeMillis() + (int) (Math.random() * 1000);
    }
}
