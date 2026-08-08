package com.soscall.service;

import com.soscall.config.JwtUtil;
import com.soscall.dto.LoginRequest;
import com.soscall.dto.RegisterRequest;
import com.soscall.model.User;
import com.soscall.repository.UserRepository;
import com.soscall.config.PasswordUtil;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    public UserService(UserRepository userRepository, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
    }

    /**
     * 注册新用户
     */
    public Map<String, Object> register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("用户名已存在");
        }

        User user = new User(
                UUID.randomUUID().toString(),
                request.getUsername(),
                PasswordUtil.hash(request.getPassword())
        );
        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getId());

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("user", Map.of("id", user.getId(), "username", user.getUsername()));
        return result;
    }

    /**
     * 用户登录
     */
    public Map<String, Object> login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("用户名或密码错误"));

        if (!PasswordUtil.verify(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }

        String token = jwtUtil.generateToken(user.getId());

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("user", Map.of("id", user.getId(), "username", user.getUsername()));
        return result;
    }

    /**
     * 获取用户信息
     */
    public User getUserById(String userId) {
        return userRepository.findById(userId).orElse(null);
    }

    /**
     * 保存用户（更新 FCM token 等）
     */
    public void saveUser(User user) {
        userRepository.save(user);
    }

    /**
     * 搜索用户
     */
    public List<Map<String, String>> searchUsers(String keyword, String excludeUserId) {
        List<User> users = userRepository.searchByUsername(keyword, List.of(excludeUserId));
        List<Map<String, String>> result = new ArrayList<>();
        for (User u : users) {
            Map<String, String> item = new HashMap<>();
            item.put("id", u.getId());
            item.put("username", u.getUsername());
            result.add(item);
        }
        return result;
    }
}
