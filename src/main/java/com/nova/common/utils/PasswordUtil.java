package com.nova.common.utils;

import cn.hutool.crypto.digest.DigestUtil;
import org.springframework.security.crypto.bcrypt.BCrypt;

/**
 * @Author huangpw
 * @Date 2026/1/6 16:18
 */
public class PasswordUtil {

    private static final String SALT = "zyxhit-admin@2026";

    // 生成盐
    public static String generateSalt() {
        return BCrypt.gensalt();
    }

    // 加密密码
    public static String hashPassword(String plainPassword, String salt) {
        return BCrypt.hashpw(plainPassword, salt);
    }

    // 校验密码
    public static boolean verifyPassword(String plainPassword, String password, String salt) {
        if (plainPassword == null || password == null) {
            return false;
        }
        try {
            return BCrypt.checkpw(plainPassword, password);
        } catch (IllegalArgumentException e) {
            // 明文或损坏的哈希不能通过登录校验。
            return false;
        }
    }

    public static String getLast6Phone(String phone) {
        if (phone == null || phone.length() < 6) {
            return phone;
        }
        return phone.substring(phone.length() - 6);
    }

    public static String encrypt(String password) {
        return DigestUtil.md5Hex(password + SALT);
    }

    public static boolean verify(String newPwd, String dbPwd) {
        return DigestUtil.md5Hex(newPwd + SALT).equals(dbPwd);
    }

    public static void main(String[] args) {
        String phone = getLast6Phone("15900049104");
        System.out.println(phone);
        String pwd = encrypt(phone);
        System.out.println(pwd);
    }
}
