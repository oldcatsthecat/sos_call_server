package com.soscall.config;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 简易密码哈希工具 (SHA-256 + Salt)
 */
public class PasswordUtil {

    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * 对密码进行哈希，返回 "salt:hash" 格式的字符串
     */
    public static String hash(String password) {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        String saltStr = Base64.getEncoder().encodeToString(salt);
        String hash = sha256(saltStr + password);
        return saltStr + ":" + hash;
    }

    /**
     * 验证密码是否匹配
     */
    public static boolean verify(String password, String stored) {
        String[] parts = stored.split(":", 2);
        if (parts.length != 2) return false;
        String salt = parts[0];
        String hash = parts[1];
        return sha256(salt + password).equals(hash);
    }

    private static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 error", e);
        }
    }
}
