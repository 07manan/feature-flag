package com.github._manan.featureflags.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github._manan.featureflags.dto.EnvironmentDto;
import com.github._manan.featureflags.dto.FlagDto;
import com.github._manan.featureflags.dto.FlagValueDto;
import com.github._manan.featureflags.dto.FlagValueVariantDto;
import com.github._manan.featureflags.dto.RegisterRequest;
import com.github._manan.featureflags.entity.FlagType;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public final class TestHelper {

    public static final String ADMIN_EMAIL = "admin@test.com";
    public static final String ADMIN_PASSWORD = "password123";
    public static final String ADMIN_FIRST_NAME = "Admin";
    public static final String ADMIN_LAST_NAME = "User";

    public static final String GUEST_EMAIL = "guest@test.com";
    public static final String GUEST_PASSWORD = "password123";
    public static final String GUEST_FIRST_NAME = "Guest";
    public static final String GUEST_LAST_NAME = "User";

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private TestHelper() {
    }

    // ── Auth helpers ──────────────────────────────────────────────────────

    /** First user registered in a clean DB automatically becomes ADMIN. */
    public static String registerAndGetToken(MockMvc mockMvc, String email, String password,
                                             String firstName, String lastName) throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email(email)
                .password(password)
                .firstName(firstName)
                .lastName(lastName)
                .build();

        MvcResult result = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("token").asText();
    }

    public static String registerAdminAndGetToken(MockMvc mockMvc) throws Exception {
        return registerAndGetToken(mockMvc, ADMIN_EMAIL, ADMIN_PASSWORD,
                ADMIN_FIRST_NAME, ADMIN_LAST_NAME);
    }

    public static String registerGuestAndGetToken(MockMvc mockMvc) throws Exception {
        return registerAndGetToken(mockMvc, GUEST_EMAIL, GUEST_PASSWORD,
                GUEST_FIRST_NAME, GUEST_LAST_NAME);
    }

    public static String bearerToken(String token) {
        return "Bearer " + token;
    }

    /** Extracts the user id from an auth response ($.user.id). */
    public static String extractUserIdFromAuth(MockMvc mockMvc, String token) throws Exception {
        // Decode JWT to get user id – simpler: just register and parse response
        // Instead we use a simple approach: call /users and find by email
        // But that adds coupling. Better to extract from the register response.
        // This helper is only used for special cases.
        return null; // Use extractId on auth response's $.user.id path instead
    }

    // ── JSON helpers ──────────────────────────────────────────────────────

    /** Extracts the "id" field from a JSON response body. */
    public static String extractId(MvcResult result) throws Exception {
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.get("id").asText();
    }

    /** Extracts "user.id" from an auth response (register/login). */
    public static String extractAuthUserId(MvcResult result) throws Exception {
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.get("user").get("id").asText();
    }

    /** Registers admin and returns both the token and the user id. */
    public static String[] registerAdminAndGetTokenAndId(MockMvc mockMvc) throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email(ADMIN_EMAIL)
                .password(ADMIN_PASSWORD)
                .firstName(ADMIN_FIRST_NAME)
                .lastName(ADMIN_LAST_NAME)
                .build();

        MvcResult result = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        String authToken = root.get("token").asText();
        String userId = root.get("user").get("id").asText();
        return new String[]{authToken, userId};
    }

    // ── Environment helpers ───────────────────────────────────────────────

    /** Creates an environment and returns its ID. */
    public static String createEnvironment(MockMvc mockMvc, String token,
                                           String key, String name, String description) throws Exception {
        EnvironmentDto request = EnvironmentDto.builder()
                .key(key)
                .name(name)
                .description(description)
                .build();

        MvcResult result = mockMvc.perform(post("/environments")
                        .header("Authorization", bearerToken(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        return extractId(result);
    }

    // ── Flag helpers ──────────────────────────────────────────────────────

    /** Creates a flag and returns its ID. */
    public static String createFlag(MockMvc mockMvc, String token,
                                    String key, String name, FlagType type,
                                    String defaultValue) throws Exception {
        FlagDto request = FlagDto.builder()
                .key(key)
                .name(name)
                .type(type)
                .defaultValue(defaultValue)
                .build();

        MvcResult result = mockMvc.perform(post("/flags")
                        .header("Authorization", bearerToken(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        return extractId(result);
    }

    // ── Flag-value helpers ────────────────────────────────────────────────

    /** Creates a flag value and returns its ID. */
    public static String createFlagValue(MockMvc mockMvc, String token,
                                         String flagId, String environmentId,
                                         List<FlagValueVariantDto> variants) throws Exception {
        FlagValueDto request = FlagValueDto.builder()
                .flagId(java.util.UUID.fromString(flagId))
                .environmentId(java.util.UUID.fromString(environmentId))
                .variants(variants)
                .build();

        MvcResult result = mockMvc.perform(post("/flag-values")
                        .header("Authorization", bearerToken(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        return extractId(result);
    }
}
