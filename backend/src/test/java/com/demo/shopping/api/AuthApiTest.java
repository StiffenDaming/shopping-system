package com.demo.shopping.api;

import com.demo.shopping.controller.AuthController;
import com.demo.shopping.dto.LoginDTO;
import com.demo.shopping.dto.LoginVO;
import com.demo.shopping.dto.RegisterDTO;
import com.demo.shopping.entity.User;
import com.demo.shopping.service.UserService;
import com.fasterxml.jackson.databind.JsonNode;
import io.qameta.allure.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/**
 * 认证 API 集成测试
 * 覆盖：登录、注册、获取个人信息、修改密码、未授权访问
 */
@Epic("Shopping System")
@Feature("认证 API")
@DisplayName("认证 API 集成测试")
@WebMvcTest(controllers = AuthController.class)
@Import(TestJwtConfig.class)
class AuthApiTest extends BaseApiTest {

    @MockBean
    private UserService userService;

    // ==================== 登录 ====================

    @Nested
    @Story("用户登录")
    @DisplayName("登录接口 POST /api/auth/login")
    class LoginTest {

        @Test
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("登录成功 - 返回 JWT Token")
        void login_Success() throws Exception {
            Allure.step("准备 Mock：UserService.login 返回 LoginVO");
            LoginVO loginVO = new LoginVO();
            loginVO.setUserId(1L);
            loginVO.setUsername("admin");
            loginVO.setRole("ADMIN");
            loginVO.setAvatar("/avatar/admin.png");
            loginVO.setToken("mock-jwt-token");
            when(userService.login(any(LoginDTO.class))).thenReturn(loginVO);

            Allure.step("发起 POST /api/auth/login，请求体含用户名和密码");
            LoginDTO dto = new LoginDTO();
            dto.setUsername("admin");
            dto.setPassword("123456");

            MvcResult result = performWithBody(post("/api/auth/login"), null, dto);

            Allure.step("验证：HTTP 200，code=200，返回 token 和用户信息");
            assertThat(result.getResponse().getStatus()).isEqualTo(200);
            JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
            assertThat(body.get("code").asInt()).isEqualTo(200);
            assertThat(body.get("message").asText()).isEqualTo("登录成功");
            assertThat(body.get("data").get("token").asText()).isNotNull();
            assertThat(body.get("data").get("username").asText()).isEqualTo("admin");
        }

        @Test
        @Severity(SeverityLevel.NORMAL)
        @DisplayName("登录失败 - 用户名为空（参数校验）")
        void login_EmptyUsername() throws Exception {
            Allure.step("发起 POST /api/auth/login，请求体 username 为空");
            LoginDTO dto = new LoginDTO();
            dto.setUsername("");
            dto.setPassword("123456");

            MvcResult result = performWithBody(post("/api/auth/login"), null, dto);

            Allure.step("验证：code=400，提示用户名不能为空");
            JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
            assertThat(body.get("code").asInt()).isEqualTo(400);
            assertThat(body.get("message").asText()).isEqualTo("用户名不能为空");
        }

        @Test
        @Severity(SeverityLevel.NORMAL)
        @DisplayName("登录失败 - 密码为空（参数校验）")
        void login_EmptyPassword() throws Exception {
            Allure.step("发起 POST /api/auth/login，请求体 password 为空");
            LoginDTO dto = new LoginDTO();
            dto.setUsername("admin");
            dto.setPassword("");

            MvcResult result = performWithBody(post("/api/auth/login"), null, dto);

            Allure.step("验证：code=400，提示密码不能为空");
            JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
            assertThat(body.get("code").asInt()).isEqualTo(400);
            assertThat(body.get("message").asText()).isEqualTo("密码不能为空");
        }
    }

    // ==================== 注册 ====================

    @Nested
    @Story("用户注册")
    @DisplayName("注册接口 POST /api/auth/register")
    class RegisterTest {

        @Test
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("注册成功")
        void register_Success() throws Exception {
            Allure.step("准备 Mock：UserService.register 正常执行无异常");
            doNothing().when(userService).register(any(RegisterDTO.class));

            Allure.step("发起 POST /api/auth/register，请求体含用户名、密码、邮箱");
            RegisterDTO dto = new RegisterDTO();
            dto.setUsername("newuser");
            dto.setPassword("123456");
            dto.setEmail("new@test.com");

            MvcResult result = performWithBody(post("/api/auth/register"), null, dto);

            Allure.step("验证：HTTP 200，code=200，提示注册成功");
            assertThat(result.getResponse().getStatus()).isEqualTo(200);
            JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
            assertThat(body.get("code").asInt()).isEqualTo(200);
            assertThat(body.get("message").asText()).isEqualTo("注册成功");
        }

        @Test
        @Severity(SeverityLevel.NORMAL)
        @DisplayName("注册失败 - 密码长度不足6位（参数校验）")
        void register_ShortPassword() throws Exception {
            Allure.step("发起 POST /api/auth/register，密码长度=3");
            RegisterDTO dto = new RegisterDTO();
            dto.setUsername("newuser");
            dto.setPassword("123");

            MvcResult result = performWithBody(post("/api/auth/register"), null, dto);

            Allure.step("验证：code=400，提示密码长度6-20个字符");
            JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
            assertThat(body.get("code").asInt()).isEqualTo(400);
            assertThat(body.get("message").asText()).isEqualTo("密码长度6-20个字符");
        }
    }

    // ==================== 获取用户信息 ====================

    @Nested
    @Story("获取用户信息")
    @DisplayName("用户信息接口 GET /api/auth/info")
    class UserInfoTest {

        @Test
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("获取用户信息 - 成功")
        void getInfo_Success() throws Exception {
            Allure.step("准备 Mock：UserService.getUserInfo 返回用户对象");
            User user = new User();
            user.setId(2L);
            user.setUsername("testuser");
            user.setEmail("test@test.com");
            user.setPhone("13800000002");
            user.setRole("USER");
            user.setStatus(1);
            when(userService.getUserInfo(2L)).thenReturn(user);

            Allure.step("发起 GET /api/auth/info，携带用户 Token");
            MvcResult result = perform(get("/api/auth/info"), userToken());

            Allure.step("验证：HTTP 200，返回用户信息");
            assertThat(result.getResponse().getStatus()).isEqualTo(200);
            JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
            assertThat(body.get("code").asInt()).isEqualTo(200);
            assertThat(body.get("data").get("username").asText()).isEqualTo("testuser");
            assertThat(body.get("data").get("role").asText()).isEqualTo("USER");
            // password 字段应被 @JsonIgnore 排除
            assertThat(body.get("data").get("password")).isNull();
        }

        @Test
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("获取用户信息 - 未携带 Token（401）")
        void getInfo_NoToken() throws Exception {
            Allure.step("发起 GET /api/auth/info，不携带 Authorization 头");

            MvcResult result = perform(get("/api/auth/info"), null);

            Allure.step("验证：HTTP 401，提示未登录");
            assertThat(result.getResponse().getStatus()).isEqualTo(401);
            JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
            assertThat(body.get("code").asInt()).isEqualTo(401);
        }
    }
}