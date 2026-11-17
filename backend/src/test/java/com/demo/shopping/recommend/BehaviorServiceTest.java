package com.demo.shopping.recommend;

import com.demo.shopping.dto.BehaviorTrackDTO;
import com.demo.shopping.entity.UserBehavior;
import com.demo.shopping.mapper.UserBehaviorMapper;
import io.qameta.allure.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 行为采集服务单元测试
 * 覆盖：行为权重映射、非法输入拦截、游客记录、字段清洗
 */
@Epic("Shopping System")
@Feature("推荐模块 - 行为采集")
@DisplayName("行为采集服务测试")
@ExtendWith(MockitoExtension.class)
class BehaviorServiceTest {

    @Mock
    private UserBehaviorMapper userBehaviorMapper;

    @InjectMocks
    private BehaviorService behaviorService;

    private static final Long USER_ID = 1L;
    private static final Long PRODUCT_ID = 10L;

    private BehaviorTrackDTO dto(String type, Long productId) {
        BehaviorTrackDTO dto = new BehaviorTrackDTO();
        dto.setBehaviorType(type);
        dto.setProductId(productId);
        dto.setScene("DETAIL");
        return dto;
    }

    private UserBehavior captureInserted(Long userId) {
        ArgumentCaptor<UserBehavior> captor = ArgumentCaptor.forClass(UserBehavior.class);
        verify(userBehaviorMapper, times(1)).insert(captor.capture());
        return captor.getValue();
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("TC-R-001 浏览行为权重为 1.00")
    void record_view_weightIsOne() {
        Allure.step("上报一条浏览行为");
        boolean ok = behaviorService.record(USER_ID, dto("VIEW", PRODUCT_ID));

        Allure.step("校验：返回 true，权重 1.00");
        assertThat(ok).isTrue();
        UserBehavior saved = captureInserted(USER_ID);
        assertThat(saved.getWeight()).isEqualByComparingTo("1.00");
        assertThat(saved.getUserId()).isEqualTo(USER_ID);
        assertThat(saved.getProductId()).isEqualTo(PRODUCT_ID);
        assertThat(saved.getBehaviorType()).isEqualTo("VIEW");
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("TC-R-002 加购行为权重为 3.00")
    void record_cart_weightIsThree() {
        behaviorService.record(USER_ID, dto("CART", PRODUCT_ID));
        assertThat(captureInserted(USER_ID).getWeight()).isEqualByComparingTo("3.00");
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("TC-R-003 下单行为权重为 5.00")
    void record_order_weightIsFive() {
        behaviorService.record(USER_ID, dto("ORDER", PRODUCT_ID));
        assertThat(captureInserted(USER_ID).getWeight()).isEqualByComparingTo("5.00");
    }

    @Test
    @Severity(SeverityLevel.MINOR)
    @DisplayName("TC-R-004 非法行为类型被拒绝")
    void record_invalidType_rejected() {
        boolean ok = behaviorService.record(USER_ID, dto("CLICK", PRODUCT_ID));

        assertThat(ok).isFalse();
        verify(userBehaviorMapper, never()).insert(any(UserBehavior.class));
    }

    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("TC-R-005 非搜索行为缺少商品ID被拒绝")
    void record_viewWithoutProduct_rejected() {
        boolean ok = behaviorService.record(USER_ID, dto("VIEW", null));

        assertThat(ok).isFalse();
        verify(userBehaviorMapper, never()).insert(any(UserBehavior.class));
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("TC-R-006 游客行为记录为 0 号用户")
    void record_guest_userIdZero() {
        Allure.step("未登录场景：userId 传 null");
        boolean ok = behaviorService.record(null, dto("VIEW", PRODUCT_ID));

        assertThat(ok).isTrue();
        assertThat(captureInserted(null).getUserId()).isZero();
    }

    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("搜索行为允许不携带商品ID")
    void record_search_withoutProduct_allowed() {
        BehaviorTrackDTO dto = dto("SEARCH", null);
        dto.setKeyword("紫水晶");

        boolean ok = behaviorService.record(USER_ID, dto);

        assertThat(ok).isTrue();
        UserBehavior saved = captureInserted(USER_ID);
        assertThat(saved.getBehaviorType()).isEqualTo("SEARCH");
        assertThat(saved.getKeyword()).isEqualTo("紫水晶");
    }

    @Test
    @Severity(SeverityLevel.MINOR)
    @DisplayName("关键词超长被截断到 100 字符")
    void record_longKeyword_truncated() {
        BehaviorTrackDTO dto = dto("SEARCH", null);
        dto.setKeyword("a".repeat(150));

        behaviorService.record(USER_ID, dto);

        assertThat(captureInserted(USER_ID).getKeyword()).hasSize(100);
    }
}
