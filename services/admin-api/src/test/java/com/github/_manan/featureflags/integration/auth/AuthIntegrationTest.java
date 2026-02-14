package com.github._manan.featureflags.integration.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github._manan.featureflags.config.TestConfig;
import com.github._manan.featureflags.dto.LoginRequest;
import com.github._manan.featureflags.dto.RegisterRequest;
import com.github._manan.featureflags.dto.UpdateUserRequest;
import com.github._manan.featureflags.support.TestHelper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestConfig.class)
@Transactional
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void register_withValidRequest_returnsCreatedWithToken() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email("test@example.com")
                .password("password123")
                .firstName("Test")
                .lastName("User")
                .build();

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.email").value("test@example.com"))
                .andExpect(jsonPath("$.user.firstName").value("Test"))
                .andExpect(jsonPath("$.user.lastName").value("User"))
                .andExpect(jsonPath("$.user.role").value("ADMIN"));
    }

    @Test
    void register_secondUser_getsGuestRole() throws Exception {
        registerUser("admin@test.com", "password123", "Admin", "User");

        RegisterRequest second = RegisterRequest.builder()
                .email("guest@test.com")
                .password("password123")
                .firstName("Guest")
                .lastName("User")
                .build();

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(second)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.role").value("GUEST"));
    }

    @Test
    void register_withDuplicateEmail_returnsBadRequest() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email("duplicate@test.com")
                .password("password123")
                .firstName("Test")
                .lastName("User")
                .build();

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Email already registered"));
    }

    @Test
    void register_withInvalidEmail_returnsBadRequest() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email("not-an-email")
                .password("password123")
                .firstName("Test")
                .lastName("User")
                .build();

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists());
    }

    @Test
    void register_withShortPassword_returnsBadRequest() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email("test@example.com")
                .password("short")
                .firstName("Test")
                .lastName("User")
                .build();

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    void register_withMissingFields_returnsBadRequest() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email("test@example.com")
                .build();

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isNotEmpty());
    }

    @Test
    void login_withValidCredentials_returnsToken() throws Exception {
        registerUser("login@test.com", "password123", "Login", "User");

        LoginRequest loginRequest = LoginRequest.builder()
                .email("login@test.com")
                .password("password123")
                .build();

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.email").value("login@test.com"));
    }

    @Test
    void login_withWrongPassword_returnsUnauthorized() throws Exception {
        registerUser("wrongpw@test.com", "password123", "Wrong", "Password");

        LoginRequest loginRequest = LoginRequest.builder()
                .email("wrongpw@test.com")
                .password("wrongpassword")
                .build();

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void login_withNonExistentEmail_returnsUnauthorized() throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .email("nonexistent@test.com")
                .password("password123")
                .build();

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_withMissingFields_returnsBadRequest() throws Exception {
        LoginRequest loginRequest = LoginRequest.builder().build();

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_disabledUser_returnsUnauthorized() throws Exception {
        // Register admin (first user)
        String[] adminData = TestHelper.registerAdminAndGetTokenAndId(mockMvc);
        String adminToken = adminData[0];

        // Register guest (second user)
        RegisterRequest guestReq = RegisterRequest.builder()
                .email("disabled@test.com")
                .password("password123")
                .firstName("Disabled")
                .lastName("User")
                .build();
        MvcResult guestResult = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(guestReq)))
                .andExpect(status().isCreated())
                .andReturn();
        String guestId = TestHelper.extractAuthUserId(guestResult);

        // Admin disables the guest
        UpdateUserRequest disableReq = UpdateUserRequest.builder()
                .enabled(false)
                .build();
        mockMvc.perform(patch("/users/{id}", guestId)
                        .header("Authorization", TestHelper.bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(disableReq)))
                .andExpect(status().isOk());

        // Disabled user tries to login
        LoginRequest loginReq = LoginRequest.builder()
                .email("disabled@test.com")
                .password("password123")
                .build();
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void register_withMalformedJson_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{bad json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Malformed JSON")));
    }

    @Test
    void register_withBlankFirstName_returnsBadRequest() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email("blank-first@test.com")
                .password("password123")
                .firstName("")
                .lastName("User")
                .build();

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.firstName").exists());
    }

    @Test
    void register_withBlankLastName_returnsBadRequest() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email("blank-last@test.com")
                .password("password123")
                .firstName("Test")
                .lastName("")
                .build();

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.lastName").exists());
    }

    @Test
    void login_withInvalidEmailFormat_returnsBadRequest() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("not-an-email")
                .password("password123")
                .build();

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists());
    }

    @Test
    void register_withExactly8CharPassword_succeeds() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email("exact8@test.com")
                .password("12345678")
                .firstName("Exact")
                .lastName("Eight")
                .build();

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Helpers
    // ═══════════════════════════════════════════════════════════════════

    private void registerUser(String email, String password,
                              String firstName, String lastName) throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email(email)
                .password(password)
                .firstName(firstName)
                .lastName(lastName)
                .build();

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }
}
