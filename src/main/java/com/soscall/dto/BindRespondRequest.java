package com.soscall.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class BindRespondRequest {

    @NotBlank(message = "绑定请求ID不能为空")
    private String bindingId;

    @NotBlank(message = "操作不能为空")
    @Pattern(regexp = "accept|reject", message = "操作必须为accept或reject")
    private String action;

    public String getBindingId() { return bindingId; }
    public void setBindingId(String bindingId) { this.bindingId = bindingId; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
}
