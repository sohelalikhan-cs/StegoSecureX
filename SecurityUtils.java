package com.securestegox;

import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public final class SecurityUtils {

    // SecureStegoX QR prefix
    public static final String QR_PREFIX = "SECURESTEGOX:";

    // Internal AES key used for QR payload protection
    private static final String QR_AES_KEY =
            "SxQR2026!SecureStegoX#AES256Key";

    // AES-GCM settings
    private static final String AES_ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int IV_LENGTH = 12;

    // Generate SHA-256 hash
    public static String hash(String text) {
        if (text == null) {
            return "";
        }

        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    text.getBytes(StandardCharsets.UTF_8)
            );

            StringBuilder result = new StringBuilder();

            for (byte b : hash) {
                String value = Integer.toHexString(0xff & b);

                if (value.length() == 1) {
                    result.append('0');
                }

                result.append(value);
            }

            return result.toString();
        } catch (Exception e) {
            throw new RuntimeException("Hash generation failed", e);
        }
    }

    // Hash user password
    public static String hashPassword(String password) {
        return hash(password);
    }

    // Verify password against stored hash
    public static boolean verifyPassword(
            String password,
            String storedHash) {

        if (password == null || storedHash == null) {
            return false;
        }

        String enteredHash = hashPassword(password);

        return MessageDigest.isEqual(
                enteredHash.getBytes(StandardCharsets.UTF_8),
                storedHash.getBytes(StandardCharsets.UTF_8)
        );
    }

    // Generate a secure random 256-bit secret key
    public static String generateSecretKey() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);

        StringBuilder key = new StringBuilder();

        for (byte b : bytes) {
            key.append(String.format("%02X", b));
        }

        return key.toString();
    }

    // Encrypt secret key for QR code using AES-256-GCM
    public static String encryptQRPayload(String secretKey) {
        if (secretKey == null || secretKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Secret key cannot be empty");
        }

        try {
            SecureRandom random = new SecureRandom();
            byte[] iv = new byte[IV_LENGTH];
            random.nextBytes(iv);

            SecretKeySpec key = createQRKey();

            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);

            GCMParameterSpec spec =
                    new GCMParameterSpec(GCM_TAG_LENGTH, iv);

            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    key,
                    spec
            );

            byte[] encrypted = cipher.doFinal(
                    secretKey.trim().getBytes(StandardCharsets.UTF_8)
            );

            byte[] combined =
                    new byte[IV_LENGTH + encrypted.length];

            System.arraycopy(
                    iv,
                    0,
                    combined,
                    0,
                    IV_LENGTH
            );

            System.arraycopy(
                    encrypted,
                    0,
                    combined,
                    IV_LENGTH,
                    encrypted.length
            );

            return QR_PREFIX +
                    Base64.encodeToString(
                            combined,
                            Base64.NO_WRAP
                    );
        } catch (Exception e) {
            throw new RuntimeException(
                    "QR payload encryption failed",
                    e
            );
        }
    }

    // Decrypt SecureStegoX QR payload
    public static String decryptQRPayload(String qrData) {
        if (qrData == null || qrData.trim().isEmpty()) {
            return null;
        }

        qrData = qrData.trim();

        if (!qrData.startsWith(QR_PREFIX)) {
            return null;
        }

        try {
            String encoded =
                    qrData.substring(QR_PREFIX.length());

            if (encoded.isEmpty()) {
                return null;
            }

            byte[] combined =
                    Base64.decode(
                            encoded,
                            Base64.NO_WRAP
                    );

            // IV + encrypted data must be present
            if (combined.length <= IV_LENGTH) {
                return null;
            }

            byte[] iv = new byte[IV_LENGTH];

            System.arraycopy(
                    combined,
                    0,
                    iv,
                    0,
                    IV_LENGTH
            );

            byte[] encrypted =
                    new byte[combined.length - IV_LENGTH];

            System.arraycopy(
                    combined,
                    IV_LENGTH,
                    encrypted,
                    0,
                    encrypted.length
            );

            SecretKeySpec key = createQRKey();

            Cipher cipher =
                    Cipher.getInstance(AES_ALGORITHM);

            GCMParameterSpec spec =
                    new GCMParameterSpec(
                            GCM_TAG_LENGTH,
                            iv
                    );

            cipher.init(
                    Cipher.DECRYPT_MODE,
                    key,
                    spec
            );

            byte[] decrypted =
                    cipher.doFinal(encrypted);

            return new String(
                    decrypted,
                    StandardCharsets.UTF_8
            ).trim();

        } catch (Exception e) {
            return null;
        }
    }

    // Check whether QR belongs to SecureStegoX
    public static boolean isSecureStegoXQR(String qrData) {
        if (qrData == null) {
            return false;
        }

        return qrData.trim().startsWith(QR_PREFIX);
    }

    // Create AES-256 key for QR encryption
    private static SecretKeySpec createQRKey() {
        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] keyBytes =
                    digest.digest(
                            QR_AES_KEY.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );

            return new SecretKeySpec(
                    keyBytes,
                    "AES"
            );

        } catch (Exception e) {
            throw new RuntimeException(
                    "QR AES key creation failed",
                    e
            );
        }
    }

    // Prevent object creation
    private SecurityUtils() {
    }
}