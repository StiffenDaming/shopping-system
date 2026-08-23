package com.demo.shopping.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.demo.shopping.common.BusinessException;
import com.demo.shopping.common.JwtUtils;
import com.demo.shopping.dto.LoginDTO;
import com.demo.shopping.dto.LoginVO;
import com.demo.shopping.dto.RegisterDTO;
import com.demo.shopping.entity.User;
import com.demo.shopping.mapper.UserMapper;
import com.demo.shopping.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;

/**
 * 用户服务实现
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Resource
    private PasswordEncoder passwordEncoder;

    @Resource
    private JwtUtils jwtUtils;

    @Override
    public LoginVO login(LoginDTO dto) {
        User user = baseMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, dto.getUsername()));
        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }
        if (user.getStatus() == 0) {
            throw new BusinessException("账号已被禁用，请联系管理员");
        }

        String token = jwtUtils.generateToken(user.getId(), user.getUsername(), user.getRole());

        LoginVO vo = new LoginVO();
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRole(user.getRole());
        vo.setAvatar(user.getAvatar());
        vo.setToken(token);
        return vo;
    }

    @Override
    public void register(RegisterDTO dto) {
        Long count = baseMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, dto.getUsername()));
        if (count > 0) {
            throw new BusinessException("用户名已存在");
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        if (StringUtils.hasText(dto.getEmail())) {
            user.setEmail(dto.getEmail());
        }
        user.setRole("USER");
        user.setStatus(1);
        baseMapper.insert(user);
    }

    @Override
    public User getUserInfo(Long userId) {
        return baseMapper.selectById(userId);
    }

    @Override
    public void updatePassword(Long userId, String oldPassword, String newPassword) {
        User user = baseMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException("原密码错误");
        }
        User update = new User();
        update.setId(userId);
        update.setPassword(passwordEncoder.encode(newPassword));
        baseMapper.updateById(update);
    }

    @Override
    public void updateProfile(Long userId, User user) {
        user.setId(userId);
        user.setPassword(null);
        user.setRole(null);
        user.setStatus(null);
        baseMapper.updateById(user);
    }

    // ===== 管理员功能 =====

    @Override
    public IPage<User> getUserPage(Integer page, Integer size, String keyword) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(User::getUsername, keyword)
                    .or().like(User::getEmail, keyword);
        }
        wrapper.orderByDesc(User::getCreateTime);
        return baseMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    public void updateUserRole(Long userId, String role) {
        if (!"ADMIN".equals(role) && !"USER".equals(role)) {
            throw new BusinessException("角色参数非法");
        }
        User update = new User();
        update.setId(userId);
        update.setRole(role);
        baseMapper.updateById(update);
    }

    @Override
    public void updateUserStatus(Long userId, Integer status) {
        if (status != 0 && status != 1) {
            throw new BusinessException("状态参数非法");
        }
        User update = new User();
        update.setId(userId);
        update.setStatus(status);
        baseMapper.updateById(update);
    }

    @Override
    public void resetPassword(Long userId) {
        User update = new User();
        update.setId(userId);
        update.setPassword(passwordEncoder.encode("123456"));
        baseMapper.updateById(update);
    }
}
