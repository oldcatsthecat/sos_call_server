package com.soscall.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soscall.model.Binding;
import com.soscall.model.User;
import com.soscall.repository.BindingRepository;
import com.soscall.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SOS 紧急呼救 WebSocket 处理器
 */
@Component
public class SOSWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(SOSWebSocketHandler.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 在线用户映射: userId -> Set<WebSocketSession>
    private final Map<String, Set<WebSocketSession>> onlineUsers = new ConcurrentHashMap<>();

    // 反向映射: sessionId -> userId
    private final Map<String, String> sessionUserMap = new ConcurrentHashMap<>();

    private final BindingRepository bindingRepository;
    private final UserRepository userRepository;

    public SOSWebSocketHandler(BindingRepository bindingRepository, UserRepository userRepository) {
        this.bindingRepository = bindingRepository;
        this.userRepository = userRepository;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String userId = (String) session.getAttributes().get("userId");
        if (userId == null) {
            try { session.close(); } catch (IOException ignored) {}
            return;
        }

        sessionUserMap.put(session.getId(), userId);
        onlineUsers.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(session);

        log.info("用户上线: userId={}, sessions={}", userId, onlineUsers.get(userId).size());

        // 通知客户端连接成功
        sendMessage(session, Map.of(
                "type", "connected",
                "data", Map.of("userId", userId)
        ));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            String userId = sessionUserMap.get(session.getId());
            if (userId == null) return;

            @SuppressWarnings("unchecked")
            Map<String, Object> msg = objectMapper.readValue(message.getPayload(), Map.class);
            String type = (String) msg.get("type");

            switch (type) {
                case "sos:call" -> handleSOSCall(userId, msg, session);
                case "sos:ack" -> handleSOSAck(userId, msg);
                case "bind:notify" -> handleBindNotify(userId, msg);
                default -> log.warn("未知消息类型: {}", type);
            }
        } catch (Exception e) {
            log.error("处理消息失败: {}", e.getMessage());
        }
    }

    /**
     * 处理紧急呼救：转发给被绑定用户
     */
    @SuppressWarnings("unchecked")
    private void handleSOSCall(String callerId, Map<String, Object> msg, WebSocketSession session) {
        try {
            String bindingId = (String) msg.get("bindingId");
            String boundUserId = (String) msg.get("boundUserId");

            log.info("SOS呼救: caller={} -> boundUser={}, binding={}", callerId, boundUserId, bindingId);

            // 验证绑定关系
            Optional<Binding> bindingOpt = bindingRepository.findById(bindingId);
            if (bindingOpt.isEmpty()) {
                sendMessage(session, Map.of("type", "sos:error", "data", Map.of("error", "绑定关系不存在")));
                return;
            }

            Binding binding = bindingOpt.get();
            if (!"accepted".equals(binding.getStatus())) {
                sendMessage(session, Map.of("type", "sos:error", "data", Map.of("error", "绑定关系未确认")));
                return;
            }

            // 确认双方是否为绑定关系的参与者
            boolean valid = (binding.getUserId().equals(callerId) && binding.getBoundUserId().equals(boundUserId))
                    || (binding.getBoundUserId().equals(callerId) && binding.getUserId().equals(boundUserId));
            if (!valid) {
                sendMessage(session, Map.of("type", "sos:error", "data", Map.of("error", "无权发起呼救")));
                return;
            }

            // 确定目标用户
            String targetUserId = callerId.equals(binding.getUserId())
                    ? binding.getBoundUserId() : binding.getUserId();

            // 发送给目标用户的所有在线设备
            Set<WebSocketSession> targetSessions = onlineUsers.get(targetUserId);
            User caller = userRepository.findById(callerId).orElse(null);
            String callId = System.currentTimeMillis() + "-" + callerId;
            String callerName = caller != null ? caller.getUsername() : "未知用户";

            Map<String, Object> sosData = new HashMap<>();
            sosData.put("callId", callId);
            sosData.put("callerId", callerId);
            sosData.put("callerName", callerName);
            sosData.put("bindingId", binding.getId());
            sosData.put("timestamp", new Date().toString());

            boolean delivered = false;

            if (targetSessions != null && !targetSessions.isEmpty()) {
                for (WebSocketSession s : targetSessions) {
                    if (s.isOpen()) {
                        sendMessage(s, Map.of("type", "sos:incoming", "data", sosData));
                        delivered = true;
                    }
                }
            }

            if (delivered) {
                sendMessage(session, Map.of("type", "sos:delivered",
                        "data", Map.of("callId", callId, "message", "紧急呼救已发送")));
            } else {
                sendMessage(session, Map.of("type", "sos:error", "data", Map.of("error", "对方不在线")));
            }
        } catch (Exception e) {
            log.error("SOS呼救处理失败: {}", e.getMessage());
        }
    }

    /**
     * 处理被绑定用户的确认（关闭震动响铃）
     */
    @SuppressWarnings("unchecked")
    private void handleSOSAck(String ackerId, Map<String, Object> msg) {
        try {
            Map<String, Object> data = (Map<String, Object>) msg.get("data");
            String callId = (String) data.get("callId");
            String callerId = (String) data.get("callerId");

            log.info("SOS确认: acker={} -> caller={}", ackerId, callerId);

            // 通知呼救者对方已确认
            Set<WebSocketSession> callerSessions = onlineUsers.get(callerId);
            if (callerSessions != null) {
                User acker = userRepository.findById(ackerId).orElse(null);
                for (WebSocketSession s : callerSessions) {
                    if (s.isOpen()) {
                        sendMessage(s, Map.of("type", "sos:acknowledged",
                                "data", Map.of(
                                        "callId", callId,
                                        "acknowledgedBy", acker != null ? acker.getUsername() : "对方",
                                        "timestamp", new Date().toString()
                                )));
                    }
                }
            }
        } catch (Exception e) {
            log.error("SOS确认处理失败: {}", e.getMessage());
        }
    }

    /**
     * 通知对方有新的绑定请求/响应
     */
    @SuppressWarnings("unchecked")
    private void handleBindNotify(String userId, Map<String, Object> msg) {
        try {
            Map<String, Object> data = (Map<String, Object>) msg.get("data");
            String targetUserId = (String) data.get("targetUserId");

            Set<WebSocketSession> targetSessions = onlineUsers.get(targetUserId);
            if (targetSessions != null) {
                Map<String, Object> notify = Map.of(
                        "type", "bind:update",
                        "data", Map.of(
                                "bindingId", data.getOrDefault("bindingId", ""),
                                "action", data.getOrDefault("action", ""),
                                "fromUser", Map.of("id", userId),
                                "timestamp", new Date().toString()
                        )
                );
                for (WebSocketSession s : targetSessions) {
                    if (s.isOpen()) {
                        sendMessage(s, notify);
                    }
                }
            }
        } catch (Exception e) {
            log.error("绑定通知失败: {}", e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String userId = sessionUserMap.remove(session.getId());
        if (userId != null) {
            Set<WebSocketSession> sessions = onlineUsers.get(userId);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    onlineUsers.remove(userId);
                }
            }
            log.info("用户离线: userId={}", userId);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("WebSocket传输错误: {}", exception.getMessage());
    }

    private void sendMessage(WebSocketSession session, Map<String, Object> message) {
        try {
            if (session.isOpen()) {
                String json = objectMapper.writeValueAsString(message);
                session.sendMessage(new TextMessage(json));
            }
        } catch (IOException e) {
            log.error("发送消息失败: {}", e.getMessage());
        }
    }
}
