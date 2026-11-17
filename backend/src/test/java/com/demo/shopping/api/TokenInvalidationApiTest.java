package com.demo.shopping.api;

import com.demo.shopping.controller.CartController;
import com.demo.shopping.entity.User;
import com.demo.shopping.service.CartService;
import io.qameta.allure.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * 令牌失效机制集成测试。
 *
 * JWT 本身无状态，签发后无法主动撤销；系统通过「每次请求查库校验状态 + 令牌版本」实现撤销：
 * 1) 用户被禁用 → 立即 401；
 * 2) 密码修改 / 重置 / 禁用后令牌版本 +1 → 旧 token 立即 401。
 */
@Epic("Shopping System")
@Feature("认证 - 令牌撤销")
@DisplayName("令牌失效机制测试")
@WebMvcTest(controllers = CartController.class)
@Import(TestJwtConfig.class)
class TokenInvalidationApiTest extends BaseApiTest {

    @MockBean
    private CartService cartService;

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("用户被禁用后，已签发的 token 立即失效")
    void disabledUser_tokenRejected() throws Exception {
        Allure.step("Mock：用户 2 状态为禁用（0）");
        User disabled = new User();
        disabled.setId(2L);
        disabled.setUsername("testuser");
        disabled.setStatus(0);
        disabled.setTokenVersion(1);
        when(userMapper.selectById(2L)).thenReturn(disabled);

        Allure.step("用该用户此前签发的 token 访问购物车");
        MvcResult result = perform(get("/api/cart"), userToken());

        Allure.step("校验：返回 401，提示账号已被禁用");
        assertThat(result.getResponse().getStatus()).isEqualTo(401);
        assertThat(result.getResponse().getContentAsString()).contains("账号已被禁用");
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("改密后令牌版本变化，旧 token 失效")
    void versionMismatch_tokenRejected() throws Exception {
        Allure.step("Mock：库中令牌版本已升到 5（说明期间改过密码）");
        User changed = new User();
        changed.setId(2L);
        changed.setUsername("testuser");
        changed.setStatus(1);
        changed.setTokenVersion(5);
        when(userMapper.selectById(2L)).thenReturn(changed);

        Allure.step("携带版本为 1 的旧 token 访问");
        String oldToken = "Bearer " + jwtUtils.generateToken(2L, "testuser", "USER", 1);
        MvcResult result = perform(get("/api/cart"), oldToken);

        Allure.step("校验：返回 401，提示登录状态已失效");
        assertThat(result.getResponse().getStatus()).isEqualTo(401);
        assertThat(result.getResponse().getContentAsString()).contains("登录状态已失效");
    }

    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("版本一致时正常放行")
    void sameVersion_allowed() throws Exception {
        User user = new User();
        user.setId(2L);
        user.setUsername("testuser");
        user.setStatus(1);
        user.setTokenVersion(3);
        when(userMapper.selectById(2L)).thenReturn(user);

        String token = "Bearer " + jwtUtils.generateToken(2L, "testuser", "USER", 3);
        MvcResult result = perform(get("/api/cart"), token);

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
    }
}
