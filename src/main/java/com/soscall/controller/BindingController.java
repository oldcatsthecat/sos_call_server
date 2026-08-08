package com.soscall.controller;

import com.soscall.config.JwtUtil;
import com.soscall.dto.ApiResponse;
import com.soscall.dto.BindRequest;
import com.soscall.dto.BindRespondRequest;
import com.soscall.service.BindingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bind")
public class BindingController {

    private final BindingService bindingService;
    private final JwtUtil jwtUtil;

    public BindingController(BindingService bindingService, JwtUtil jwtUtil) {
        this.bindingService = bindingService;
        this.jwtUtil = jwtUtil;
    }

    private String extractUserId(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        if (!jwtUtil.validateToken(token)) {
            throw new RuntimeException("认证令牌无效");
        }
        return jwtUtil.getUserIdFromToken(token);
    }

    /**
     * 发送绑定请求
     */
    @PostMapping("/request")
    public ResponseEntity<?> requestBinding(@Valid @RequestBody BindRequest request,
                                            @RequestHeader("Authorization") String authHeader) {
        try {
            String userId = extractUserId(authHeader);
            Map<String, Object> result = bindingService.requestBinding(userId, request.getBoundUserId());
            return ResponseEntity.status(201).body(ApiResponse.ok("绑定请求已发送").put("data", result.get("binding")));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 响应绑定请求
     */
    @PostMapping("/respond")
    public ResponseEntity<?> respondToBinding(@Valid @RequestBody BindRespondRequest request,
                                              @RequestHeader("Authorization") String authHeader) {
        try {
            String userId = extractUserId(authHeader);
            Map<String, Object> result = bindingService.respondToBinding(
                    request.getBindingId(), request.getAction(), userId);
            String msg = "accept".equals(request.getAction()) ? "已接受绑定请求" : "已拒绝绑定请求";
            return ResponseEntity.ok(ApiResponse.ok(msg).put("data", result.get("binding")));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 获取所有绑定关系
     */
    @GetMapping("/list")
    public ResponseEntity<?> getBindings(@RequestHeader("Authorization") String authHeader) {
        try {
            String userId = extractUserId(authHeader);
            List<Map<String, Object>> bindings = bindingService.getBindingsForUser(userId);
            return ResponseEntity.ok(ApiResponse.ok("成功").put("bindings", bindings));
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 获取待处理的绑定请求
     */
    @GetMapping("/pending")
    public ResponseEntity<?> getPendingBindings(@RequestHeader("Authorization") String authHeader) {
        try {
            String userId = extractUserId(authHeader);
            List<Map<String, Object>> pending = bindingService.getPendingBindingsForUser(userId);
            return ResponseEntity.ok(ApiResponse.ok("成功").put("pending", pending));
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 获取已接受的绑定
     */
    @GetMapping("/accepted")
    public ResponseEntity<?> getAcceptedBindings(@RequestHeader("Authorization") String authHeader) {
        try {
            String userId = extractUserId(authHeader);
            List<Map<String, Object>> bindings = bindingService.getAcceptedBindingsForUser(userId);
            return ResponseEntity.ok(ApiResponse.ok("成功").put("bindings", bindings));
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(ApiResponse.error(e.getMessage()));
        }
    }
}
