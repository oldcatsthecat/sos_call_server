package com.soscall.dto;

import jakarta.validation.constraints.NotBlank;

public class BindRequest {

    @NotBlank(message = "被绑定用户ID不能为空")
    private String boundUserId;

    public String getBoundUserId() { return boundUserId; }
    public void setBoundUserId(String boundUserId) { this.boundUserId = boundUserId; }
}
