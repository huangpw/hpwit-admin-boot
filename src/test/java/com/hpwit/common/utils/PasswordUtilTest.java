package com.hpwit.common.utils;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class PasswordUtilTest {
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"123456", "password: $2a$10$invalid", "$2a$10$bad"})
    void rejectsInvalidStoredPasswords(String storedPassword) {
        assertFalse(PasswordUtil.verifyPassword("123456", storedPassword, null));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "$2a$10$G3qDYSkxKP5SXHYs3VUz9e9QD4fg2JupoVBryfXzbWJPGxbXvWjQS",
            "$2a$10$KWiVw8Z7Zcifw0dNMsea/eX64gQHbJjyShrpfAdmmi3EOgKtO8Ivm",
            "$2a$10$DOlFAJL9hjcdRzcYQyw3NeoDoMiELlHVm3KIsSOP6ehtPFIEAgAIm"
    })
    void recoveryHashesMatchOriginalPassword(String hash) {
        String salt = hash.substring(0, 29);
        assertEquals(hash, PasswordUtil.hashPassword("123456", salt));
        assertTrue(PasswordUtil.verifyPassword("123456", hash, salt));
        assertFalse(PasswordUtil.verifyPassword("wrong-password", hash, salt));
        assertFalse(PasswordUtil.verifyPassword(null, hash, salt));
    }
}
