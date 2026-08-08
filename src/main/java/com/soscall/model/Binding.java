package com.soscall.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "bindings")
public class Binding {

    @Id
    @Column(length = 64)
    private String id;

    /** 发起绑定的用户ID（呼救者） */
    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    /** 被绑定的用户ID（被呼救者） */
    @Column(name = "bound_user_id", nullable = false, length = 64)
    private String boundUserId;

    /** 状态: pending / accepted / rejected */
    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Binding() {}

    public Binding(String id, String userId, String boundUserId, String status) {
        this.id = id;
        this.userId = userId;
        this.boundUserId = boundUserId;
        this.status = status;
    }

    // Getters and Setters

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getBoundUserId() { return boundUserId; }
    public void setBoundUserId(String boundUserId) { this.boundUserId = boundUserId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
