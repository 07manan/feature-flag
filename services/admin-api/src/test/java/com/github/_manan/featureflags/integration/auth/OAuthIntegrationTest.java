package com.github._manan.featureflags.integration.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github._manan.featureflags.config.TestConfig;
import com.github._manan.featureflags.config.TestOAuthConfig;
import com.github._manan.featureflags.dto.OAuthRequest;
import com.github._manan.featureflags.dto.OAuthUserInfo;
import com.github._manan.featureflags.dto.RegisterRequest;
import com.github._manan.featureflags.oauth.OAuthVerificationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import({TestConfig.class, TestOAuthConfig.class})
@Transactional
class OAuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestOAuthConfig.MockGoogleVerifier mockGoogleVerifier;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @AfterEach
    void tearDown() {
        mockGoogleVerifier.reset();
    }

    // ── Happy paths ───────────────────────────────────────────────────────

    @Test
    void oauth2Google_newUser_createsAdminAndReturnsToken() throws Exception {
        mockGoogleVerifier.setHandler(token ->
                new OAuthUserInfo("oauth-admin@test.com", "OAuth", "Admin", "google-id-1"));

        OAuthRequest request = OAuthRequest.builder().token("valid-google-token").build();

        mockMvc.perform(post("/auth/oauth2/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.email").value("oauth-admin@test.com"))
                .andExpect(jsonPath("$.user.firstName").value("OAuth"))
                .andExpect(jsonPath("$.user.lastName").value("Admin"))
                .andExpect(jsonPath("$.user.role").value("ADMIN"));
    }

    @Test
    void oauth2Google_secondUser_getsGuestRole() throws Exception {
        // First user → ADMIN
        registerLocalUser("first@test.com");

        mockGoogleVerifier.setHandler(token ->
                new OAuthUserInfo("second-oauth@test.com", "Second", "User", "google-id-2"));

        OAuthRequest request = OAuthRequest.builder().token("valid-token").build();

        mockMvc.perform(post("/auth/oauth2/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("GUEST"));
    }

    @Test
    void oauth2Google_existingEmail_mergesProvider() throws Exception {
        // Register local user first
        registerLocalUser("merge@test.com");

        mockGoogleVerifier.setHandler(token ->
                new OAuthUserInfo("merge@test.com", "Merge", "User", "google-id-merge"));

        OAuthRequest request = OAuthRequest.builder().token("merge-token").build();

        mockMvc.perform(post("/auth/oauth2/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value("merge@test.com"))
                .andExpect(jsonPath("$.user.role").value("ADMIN"));
    }

    @Test
    void oauth2Google_existingProvider_returnsSameUser() throws Exception {
        mockGoogleVerifier.setHandler(token ->
                new OAuthUserInfo("repeat@test.com", "Repeat", "User", "google-id-repeat"));

        OAuthRequest request = OAuthRequest.builder().token("first-call").build();

        // First call creates the user
        mockMvc.perform(post("/auth/oauth2/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value("repeat@test.com"));

        // Second call with same provider+id returns same user
        mockMvc.perform(post("/auth/oauth2/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value("repeat@test.com"));
    }

    // ── Error paths ───────────────────────────────────────────────────────

    @Test
    void oauth2Google_invalidToken_returnsUnauthorized() throws Exception {
        mockGoogleVerifier.setHandler(token -> {
            throw new OAuthVerificationException("Invalid Google ID token");
        });

        OAuthRequest request = OAuthRequest.builder().token("bad-token").build();

        mockMvc.perform(post("/auth/oauth2/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid Google ID token"));
    }

    @Test
    void oauth2Google_emailNotVerified_returnsUnauthorized() throws Exception {
        mockGoogleVerifier.setHandler(token -> {
            throw new OAuthVerificationException("Google email is not verified");
        });

        OAuthRequest request = OAuthRequest.builder().token("unverified-token").build();

        mockMvc.perform(post("/auth/oauth2/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Google email is not verified"));
    }

    @Test
    void oauth2_unsupportedProvider_returnsBadRequest() throws Exception {
        OAuthRequest request = OAuthRequest.builder().token("any-token").build();

        mockMvc.perform(post("/auth/oauth2/unsupported")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Unsupported OAuth provider")));
    }

    @Test
    void oauth2Google_blankToken_returnsBadRequest() throws Exception {
        OAuthRequest request = OAuthRequest.builder().token("").build();

        mockMvc.perform(post("/auth/oauth2/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.token").exists());
    }

    @Test
    void oauth2Google_missingToken_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/auth/oauth2/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.token").exists());
    }

    @Test
    void oauth2Google_malformedJson_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/auth/oauth2/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{bad json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Malformed JSON")));
    }

    @Test
    void oauth2Google_emptyLastName_usesFirstNameAsLastName() throws Exception {
        mockGoogleVerifier.setHandler(token ->
                new OAuthUserInfo("empty-last@test.com", "OnlyFirst", "", "google-id-empty-last"));

        OAuthRequest request = OAuthRequest.builder().token("empty-last-token").build();

        mockMvc.perform(post("/auth/oauth2/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.firstName").value("OnlyFirst"))
                .andExpect(jsonPath("$.user.lastName").value("OnlyFirst"));
    }

    @Test
    void oauth2_providerCaseVariant_resolves() throws Exception {
        mockGoogleVerifier.setHandler(token ->
                new OAuthUserInfo("case-test@test.com", "Case", "Test", "google-id-case"));

        OAuthRequest request = OAuthRequest.builder().token("case-token").build();

        // Uppercase provider should resolve via AuthProvider.valueOf(provider.toUpperCase())
        mockMvc.perform(post("/auth/oauth2/GOOGLE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value("case-test@test.com"));
    }

    @Test
    void oauth2Google_nullTokenField_returnsBadRequest() throws Exception {
        // JSON with null token: {"token": null}
        mockMvc.perform(post("/auth/oauth2/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\": null}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.token").exists());
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private void registerLocalUser(String email) throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email(email)
                .password("password123")
                .firstName("Local")
                .lastName("User")
                .build();

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }
}
