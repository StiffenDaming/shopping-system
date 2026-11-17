package com.demo.shopping.service;

import com.demo.shopping.dto.CaptchaVO;
import io.qameta.allure.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Base64;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 图形验证码单元测试
 * 覆盖：图片生成、正确/错误校验、一次性消费、过期失效、空参数
 */
@Epic("Shopping System")
@Feature("认证 - 图形验证码")
@DisplayName("图形验证码测试")
class CaptchaServiceTest {

    private final CaptchaService captchaService = new CaptchaService();

    @SuppressWarnings("unchecked")
    private Map<String, Object> store() throws Exception {
        Field field = CaptchaService.class.getDeclaredField("store");
        field.setAccessible(true);
        return (Map<String, Object>) field.get(captchaService);
    }

    private String codeOf(String captchaId) throws Exception {
        Object entry = store().get(captchaId);
        Field codeField = entry.getClass().getDeclaredField("code");
        codeField.setAccessible(true);
        return (String) codeField.get(entry);
    }

    private void expire(String captchaId) throws Exception {
        Object entry = store().get(captchaId);
        Field expireField = entry.getClass().getDeclaredField("expireAt");
        expireField.setAccessible(true);
        expireField.setLong(entry, System.currentTimeMillis() - 1000L);
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("生成验证码 - 返回 ID 与合法 PNG 图片")
    void create_returnsIdAndPng() {
        CaptchaVO vo = captchaService.create();

        assertThat(vo.getCaptchaId()).isNotBlank();
        assertThat(vo.getImage()).startsWith("data:image/png;base64,");

        Allure.step("校验 Base64 解码后是 PNG 文件头");
        String base64 = vo.getImage().substring("data:image/png;base64,".length());
        byte[] bytes = Base64.getDecoder().decode(base64);
        assertThat(bytes.length).isGreaterThan(100);
        assertThat(bytes[0] & 0xFF).isEqualTo(0x89);
        assertThat(bytes[1]).isEqualTo((byte) 'P');
        assertThat(bytes[2]).isEqualTo((byte) 'N');
        assertThat(bytes[3]).isEqualTo((byte) 'G');
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("校验通过 - 输入正确验证码")
    void verify_correctCode_returnsTrue() throws Exception {
        CaptchaVO vo = captchaService.create();
        String code = codeOf(vo.getCaptchaId());

        assertThat(captchaService.verify(vo.getCaptchaId(), code)).isTrue();
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("校验失败 - 输入错误验证码")
    void verify_wrongCode_returnsFalse() throws Exception {
        CaptchaVO vo = captchaService.create();
        String code = codeOf(vo.getCaptchaId());
        String wrong = code.equals("0000") ? "1111" : "0000";

        assertThat(captchaService.verify(vo.getCaptchaId(), wrong)).isFalse();
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("一次性消费 - 同一 ID 第二次校验必然失败")
    void verify_secondTime_returnsFalse() throws Exception {
        CaptchaVO vo = captchaService.create();
        String code = codeOf(vo.getCaptchaId());

        assertThat(captchaService.verify(vo.getCaptchaId(), code)).isTrue();
        assertThat(captchaService.verify(vo.getCaptchaId(), code)).isFalse();
    }

    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("过期失效 - 超过 5 分钟不可用")
    void verify_expired_returnsFalse() throws Exception {
        CaptchaVO vo = captchaService.create();
        String code = codeOf(vo.getCaptchaId());
        expire(vo.getCaptchaId());

        assertThat(captchaService.verify(vo.getCaptchaId(), code)).isFalse();
    }

    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("非法参数 - ID 或验证码为空直接失败")
    void verify_blankArgs_returnsFalse() {
        assertThat(captchaService.verify(null, "1234")).isFalse();
        assertThat(captchaService.verify("", "1234")).isFalse();
        assertThat(captchaService.verify("not-exist-id", "1234")).isFalse();

        CaptchaVO vo = captchaService.create();
        assertThat(captchaService.verify(vo.getCaptchaId(), null)).isFalse();
        assertThat(captchaService.verify(vo.getCaptchaId(), "  ")).isFalse();
    }
}
