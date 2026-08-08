package com.soscall.dto;

import java.util.HashMap;
import java.util.Map;

/**
 * 通用API响应
 */
public class ApiResponse {

    private boolean success;
    private String message;
    private Map<String, Object> data;

    private ApiResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
        this.data = new HashMap<>();
    }

    public static ApiResponse ok(String message) {
        return new ApiResponse(true, message);
    }

    public static ApiResponse error(String message) {
        return new ApiResponse(false, message);
    }

    public ApiResponse put(String key, Object value) {
        this.data.put(key, value);
        return this;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Map<String, Object> getData() { return data; }
    public void setData(Map<String, Object> data) { this.data = data; }
}
