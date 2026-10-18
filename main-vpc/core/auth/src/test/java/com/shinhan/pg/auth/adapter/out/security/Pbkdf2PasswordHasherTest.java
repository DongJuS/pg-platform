package com.shinhan.pg.auth.adapter.out.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Pbkdf2PasswordHasherTest {
    private final Pbkdf2PasswordHasher hasher = new Pbkdf2PasswordHasher();

    @Test
    void hashVerifiesAndUsesRandomSalt() {
        var first = hasher.hash("secret-value1");
        var second = hasher.hash("secret-value1");
        assertNotEquals(first, second, "salt must be random");
        assertTrue(hasher.matches("secret-value1", first));
        assertTrue(hasher.matches("secret-value1", second));
    }

    @Test
    void wrongPasswordDoesNotMatch() {
        var stored = hasher.hash("secret-value1");
        assertFalse(hasher.matches("secret-value2", stored));
    }

    @Test
    void tamperedOrForeignHashDoesNotMatch() {
        assertFalse(hasher.matches("secret-value1", "pbkdf2$600000$AAAA$AAAA"));
        assertFalse(hasher.matches("secret-value1", "not-a-hash"));
        assertFalse(hasher.matches(null, "pbkdf2$600000$AAAA$AAAA"));
    }
}
