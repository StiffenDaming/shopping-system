package com.demo.shopping.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.demo.shopping.dto.LoginDTO;
import com.demo.shopping.dto.LoginVO;
import com.demo.shopping.dto.RegisterDTO;
import com.demo.shopping.entity.User;
import com.baomidou.mybatisplus.core.metadata.IPage;

public interface UserService extends IService<User> {

    LoginVO login(LoginDTO dto);

    void register(RegisterDTO dto);

    User getUserInfo(Long userId);

    void updatePassword(Long userId, String oldPassword, String newPassword);

    void updateProfile(Long userId, User user);

    // ===== 管理员功能 =====
    IPage<User> getUserPage(Integer page, Integer size, String keyword);

    void updateUserRole(Long userId, String role);

    void updateUserStatus(Long userId, Integer status);

    void resetPassword(Long userId);
}
