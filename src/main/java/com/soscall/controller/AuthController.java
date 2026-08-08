package com.soscall.controller;

import com.soscall.config.JwtUtil;
import com.soscall.dto.ApiResponse;
import com.soscall.dto.LoginRequest;
import com.soscall.dto.RegisterRequest;
import com.soscall.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    public AuthController(UserService userService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            Map<String, Object> result = userService.register(request);
            return ResponseEntity.status(201).body(
                    ApiResponse.ok("注册成功")
                            .put("token", result.get("token"))
                            .put("user", result.get("user"))
            );
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            Map<String, Object> result = userService.login(request);
            return ResponseEntity.ok(
                    ApiResponse.ok("登录成功")
                            .put("token", result.get("token"))
                            .put("user", result.get("user"))
            );
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            if (!jwtUtil.validateToken(token)) {
                return ResponseEntity.status(401).body(ApiResponse.error("认证令牌无效"));
            }
            String userId = jwtUtil.getUserIdFromToken(token);
            var user = userService.getUserById(userId);
            if (user == null) {
                return ResponseEntity.status(404).body(ApiResponse.error("用户不存在"));
            }
            return ResponseEntity.ok(
                    ApiResponse.ok("成功")
                            .put("user", Map.of("id", user.getId(), "username", user.getUsername()))
            );
        } catch (Exception e) {
            return ResponseEntity.status(401).body(ApiResponse.error("认证失败"));
        }
    }

    /**
     * 搜索用户
     */
    @GetMapping("/users/search")
    public ResponseEntity<?> searchUsers(
            @RequestParam String keyword,
            @RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            if (!jwtUtil.validateToken(token)) {
                return ResponseEntity.status(401).body(ApiResponse.error("认证令牌无效"));
            }
            String userId = jwtUtil.getUserIdFromToken(token);
            List<Map<String, String>> users = userService.searchUsers(keyword, userId);
            return ResponseEntity.ok(ApiResponse.ok("成功").put("users", users));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(ApiResponse.error("认证失败"));
        }
    }

}
