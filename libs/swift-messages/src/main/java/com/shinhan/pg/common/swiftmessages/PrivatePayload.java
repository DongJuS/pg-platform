package com.shinhan.pg.common.swiftmessages;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/** Versioned AES-256-GCM envelope, record identity authenticated as AAD. No key fallback. */
public final class PrivatePayload {
    private final SecretKeySpec key;
    private final SecureRandom random = new SecureRandom();

    public PrivatePayload(String base64Key) {
        try {
            byte[] bytes = Base64.getDecoder().decode(base64Key);
            if (bytes.length != 32) throw new IllegalArgumentException();
            key = new SecretKeySpec(bytes, "AES");
        } catch (RuntimeException invalid) {
            throw new IllegalStateException("MVP5 payload key requires 32 base64 encoded bytes");
        }
    }

    public String encrypt(String identity, String value) {
        byte[] nonce = new byte[12];
        random.nextBytes(nonce);
        try {
            var cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, nonce));
            cipher.updateAAD(identity.getBytes(StandardCharsets.UTF_8));
            return "v1."
                    + Base64.getEncoder().encodeToString(nonce)
                    + "."
                    + Base64.getEncoder()
                            .encodeToString(cipher.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception failure) {
            throw SwiftProblem.unavailable();
        }
    }

    public String decrypt(String identity, String envelope) {
        try {
            var parts = envelope.split("\\.", -1);
            if (parts.length != 3 || !"v1".equals(parts[0])) throw new IllegalArgumentException();
            var cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(
                    Cipher.DECRYPT_MODE,
                    key,
                    new GCMParameterSpec(128, Base64.getDecoder().decode(parts[1])));
            cipher.updateAAD(identity.getBytes(StandardCharsets.UTF_8));
            return new String(
                    cipher.doFinal(Base64.getDecoder().decode(parts[2])), StandardCharsets.UTF_8);
        } catch (Exception failure) {
            throw SwiftProblem.unavailable();
        }
    }
}
