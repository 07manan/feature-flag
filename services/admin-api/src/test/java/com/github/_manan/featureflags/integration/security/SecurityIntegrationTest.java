package com.github._manan.featureflags.integration.security;

import com.github._manan.featureflags.config.TestConfig;
import com.github._manan.featureflags.support.TestHelper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestConfig.class)
@Transactional
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Value("${jwt.secret}")
    private String jwtSecret;

    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        adminToken = TestHelper.registerAdminAndGetToken(mockMvc);
    }

    @Test
    void expiredJwtToken_protectedEndpoint_returnsForbidden() throws Exception {
        // Create an expired JWT token
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
        String expiredToken = Jwts.builder()
                .subject("admin@test.com")
                .claims(Map.of("roles", java.util.List.of(Map.of("authority", "ROLE_ADMIN"))))
                .issuedAt(new Date(System.currentTimeMillis() - 200000))
                .expiration(new Date(System.currentTimeMillis() - 100000)) // expired 100s ago
                .signWith(key)
                .compact();

        mockMvc.perform(get("/flags")
                        .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void malformedJwtToken_protectedEndpoint_returnsForbidden() throws Exception {
        mockMvc.perform(get("/flags")
                        .header("Authorization", "Bearer garbage.not.a.jwt.token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void tamperedJwtToken_protectedEndpoint_returnsForbidden() throws Exception {
        // Take a valid token and tamper with it
        String[] parts = adminToken.split("\\.");
        // Modify the payload (middle part)
        String tamperedPayload = parts[1] + "TAMPERED";
        String tamperedToken = parts[0] + "." + tamperedPayload + "." + parts[2];

        mockMvc.perform(get("/flags")
                        .header("Authorization", "Bearer " + tamperedToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void missingBearerPrefix_protectedEndpoint_returnsForbidden() throws Exception {
        // Send Authorization header without "Bearer " prefix
        mockMvc.perform(get("/flags")
                        .header("Authorization", adminToken))
                .andExpect(status().isForbidden());
    }

    // ═══════════════════════════════════════════════════════════════════
    //  HTTP method not allowed
    // ═══════════════════════════════════════════════════════════════════

    @Test
    void methodNotAllowed_putOnRegister_returns405() throws Exception {
        mockMvc.perform(put("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.message", containsString("not supported")));
    }

    @Test
    void methodNotAllowed_deleteOnLogin_returns405() throws Exception {
        mockMvc.perform(delete("/auth/login"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.message", containsString("not supported")));
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Public endpoints
    // ═══════════════════════════════════════════════════════════════════

    @Test
    void actuatorHealth_withoutAuth_returns200() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Error response structure
    // ═══════════════════════════════════════════════════════════════════

    @Test
    void errorResponseStructure_containsAllFields() throws Exception {
        // Trigger a 404 error and verify the response contains all expected fields
        mockMvc.perform(get("/flags/{id}", "00000000-0000-0000-0000-000000000000")
                        .header("Authorization", TestHelper.bearerToken(adminToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }
}
