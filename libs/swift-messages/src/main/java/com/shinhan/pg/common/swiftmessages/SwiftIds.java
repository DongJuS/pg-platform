package com.shinhan.pg.common.swiftmessages;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;

/** Stable business identities; event and delivery identities are allocated separately. */
public final class SwiftIds {
    private SwiftIds() {}

    public static UUID id(UUID transferId, String purpose) {
        return UUID.nameUUIDFromBytes(
                ("mvp5:" + transferId + ":" + purpose).getBytes(StandardCharsets.UTF_8));
    }

    public static String hash(String value) {
        try {
            return HexFormat.of()
                    .formatHex(
                            MessageDigest.getInstance("SHA-256")
                                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new IllegalStateException();
        }
    }

    public static void uetr(String value) {
        try {
            var uuid = UUID.fromString(value);
            SwiftProblem.require(
                    uuid.toString().equals(value) && uuid.version() == 4 && uuid.variant() == 2);
        } catch (RuntimeException invalid) {
            throw SwiftProblem.invalid();
        }
    }
}
