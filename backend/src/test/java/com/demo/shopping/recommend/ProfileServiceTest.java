package com.demo.shopping.recommend;

import com.demo.shopping.config.RecommendProperties;
import com.demo.shopping.entity.User;
import com.demo.shopping.entity.UserBehavior;
import com.demo.shopping.entity.UserProfile;
import com.demo.shopping.mapper.UserBehaviorMapper;
import com.demo.shopping.mapper.UserMapper;
import com.demo.shopping.mapper.UserProfileMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.qameta.allure.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 用户画像构建单元测试
 * 覆盖：空行为处理、时间衰减、向量归一化、活跃度分级、更新而非新增
 */
@Epic("Shopping System")
@Feature("推荐模块 - 用户画像")
@DisplayName("用户画像构建测试")
@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private UserBehaviorMapper userBehaviorMapper;
    @Mock
    private UserProfileMapper userProfileMapper;
    @Mock
    private RecommendProperties properties;
    @Mock
    private CatalogCache catalogCache;

    @InjectMocks
    private ProfileService profileService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final Long USER_ID = 1L;

    private UserBehavior behavior(Long productId, String type, String weight, LocalDateTime time) {
        UserBehavior b = new UserBehavior();
        b.setUserId(USER_ID);
        b.setProductId(productId);
        b.setBehaviorType(type);
        b.setWeight(new BigDecimal(weight));
        b.setStayMs(0);
        b.setCreateTime(time);
        return b;
    }

    private Map<String, Double> parseTags(UserProfile profile) throws Exception {
        return objectMapper.readValue(profile.getTagVector(), new TypeReference<Map<String, Double>>() {
        });
    }

    private UserProfile captureInserted() {
        ArgumentCaptor<UserProfile> captor = ArgumentCaptor.forClass(UserProfile.class);
        verify(userProfileMapper, times(1)).insert(captor.capture());
        return captor.getValue();
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("TC-R-007 无行为用户不生成画像")
    void rebuild_noBehavior_returnsFalse() {
        when(properties.getProfileWindowDays()).thenReturn(90);
        when(userBehaviorMapper.selectList(any())).thenReturn(Collections.emptyList());

        boolean built = profileService.rebuild(USER_ID);

        assertThat(built).isFalse();
        verify(userProfileMapper, never()).insert(any(UserProfile.class));
        verify(userProfileMapper, never()).updateById(any(UserProfile.class));
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("TC-R-008 时间衰减：14 天前行为权重约为一半")
    void rebuild_timeDecay_halvesOldBehavior() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime old = now.minusDays(14);

        when(properties.getProfileWindowDays()).thenReturn(90);
        when(properties.getDecayLambda()).thenReturn(0.05);
        when(userBehaviorMapper.selectList(any())).thenReturn(Arrays.asList(
                behavior(1L, "VIEW", "1.00", now),
                behavior(2L, "VIEW", "1.00", old)));
        when(userProfileMapper.selectOne(any())).thenReturn(null);
        when(catalogCache.categoryOf(any())).thenReturn(null);
        when(catalogCache.tagsOf(1L)).thenReturn(Collections.singletonList("新标签"));
        when(catalogCache.tagsOf(2L)).thenReturn(Collections.singletonList("旧标签"));

        boolean built = profileService.rebuild(USER_ID);
        assertThat(built).isTrue();

        Allure.step("校验：旧标签权重 / 新标签权重 ≈ e^(-0.7) ≈ 0.497");
        Map<String, Double> tags = parseTags(captureInserted());
        double ratio = tags.get("旧标签") / tags.get("新标签");
        assertThat(ratio).isBetween(0.45, 0.55);
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("TC-R-009 标签向量归一化到 [0,1]")
    void rebuild_vectorNormalized() throws Exception {
        LocalDateTime now = LocalDateTime.now();

        when(properties.getProfileWindowDays()).thenReturn(90);
        when(properties.getDecayLambda()).thenReturn(0.05);
        // 下单权重 5，浏览权重 1，归一化后应为 1.0 与 0.2
        when(userBehaviorMapper.selectList(any())).thenReturn(Arrays.asList(
                behavior(1L, "ORDER", "5.00", now),
                behavior(2L, "VIEW", "1.00", now)));
        when(userProfileMapper.selectOne(any())).thenReturn(null);
        when(catalogCache.categoryOf(any())).thenReturn(null);
        when(catalogCache.tagsOf(1L)).thenReturn(Collections.singletonList("强兴趣"));
        when(catalogCache.tagsOf(2L)).thenReturn(Collections.singletonList("弱兴趣"));

        profileService.rebuild(USER_ID);
        Map<String, Double> tags = parseTags(captureInserted());

        assertThat(tags.get("强兴趣")).isEqualTo(1.0);
        assertThat(tags.get("弱兴趣")).isBetween(0.15, 0.25);
        assertThat(tags.values()).allMatch(v -> v >= 0.0 && v <= 1.0);
    }

    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("TC-R-010 活跃度分级：30 条以上为 HIGH")
    void rebuild_activityLevelHigh() {
        LocalDateTime now = LocalDateTime.now();
        List<UserBehavior> behaviors = new ArrayList<>();
        for (int i = 0; i < 35; i++) {
            behaviors.add(behavior(1L, "VIEW", "1.00", now));
        }

        when(properties.getProfileWindowDays()).thenReturn(90);
        when(properties.getDecayLambda()).thenReturn(0.05);
        when(userBehaviorMapper.selectList(any())).thenReturn(behaviors);
        when(userProfileMapper.selectOne(any())).thenReturn(null);
        when(catalogCache.categoryOf(any())).thenReturn(null);
        when(catalogCache.tagsOf(1L)).thenReturn(Collections.singletonList("文玩雅器"));

        profileService.rebuild(USER_ID);

        assertThat(captureInserted().getActivityLevel()).isEqualTo("HIGH");
    }

    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("TC-R-011 已有画像时执行更新而非新增")
    void rebuild_existingProfile_updates() {
        LocalDateTime now = LocalDateTime.now();
        UserProfile existing = new UserProfile();
        existing.setId(99L);
        existing.setUserId(USER_ID);

        when(properties.getProfileWindowDays()).thenReturn(90);
        when(properties.getDecayLambda()).thenReturn(0.05);
        when(userBehaviorMapper.selectList(any())).thenReturn(
                Collections.singletonList(behavior(1L, "VIEW", "1.00", now)));
        when(userProfileMapper.selectOne(any())).thenReturn(existing);
        when(catalogCache.categoryOf(any())).thenReturn(null);
        when(catalogCache.tagsOf(1L)).thenReturn(Collections.singletonList("文玩雅器"));

        profileService.rebuild(USER_ID);

        ArgumentCaptor<UserProfile> captor = ArgumentCaptor.forClass(UserProfile.class);
        verify(userProfileMapper, times(1)).updateById(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(99L);
        verify(userProfileMapper, never()).insert(any(UserProfile.class));
    }

    @Test
    @Severity(SeverityLevel.MINOR)
    @DisplayName("空画像向量解析返回空 Map，不抛异常")
    void parseVector_nullJson_returnsEmpty() {
        assertThat(profileService.parseVector(null)).isEmpty();
        assertThat(profileService.parseVector("{}")).isEmpty();
        assertThat(profileService.parseVector("非法JSON")).isEmpty();
    }
}
