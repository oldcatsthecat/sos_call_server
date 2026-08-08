package com.soscall.service;

import com.soscall.model.Binding;
import com.soscall.model.User;
import com.soscall.repository.BindingRepository;
import com.soscall.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class BindingService {

    private final BindingRepository bindingRepository;
    private final UserRepository userRepository;

    public BindingService(BindingRepository bindingRepository, UserRepository userRepository) {
        this.bindingRepository = bindingRepository;
        this.userRepository = userRepository;
    }

    /**
     * 发送绑定请求
     */
    public Map<String, Object> requestBinding(String userId, String boundUserId) {
        if (userId.equals(boundUserId)) {
            throw new RuntimeException("不能绑定自己");
        }

        User boundUser = userRepository.findById(boundUserId)
                .orElseThrow(() -> new RuntimeException("目标用户不存在"));

        // 检查是否已有绑定关系
        Optional<Binding> existing = bindingRepository.findBindingBetween(userId, boundUserId);
        if (existing.isPresent()) {
            Binding b = existing.get();
            if ("accepted".equals(b.getStatus())) {
                throw new RuntimeException("已经与该用户绑定");
            }
            if ("pending".equals(b.getStatus())) {
                throw new RuntimeException("已有待处理的绑定请求");
            }
        }

        Binding binding = new Binding(
                UUID.randomUUID().toString(),
                userId,
                boundUserId,
                "pending"
        );
        bindingRepository.save(binding);

        Map<String, Object> result = new HashMap<>();
        result.put("binding", Map.of(
                "id", binding.getId(),
                "userId", binding.getUserId(),
                "boundUserId", binding.getBoundUserId(),
                "status", binding.getStatus(),
                "boundUserName", boundUser.getUsername()
        ));
        return result;
    }

    /**
     * 响应绑定请求
     */
    public Map<String, Object> respondToBinding(String bindingId, String action, String userId) {
        Binding binding = bindingRepository.findById(bindingId)
                .orElseThrow(() -> new RuntimeException("绑定请求不存在"));

        // 只有被绑定用户可以响应
        if (!binding.getBoundUserId().equals(userId)) {
            throw new RuntimeException("只有被绑定用户可以响应此请求");
        }

        if (!"pending".equals(binding.getStatus())) {
            throw new RuntimeException("该绑定请求已处理");
        }

        String status = "accept".equals(action) ? "accepted" : "rejected";
        binding.setStatus(status);
        binding.setUpdatedAt(LocalDateTime.now());
        bindingRepository.save(binding);

        User requester = userRepository.findById(binding.getUserId()).orElse(null);
        User responder = userRepository.findById(binding.getBoundUserId()).orElse(null);

        Map<String, Object> result = new HashMap<>();
        result.put("binding", Map.of(
                "id", binding.getId(),
                "userId", binding.getUserId(),
                "boundUserId", binding.getBoundUserId(),
                "status", status,
                "requesterName", requester != null ? requester.getUsername() : "",
                "responderName", responder != null ? responder.getUsername() : ""
        ));
        return result;
    }

    /**
     * 获取用户的所有绑定关系
     */
    public List<Map<String, Object>> getBindingsForUser(String userId) {
        List<Binding> bindings = bindingRepository.findBindingsForUser(userId);
        List<Map<String, Object>> result = new ArrayList<>();

        for (Binding b : bindings) {
            String otherUserId = b.getUserId().equals(userId) ? b.getBoundUserId() : b.getUserId();
            User otherUser = userRepository.findById(otherUserId).orElse(null);

            Map<String, Object> item = new HashMap<>();
            item.put("id", b.getId());
            item.put("userId", b.getUserId());
            item.put("boundUserId", b.getBoundUserId());
            item.put("status", b.getStatus());
            item.put("isRequester", b.getUserId().equals(userId));
            item.put("otherUser", otherUser != null
                    ? Map.of("id", otherUser.getId(), "username", otherUser.getUsername())
                    : null);
            item.put("createdAt", b.getCreatedAt() != null ? b.getCreatedAt().toString() : null);
            item.put("updatedAt", b.getUpdatedAt() != null ? b.getUpdatedAt().toString() : null);
            result.add(item);
        }
        return result;
    }

    /**
     * 获取待处理的绑定请求
     */
    public List<Map<String, Object>> getPendingBindingsForUser(String userId) {
        List<Binding> pending = bindingRepository.findByBoundUserIdAndStatus(userId, "pending");
        List<Map<String, Object>> result = new ArrayList<>();

        for (Binding b : pending) {
            User requester = userRepository.findById(b.getUserId()).orElse(null);
            Map<String, Object> item = new HashMap<>();
            item.put("id", b.getId());
            item.put("requester", requester != null
                    ? Map.of("id", requester.getId(), "username", requester.getUsername())
                    : null);
            item.put("createdAt", b.getCreatedAt() != null ? b.getCreatedAt().toString() : null);
            result.add(item);
        }
        return result;
    }

    /**
     * 获取已接受的绑定
     */
    public List<Map<String, Object>> getAcceptedBindingsForUser(String userId) {
        List<Binding> accepted = bindingRepository.findAcceptedBindingsForUser(userId);
        List<Map<String, Object>> result = new ArrayList<>();

        for (Binding b : accepted) {
            String otherUserId = b.getUserId().equals(userId) ? b.getBoundUserId() : b.getUserId();
            User otherUser = userRepository.findById(otherUserId).orElse(null);

            Map<String, Object> item = new HashMap<>();
            item.put("id", b.getId());
            item.put("boundUser", otherUser != null
                    ? Map.of("id", otherUser.getId(), "username", otherUser.getUsername())
                    : null);
            item.put("isRequester", b.getUserId().equals(userId));
            result.add(item);
        }
        return result;
    }
}
