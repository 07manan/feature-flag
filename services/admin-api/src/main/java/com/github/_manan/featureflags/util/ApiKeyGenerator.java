package com.github._manan.featureflags.util;

import java.security.SecureRandom;
import java.util.Base64;

public final class ApiKeyGenerator {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int KEY_BYTE_LENGTH = 32;
    private static final String PREFIX = "ff";

    private ApiKeyGenerator() {
        // Utility class
    }

    /**
     * Generates a secure API key with format: ff_{environmentKey}_{randomBase64UrlSafe}
     * Example: ff_production_xK9mP2nQ4wE7rT1yU6iO3pA8sD5fG0hJkL
     *
     * @param environmentKey the environment's key (e.g., "production", "staging")
     * @return a unique API key string
     */
    public static String generateApiKey(String environmentKey) {
        byte[] randomBytes = new byte[KEY_BYTE_LENGTH];
        SECURE_RANDOM.nextBytes(randomBytes);
        String randomPart = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        return String.format("%s_%s_%s", PREFIX, environmentKey, randomPart);
    }
}
