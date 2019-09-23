package com.zx.encryptstack.utils;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by Lyh on
 * 2019/9/23
 */
public class AES {

    /**
     * 用秘钥进行加密
     * @param content   明文
     * @param secretKey 秘钥
     * @return byte数组的密文
     * @throws Exception
     */
    public static byte[] encrypt(String content, SecretKey secretKey) throws Exception {
        // 秘钥
        byte[] enCodeFormat = secretKey.getEncoded();
        return encrypt(content, enCodeFormat);
    }

    /**
     * 用秘钥进行加密
     * @param content   明文
     * @param secretKeyEncoded 秘钥Encoded
     * @return byte数组的密文
     * @throws Exception
     */
    public static byte[] encrypt(String content, byte[] secretKeyEncoded) throws Exception {
        // 创建AES秘钥
        SecretKeySpec key = new SecretKeySpec(secretKeyEncoded, "AES");
        // 创建密码器
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        // Cipher cipher = Cipher.getInstance("AES");
        // 初始化加密器
        cipher.init(Cipher.ENCRYPT_MODE, key);
        // 加密
        return cipher.doFinal(content.getBytes("UTF-8"));
    }




}
