package com.demo.shopping.controller;

import com.demo.shopping.common.Result;
import com.demo.shopping.common.UserContext;
import com.demo.shopping.dto.CaptchaVO;
import com.demo.shopping.dto.LoginDTO;
import com.demo.shopping.dto.LoginVO;
import com.demo.shopping.dto.RegisterDTO;
import com.demo.shopping.entity.User;
import com.demo.shopping.service.CaptchaService;
import com.demo.shopping.service.UserService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

/**
 * 认证控制器：登录、注册、个人信息
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Resource
    private UserService userService;

    @Resource
    private CaptchaService captchaService;

    /**
     * 获取图形验证码（注册页使用，无需登录）
     */
    @GetMapping("/captcha")
    public Result<CaptchaVO> captcha() {
        return Result.success(captchaService.create());
    }

    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return Result.success("登录成功", userService.login(dto));
    }

    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterDTO dto) {
        userService.register(dto);
        return Result.success("注册成功", null);
    }

    @GetMapping("/info")
    public Result<User> info() {
        return Result.success(userService.getUserInfo(UserContext.getCurrentId()));
    }

    @PutMapping("/password")
    public Result<Void> updatePassword(@RequestParam String oldPassword,
                                       @RequestParam String newPassword) {
        userService.updatePassword(UserContext.getCurrentId(), oldPassword, newPassword);
        return Result.success("密码修改成功，请重新登录", null);
    }
}
