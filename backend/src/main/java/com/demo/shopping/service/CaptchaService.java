package com.demo.shopping.service;

import com.demo.shopping.dto.CaptchaVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 图形验证码服务（进程内存储）。
 *
 * 设计要点：
 * 1. 验证码存放在 ConcurrentHashMap 中，5 分钟过期，校验一次即失效（一次性消费），
 *    因此不需要引入 Redis，也不依赖任何第三方库；
 * 2. 图片用 5×7 点阵自绘，不使用系统字体 —— 容器镜像通常没有安装中文字体/字体配置，
 *    Graphics2D.drawString 可能抛 Fontconfig 错误，点阵绘制在任何环境都稳定；
 * 3. 校验忽略大小写（当前为纯数字，保留该逻辑以便后续扩展字母）。
 */
@Slf4j
@Service
public class CaptchaService {

    private static final int WIDTH = 120;
    private static final int HEIGHT = 44;
    private static final int CODE_LENGTH = 4;
    private static final int SCALE = 4;
    private static final long TTL_MILLIS = 5 * 60 * 1000L;
    private static final int MAX_ENTRIES = 10000;

    /** 5×7 点阵字模：数字 0~9，每行 5 个字符，"1" 表示实心像素 */
    private static final String[][] DIGITS = {
            {"01110", "10001", "10011", "10101", "11001", "10001", "01110"}, // 0
            {"00100", "01100", "00100", "00100", "00100", "00100", "01110"}, // 1
            {"01110", "10001", "00001", "00010", "00100", "01000", "11111"}, // 2
            {"11111", "00010", "00100", "00010", "00001", "10001", "01110"}, // 3
            {"00010", "00110", "01010", "10010", "11111", "00010", "00010"}, // 4
            {"11111", "10000", "11110", "00001", "00001", "10001", "01110"}, // 5
            {"00110", "01000", "10000", "11110", "10001", "10001", "01110"}, // 6
            {"11111", "00001", "00010", "00100", "01000", "01000", "01000"}, // 7
            {"01110", "10001", "10001", "01110", "10001", "10001", "01110"}, // 8
            {"01110", "10001", "10001", "01111", "00001", "00010", "01100"}  // 9
    };

    /** 干扰色系，避免与背景过近 */
    private static final Color[] INK = {
            new Color(0x2F, 0x55, 0x97),
            new Color(0xC5, 0x5A, 0x11),
            new Color(0x2E, 0x7D, 0x32),
            new Color(0x6A, 0x3D, 0x9A)
    };

    private final Map<String, Entry> store = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    /**
     * 生成一个新验证码。
     */
    public CaptchaVO create() {
        purge();
        StringBuilder code = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(random.nextInt(10));
        }
        String captchaId = UUID.randomUUID().toString().replace("-", "");
        store.put(captchaId, new Entry(code.toString(), System.currentTimeMillis() + TTL_MILLIS));

        CaptchaVO vo = new CaptchaVO();
        vo.setCaptchaId(captchaId);
        vo.setImage("data:image/png;base64," + render(code.toString()));
        return vo;
    }

    /**
     * 校验验证码。无论成功与否都会移除该 captchaId，防止同一张图被反复试探。
     */
    public boolean verify(String captchaId, String input) {
        if (captchaId == null || captchaId.isEmpty() || input == null || input.trim().isEmpty()) {
            return false;
        }
        Entry entry = store.remove(captchaId);
        if (entry == null) {
            return false;
        }
        if (entry.expireAt < System.currentTimeMillis()) {
            return false;
        }
        return entry.code.equalsIgnoreCase(input.trim());
    }

    /**
     * 清理过期条目，避免长期运行内存膨胀。
     */
    private void purge() {
        long now = System.currentTimeMillis();
        if (store.size() >= MAX_ENTRIES) {
            store.clear();
            return;
        }
        store.entrySet().removeIf(e -> e.getValue().expireAt < now);
    }

    /**
     * 绘制验证码图片：浅色背景 + 干扰线 + 噪点 + 点阵数字。
     */
    private String render(String code) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setColor(new Color(0xF2, 0xF6, 0xFA));
            g.fillRect(0, 0, WIDTH, HEIGHT);

            // 干扰线
            g.setStroke(new BasicStroke(1.2f));
            for (int i = 0; i < 6; i++) {
                g.setColor(new Color(random.nextInt(180) + 60,
                        random.nextInt(180) + 60,
                        random.nextInt(180) + 60));
                g.drawLine(random.nextInt(WIDTH), random.nextInt(HEIGHT),
                        random.nextInt(WIDTH), random.nextInt(HEIGHT));
            }

            // 噪点
            for (int i = 0; i < 60; i++) {
                g.setColor(new Color(random.nextInt(200) + 40,
                        random.nextInt(200) + 40,
                        random.nextInt(200) + 40));
                g.fillRect(random.nextInt(WIDTH), random.nextInt(HEIGHT), 1, 1);
            }

            // 点阵数字
            int charWidth = 5 * SCALE;
            int gap = (WIDTH - CODE_LENGTH * charWidth) / (CODE_LENGTH + 1);
            for (int i = 0; i < CODE_LENGTH; i++) {
                int digit = code.charAt(i) - '0';
                String[] pattern = DIGITS[digit];
                int x0 = gap + i * (charWidth + gap);
                int y0 = (HEIGHT - 7 * SCALE) / 2 + random.nextInt(5) - 2;
                g.setColor(INK[random.nextInt(INK.length)]);
                for (int row = 0; row < 7; row++) {
                    String line = pattern[row];
                    for (int col = 0; col < 5; col++) {
                        if (line.charAt(col) == '1') {
                            g.fillRect(x0 + col * SCALE, y0 + row * SCALE, SCALE, SCALE);
                        }
                    }
                }
            }
        } finally {
            g.dispose();
        }

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", out);
            return Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (IOException e) {
            log.error("验证码图片生成失败", e);
            throw new IllegalStateException("验证码图片生成失败", e);
        }
    }

    /**
     * 验证码条目
     */
    private static class Entry {
        private final String code;
        private final long expireAt;

        Entry(String code, long expireAt) {
            this.code = code;
            this.expireAt = expireAt;
        }
    }
}
