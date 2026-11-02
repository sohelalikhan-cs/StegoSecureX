package com.securestegox;
import android.util.Base64;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
public class CryptoUtils {
    private static final int KEY_LENGTH = 256;   // AES-256
    private static final int ITERATION_COUNT = 65536;
    private static final int IV_LENGTH = 16;
    private static final int SALT_LENGTH = 16;
    // SHA-256
    public static byte[] sha256(byte[] data) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return digest.digest(data);
    }
    // AES ENCRYPT
    public static byte[] aesEncrypt(byte[] data, String password) throws Exception {

        SecureRandom random = new SecureRandom();
        // Generate Salt
        byte[] salt = new byte[SALT_LENGTH];
        random.nextBytes(salt);
        // Generate Key using PBKDF2
        SecretKey secretKey = generateKey(password, salt);
        // Generate IV
        byte[] iv = new byte[IV_LENGTH];
        random.nextBytes(iv);
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(iv));
        byte[] encrypted = cipher.doFinal(data);
        // Combine Salt + IV + Encrypted
        byte[] combined = new byte[salt.length + iv.length + encrypted.length];
        System.arraycopy(salt, 0, combined, 0, salt.length);
        System.arraycopy(iv, 0, combined, salt.length, iv.length);
        System.arraycopy(encrypted, 0, combined, salt.length + iv.length, encrypted.length);
        return combined;
    }
    // AES DECRYPT
    public static byte[] aesDecrypt(byte[] data, String password) throws Exception {
        // Extract Salt
        byte[] salt = Arrays.copyOfRange(data, 0, SALT_LENGTH);
        // Extract IV
        byte[] iv = Arrays.copyOfRange(data, SALT_LENGTH, SALT_LENGTH + IV_LENGTH);
        // Extract Encrypted Data
        byte[] encrypted = Arrays.copyOfRange(data,
                SALT_LENGTH + IV_LENGTH, data.length);
        SecretKey secretKey = generateKey(password, salt);
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));
        return cipher.doFinal(encrypted);
    }
    // KEY GENERATION USING PBKDF2
    private static SecretKey generateKey(String password, byte[] salt) throws Exception {
        SecretKeyFactory factory =
                SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(
                password.toCharArray(),
                salt,
                ITERATION_COUNT,
                KEY_LENGTH
        );
        SecretKey tmp = factory.generateSecret(spec);
        return new SecretKeySpec(tmp.getEncoded(), "AES");
    }
    // BASE64 ENCODE
    public static String toBase64(byte[] data) {
        return Base64.encodeToString(data, Base64.NO_WRAP);
    }
    // BASE64 DECODE
    public static byte[] fromBase64(String base64) {
        return Base64.decode(base64, Base64.NO_WRAP);
    }
}