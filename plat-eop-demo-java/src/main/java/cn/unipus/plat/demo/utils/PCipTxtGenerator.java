package cn.unipus.plat.demo.utils;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.time.Instant;
import java.util.Base64;

/**
 * @Author: hushun
 * @Date: 2025/3/13
 * @Description:
 */
public class PCipTxtGenerator {
    private static final byte[] iv = new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16};

    public static String getCipTxt(String secret) {
        String cipTxt = "";
        long timestampInSeconds = Instant.now().getEpochSecond();
        try {
            cipTxt = encrypt(String.valueOf(timestampInSeconds), secret);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return cipTxt;
    }

    private static String encrypt(String content, String password) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        SecretKeySpec keySpec = new SecretKeySpec(password.getBytes(), "AES");
        IvParameterSpec ivp = new IvParameterSpec(iv);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivp);
        byte[] encrypted = cipher.doFinal(content.getBytes());
        return Base64.getEncoder().encodeToString(encrypted);
    }
}
