package com.github._manan.featureflags.integration.flagvalues;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github._manan.featureflags.config.TestConfig;
import com.github._manan.featureflags.dto.FlagValueDto;
import com.github._manan.featureflags.dto.FlagValueVariantDto;
import com.github._manan.featureflags.entity.FlagType;
import com.github._manan.featureflags.support.TestHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestConfig.class)
@Transactional
class FlagValueIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String adminToken;

    // Reusable fixtures created in @BeforeEach
    private String boolFlagId;
    private String stringFlagId;
    private String numberFlagId;
    private String envId;
    private String env2Id;

    @BeforeEach
    void setUp() throws Exception {
        adminToken = TestHelper.registerAdminAndGetToken(mockMvc);
        boolFlagId = TestHelper.createFlag(mockMvc, adminToken,
                "bool-flag", "Boolean Flag", FlagType.BOOLEAN, "true");
        stringFlagId = TestHelper.createFlag(mockMvc, adminToken,
                "string-flag", "String Flag", FlagType.STRING, "default");
        numberFlagId = TestHelper.createFlag(mockMvc, adminToken,
                "number-flag", "Number Flag", FlagType.NUMBER, "42");
        envId = TestHelper.createEnvironment(mockMvc, adminToken,
                "test-env", "Test Environment", null);
        env2Id = TestHelper.createEnvironment(mockMvc, adminToken,
                "test-env-2", "Test Environment 2", null);
    }

    // ═══════════════════════════════════════════════════════════════════
    //  POST /flag-values  — create
    // ═══════════════════════════════════════════════════════════════════

    @Test
    void createFlagValue_stringFlag_singleVariant_returnsCreated() throws Exception {
        FlagValueDto request = buildFlagValueRequest(stringFlagId, envId,
                List.of(variant("hello", 100)));

        mockMvc.perform(post("/flag-values")
                        .header("Authorization", TestHelper.bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.flagId").value(stringFlagId))
                .andExpect(jsonPath("$.flagKey").value("string-flag"))
                .andExpect(jsonPath("$.environmentId").value(envId))
                .andExpect(jsonPath("$.environmentKey").value("test-env"))
                .andExpect(jsonPath("$.isActive").value(true))
                .andExpect(jsonPath("$.variants", hasSize(1)))
                .andExpect(jsonPath("$.variants[0].value").value("hello"))
                .andExpect(jsonPath("$.variants[0].percentage").value(100));
    }

    @Test
    void createFlagValue_booleanFlag_twoVariants_returnsCreated() throws Exception {
        FlagValueDto request = buildFlagValueRequest(boolFlagId, envId,
                List.of(variant("true", 60), variant("false", 40)));

        mockMvc.perform(post("/flag-values")
                        .header("Authorization", TestHelper.bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.variants", hasSize(2)))
                .andExpect(jsonPath("$.variants[0].value").value("true"))
                .andExpect(jsonPath("$.variants[0].percentage").value(60))
                .andExpect(jsonPath("$.variants[1].value").value("false"))
                .andExpect(jsonPath("$.variants[1].percentage").value(40));
    }

    @Test
    void createFlagValue_numberFlag_twoVariants_returnsCreated() throws Exception {
        FlagValueDto request = buildFlagValueRequest(numberFlagId, envId,
                List.of(variant("10", 50), variant("20", 50)));

        mockMvc.perform(post("/flag-values")
                        .header("Authorization", TestHelper.bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.variants", hasSize(2)));
    }

    @Test
    void createFlagValue_duplicateFlagEnvironmentCombo_returnsBadRequest() throws Exception {
        FlagValueDto request = buildFlagValueRequest(stringFlagId, envId,
                List.of(variant("val", 100)));

        mockMvc.perform(post("/flag-values")
                        .header("Authorization", TestHelper.bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Same flag + environment → error
        mockMvc.perform(post("/flag-values")
                        .header("Authorization", TestHelper.bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("already exists")));
    }

    @Test
    void createFlagValue_nullFlagId_returnsBadRequest() throws Exception {
        FlagValueDto request = FlagValueDto.builder()
                .environmentId(UUID.fromString(envId))
                .variants(List.of(variant("val", 100)))
                .build();

        mockMvc.perform(post("/flag-values")
                        .header("Authorization", TestHelper.bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.flagId").exists());
    }

    @Test
    void createFlagValue_nullEnvironmentId_returnsBadRequest() throws Exception {
        FlagValueDto request = FlagValueDto.builder()
                .flagId(UUID.fromString(stringFlagId))
                .variants(List.of(variant("val", 100)))
                .build();

        mockMvc.perform(post("/flag-values")
                        .header("Authorization", TestHelper.bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.environmentId").exists());
    }

    @Test
    void createFlagValue_nullVariants_returnsBadRequest() throws Exception {
        FlagValueDto request = FlagValueDto.builder()
                .flagId(UUID.fromString(stringFlagId))
                .environmentId(UUID.fromString(envId))
                .build();

        mockMvc.perform(post("/flag-values")
                        .header("Authorization", TestHelper.bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.variants").exists());
    }

    @Test
    void createFlagValue_emptyVariantsList_returnsBadRequest() throws Exception {
        FlagValueDto request = buildFlagValueRequest(stringFlagId, envId, List.of());

        mockMvc.perform(post("/flag-values")
                        .header("Authorization", TestHelper.bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createFlagValue_percentageSumNot100_undershoot_returnsBadRequest() throws Exception {
        FlagValueDto request = buildFlagValueRequest(stringFlagId, envId,
                List.of(variant("a", 50), variant("b", 40)));

        mockMvc.perform(post("/flag-values")
                        .header("Authorization", TestHelper.bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createFlagValue_percentageSumNot100_overshoot_returnsBadRequest() throws Exception {
        FlagValueDto request = buildFlagValueRequest(stringFlagId, envId,
                List.of(variant("a", 60), variant("b", 50)));

        mockMvc.perform(post("/flag-values")
                        .header("Authorization", TestHelper.bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createFlagValue_variantWithBlankValue_returnsBadRequest() throws Exception {
        FlagValueDto request = buildFlagValueRequest(stringFlagId, envId,
                List.of(FlagValueVariantDto.builder().value("").percentage(100).build()));

        mockMvc.perform(post("/flag-values")
                        .header("Authorization", TestHelper.bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createFlagValue_variantWithNullPercentage_returnsBadRequest() throws Exception {
        FlagValueDto request = buildFlagValueRequest(stringFlagId, envId,
                List.of(FlagValueVariantDto.builder().value("val").build()));

        mockMvc.perform(post("/flag-values")
                        .header("Authorization", TestHelper.bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createFlagValue_variantWithNegativePercentage_returnsBadRequest() throws Exception {
        FlagValueDto request = buildFlagValueRequest(stringFlagId, envId,
                List.of(variant("val", -1)));

        mockMvc.perform(post("/flag-values")
                        .header("Authorization", TestHelper.bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createFlagValue_variantWithPercentageOver100_returnsBadRequest() throws Exception {
        FlagValueDto request = buildFlagValueRequest(stringFlagId, envId,
                List.of(variant("val", 101)));

        mockMvc.perform(post("/flag-values")
                        .header("Authorization", TestHelper.bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createFlagValue_booleanFlagInvalidVariantValue_returnsBadRequest() throws Exception {
        FlagValueDto request = buildFlagValueRequest(boolFlagId, envId,
                List.of(variant("yes", 100)));

        mockMvc.perform(post("/flag-values")
                        .header("Authorization", TestHelper.bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("invalid BOOLEAN value")));
    }

    @Test
    void createFlagValue_numberFlagInvalidVariantValue_returnsBadRequest() throws Exception {
        FlagValueDto request = buildFlagValueRequest(numberFlagId, envId,
                List.of(variant("abc", 100)));

        mockMvc.perform(post("/flag-values")
                        .header("Authorization", TestHelper.bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("invalid NUMBER value")));
    }

    @Test
    void createFlagValue_nonExistentFlagId_returnsNotFound() throws Exception {
        FlagValueDto request = buildFlagValueRequest(
                "00000000-0000-0000-0000-000000000000", envId,
                List.of(variant("val", 100)));

        mockMvc.perform(post("/flag-values")
                        .header("Authorization", TestHelper.bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void createFlagValue_nonExistentEnvironmentId_returnsNotFound() throws Exception {
        FlagValueDto request = buildFlagValueRequest(
                stringFlagId, "00000000-0000-0000-0000-000000000000",
                List.of(variant("val", 100)));

        mockMvc.perform(post("/flag-values")
                        .header("Authorization", TestHelper.bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    // ═══════════════════════════════════════════════════════════════════
    //  GET /flag-values  — list with filters
    // ═══════════════════════════════════════════════════════════════════

    @Test
    void getAllFlagValues_returnsAll() throws Exception {
        createTestFlagValue(stringFlagId, envId, List.of(variant("a", 100)));
        createTestFlagValue(boolFlagId, envId, List.of(variant("true", 100)));

        mockMvc.perform(get("/flag-values")
                        .header("Authorization", TestHelper.bearerToken(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void getAllFlagValues_filterByFlagId_returnsOnlyMatchingFlag() throws Exception {
        createTestFlagValue(stringFlagId, envId, List.of(variant("a", 100)));
        createTestFlagValue(boolFlagId, envId, List.of(variant("true", 100)));

        mockMvc.perform(get("/flag-values")
                        .param("flagId", stringFlagId)
                        .header("Authorization", TestHelper.bearerToken(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].flagKey").value("string-flag"));
    }

    @Test
    void getAllFlagValues_filterByEnvironmentId_returnsOnlyMatchingEnv() throws Exception {
        createTestFlagValue(stringFlagId, envId, List.of(variant("a", 100)));
        createTestFlagValue(stringFlagId, env2Id, List.of(variant("b", 100)));

        mockMvc.perform(get("/flag-values")
                        .param("environmentId", envId)
                        .header("Authorization", TestHelper.bearerToken(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].environmentKey").value("test-env"));
    }

    @Test
    void getAllFlagValues_filterByBothFlagAndEnv_returnsExactMatch() throws Exception {
        createTestFlagValue(stringFlagId, envId, List.of(variant("a", 100)));
        createTestFlagValue(stringFlagId, env2Id, List.of(variant("b", 100)));
        createTestFlagValue(boolFlagId, envId, List.of(variant("true", 100)));

        mockMvc.perform(get("/flag-values")
                        .param("flagId", stringFlagId)
                        .param("environmentId", envId)
                        .header("Authorization", TestHelper.bearerToken(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].flagKey").value("string-flag"))
                .andExpect(jsonPath("$[0].environmentKey").value("test-env"));
    }

    @Test
    void getAllFlagValues_emptyDb_returnsEmptyList() throws Exception {
        mockMvc.perform(get("/flag-values")
                        .header("Authorization", TestHelper.bearerToken(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // ═══════════════════════════════════════════════════════════════════
    //  GET /flag-values/{id}  — get by id
    // ═══════════════════════════════════════════════════════════════════

    @Test
    void getFlagValueById_existing_returnsFullDto() throws Exception {
        String fvId = createTestFlagValue(boolFlagId, envId,
                List.of(variant("true", 70), variant("false", 30)));

        mockMvc.perform(get("/flag-values/{id}", fvId)
                        .header("Authorization", TestHelper.bearerToken(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(fvId))
                .andExpect(jsonPath("$.flagId").value(boolFlagId))
                .andExpect(jsonPath("$.flagKey").value("bool-flag"))
                .andExpect(jsonPath("$.flagType").value("BOOLEAN"))
                .andExpect(jsonPath("$.environmentId").value(envId))
                .andExpect(jsonPath("$.environmentKey").value("test-env"))
                .andExpect(jsonPath("$.isActive").value(true))
                .andExpect(jsonPath("$.variants", hasSize(2)));
    }

    @Test
    void getFlagValueById_nonExistent_returnsNotFound() throws Exception {
        mockMvc.perform(get("/flag-values/{id}", "00000000-0000-0000-0000-000000000000")
                        .header("Authorization", TestHelper.bearerToken(adminToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getFlagValueById_invalidUuid_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/flag-values/{id}", "not-a-uuid")
                        .header("Authorization", TestHelper.bearerToken(adminToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Invalid value")));
    }

    // ═══════════════════════════════════════════════════════════════════
    //  PUT /flag-values/{id}  — update (full variant replacement)
    // ═══════════════════════════════════════════════════════════════════

    @Test
    void updateFlagValue_replaceVariants_returnsUpdated() throws Exception {
        String fvId = createTestFlagValue(stringFlagId, envId,
                List.of(variant("old", 100)));

        FlagValueDto updateRequest = buildFlagValueRequest(stringFlagId, envId,
                List.of(variant("new-a", 50), variant("new-b", 50)));

        mockMvc.perform(put("/flag-values/{id}", fvId)
                        .header("Authorization", TestHelper.bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.variants", hasSize(2)))
                .andExpect(jsonPath("$.variants[0].value").value("new-a"))
                .andExpect(jsonPath("$.variants[1].value").value("new-b"));
    }

    @Test
    void updateFlagValue_changeFlagId_returnsBadRequest() throws Exception {
        String fvId = createTestFlagValue(stringFlagId, envId,
                List.of(variant("val", 100)));

        // Attempt to change flagId to boolFlagId
        FlagValueDto updateRequest = buildFlagValueRequest(boolFlagId, envId,
                List.of(variant("true", 100)));

        mockMvc.perform(put("/flag-values/{id}", fvId)
                        .header("Authorization", TestHelper.bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Flag ID cannot be changed"));
    }

    @Test
    void updateFlagValue_booleanFlagInvalidVariant_returnsBadRequest() throws Exception {
        String fvId = createTestFlagValue(boolFlagId, envId,
                List.of(variant("true", 100)));

        FlagValueDto updateRequest = buildFlagValueRequest(boolFlagId, envId,
                List.of(variant("maybe", 100)));

        mockMvc.perform(put("/flag-values/{id}", fvId)
                        .header("Authorization", TestHelper.bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("invalid BOOLEAN value")));
    }

    @Test
    void updateFlagValue_invalidPercentageDistribution_returnsBadRequest() throws Exception {
        String fvId = createTestFlagValue(stringFlagId, envId,
                List.of(variant("val", 100)));

        FlagValueDto updateRequest = buildFlagValueRequest(stringFlagId, envId,
                List.of(variant("a", 30), variant("b", 30)));

        mockMvc.perform(put("/flag-values/{id}", fvId)
                        .header("Authorization", TestHelper.bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateFlagValue_nonExistentId_returnsNotFound() throws Exception {
        FlagValueDto updateRequest = buildFlagValueRequest(stringFlagId, envId,
                List.of(variant("val", 100)));

        mockMvc.perform(put("/flag-values/{id}", "00000000-0000-0000-0000-000000000000")
                        .header("Authorization", TestHelper.bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateFlagValue_numberFlagInvalidVariant_returnsBadRequest() throws Exception {
        String fvId = createTestFlagValue(numberFlagId, envId,
                List.of(variant("10", 100)));

        FlagValueDto updateRequest = buildFlagValueRequest(numberFlagId, envId,
                List.of(variant("not-a-number", 100)));

        mockMvc.perform(put("/flag-values/{id}", fvId)
                        .header("Authorization", TestHelper.bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("invalid NUMBER value")));
    }

    // ═══════════════════════════════════════════════════════════════════
    //  DELETE /flag-values/{id}  — soft-delete
    // ═══════════════════════════════════════════════════════════════════

    @Test
    void deleteFlagValue_existing_returnsNoContent() throws Exception {
        String fvId = createTestFlagValue(stringFlagId, envId,
                List.of(variant("val", 100)));

        mockMvc.perform(delete("/flag-values/{id}", fvId)
                        .header("Authorization", TestHelper.bearerToken(adminToken)))
                .andExpect(status().isNoContent());

        // Verify soft-deleted
        mockMvc.perform(get("/flag-values/{id}", fvId)
                        .header("Authorization", TestHelper.bearerToken(adminToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteFlagValue_nonExistent_returnsNotFound() throws Exception {
        mockMvc.perform(delete("/flag-values/{id}", "00000000-0000-0000-0000-000000000000")
                        .header("Authorization", TestHelper.bearerToken(adminToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteFlagValue_disappearsFromList() throws Exception {
        String fvId = createTestFlagValue(stringFlagId, envId,
                List.of(variant("a", 100)));
        createTestFlagValue(boolFlagId, envId, List.of(variant("true", 100)));

        mockMvc.perform(delete("/flag-values/{id}", fvId)
                        .header("Authorization", TestHelper.bearerToken(adminToken)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/flag-values")
                        .header("Authorization", TestHelper.bearerToken(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].flagKey").value("bool-flag"));
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Auth / Authorization
    // ═══════════════════════════════════════════════════════════════════

    @Test
    void flagValueEndpoints_withoutAuth_returnsForbidden() throws Exception {
        mockMvc.perform(get("/flag-values"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/flag-values")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void flagValueEndpoints_withGuestRole_returnsForbidden() throws Exception {
        // Register guest (second user)
        String guestToken = TestHelper.registerAndGetToken(
                mockMvc, "guest@test.com", "password123", "Guest", "User");

        mockMvc.perform(get("/flag-values")
                        .header("Authorization", TestHelper.bearerToken(guestToken)))
                .andExpect(status().isForbidden());

        FlagValueDto request = buildFlagValueRequest(stringFlagId, envId,
                List.of(variant("val", 100)));

        mockMvc.perform(post("/flag-values")
                        .header("Authorization", TestHelper.bearerToken(guestToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Cross-module: flag deletion cascades to flag values
    // ═══════════════════════════════════════════════════════════════════

    @Test
    void deletingFlag_cascadesDeactivationOfFlagValues() throws Exception {
        String fvId = createTestFlagValue(stringFlagId, envId,
                List.of(variant("val", 100)));

        // Delete the flag
        mockMvc.perform(delete("/flags/{id}", stringFlagId)
                        .header("Authorization", TestHelper.bearerToken(adminToken)))
                .andExpect(status().isNoContent());

        // Flag value should be soft-deleted too
        mockMvc.perform(get("/flag-values/{id}", fvId)
                        .header("Authorization", TestHelper.bearerToken(adminToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void createFlagValue_threeVariants_validPercentages_returnsCreated() throws Exception {
        FlagValueDto request = buildFlagValueRequest(stringFlagId, envId,
                List.of(variant("a", 33), variant("b", 33), variant("c", 34)));

        mockMvc.perform(post("/flag-values")
                        .header("Authorization", TestHelper.bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.variants", hasSize(3)));
    }

    @Test
    void createFlagValue_variantWithZeroPercentage_validTotal_returnsCreated() throws Exception {
        FlagValueDto request = buildFlagValueRequest(stringFlagId, envId,
                List.of(variant("enabled", 100), variant("disabled", 0)));

        mockMvc.perform(post("/flag-values")
                        .header("Authorization", TestHelper.bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.variants", hasSize(2)));
    }

    @Test
    void createFlagValue_malformedJson_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/flag-values")
                        .header("Authorization", TestHelper.bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{bad"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Malformed JSON")));
    }

    @Test
    void getAllFlagValues_searchByFlagKey_returnsFiltered() throws Exception {
        createTestFlagValue(stringFlagId, envId, List.of(variant("a", 100)));
        createTestFlagValue(boolFlagId, env2Id, List.of(variant("true", 100)));

        // Search by flag key — "string" should match "string-flag"
        mockMvc.perform(get("/flag-values")
                        .param("search", "string")
                        .header("Authorization", TestHelper.bearerToken(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].flagKey").value("string-flag"));
    }

    @Test
    void getAllFlagValues_searchByEnvironmentName_returnsFiltered() throws Exception {
        createTestFlagValue(stringFlagId, envId, List.of(variant("a", 100)));
        createTestFlagValue(boolFlagId, env2Id, List.of(variant("true", 100)));

        // Search by environment name — "Environment 2" should match env2 only
        mockMvc.perform(get("/flag-values")
                        .param("search", "Environment 2")
                        .header("Authorization", TestHelper.bearerToken(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].environmentKey").value("test-env-2"));
    }

    @Test
    void getAllFlagValues_searchCaseInsensitive_returnsMatch() throws Exception {
        createTestFlagValue(stringFlagId, envId, List.of(variant("a", 100)));

        mockMvc.perform(get("/flag-values")
                        .param("search", "STRING")
                        .header("Authorization", TestHelper.bearerToken(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void getAllFlagValues_searchNoMatch_returnsEmptyList() throws Exception {
        createTestFlagValue(stringFlagId, envId, List.of(variant("a", 100)));

        mockMvc.perform(get("/flag-values")
                        .param("search", "nonexistent-xyz")
                        .header("Authorization", TestHelper.bearerToken(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void createFlagValue_withDeactivatedFlag_returnsNotFound() throws Exception {
        // Delete (deactivate) the bool flag
        mockMvc.perform(delete("/flags/{id}", boolFlagId)
                        .header("Authorization", TestHelper.bearerToken(adminToken)))
                .andExpect(status().isNoContent());

        // Try creating flag value for deactivated flag
        FlagValueDto request = buildFlagValueRequest(boolFlagId, envId,
                List.of(variant("true", 100)));

        mockMvc.perform(post("/flag-values")
                        .header("Authorization", TestHelper.bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void createFlagValue_withDeactivatedEnvironment_returnsNotFound() throws Exception {
        // Delete (deactivate) the environment
        mockMvc.perform(delete("/environments/{id}", envId)
                        .header("Authorization", TestHelper.bearerToken(adminToken)))
                .andExpect(status().isNoContent());

        // Try creating flag value for deactivated environment
        FlagValueDto request = buildFlagValueRequest(stringFlagId, envId,
                List.of(variant("val", 100)));

        mockMvc.perform(post("/flag-values")
                        .header("Authorization", TestHelper.bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void createFlagValue_afterSoftDelete_sameComboSucceeds() throws Exception {
        // Create flag value
        String fvId = createTestFlagValue(stringFlagId, envId, List.of(variant("a", 100)));

        // Soft-delete it
        mockMvc.perform(delete("/flag-values/{id}", fvId)
                        .header("Authorization", TestHelper.bearerToken(adminToken)))
                .andExpect(status().isNoContent());

        // Re-create same flag+env combo — should succeed
        FlagValueDto request = buildFlagValueRequest(stringFlagId, envId,
                List.of(variant("b", 100)));

        mockMvc.perform(post("/flag-values")
                        .header("Authorization", TestHelper.bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.variants[0].value").value("b"));
    }

    @Test
    void updateFlagValue_changeEnvironmentId_isIgnored() throws Exception {
        String fvId = createTestFlagValue(stringFlagId, envId, List.of(variant("a", 100)));

        // Try updating with different environmentId
        FlagValueDto updateRequest = buildFlagValueRequest(stringFlagId, env2Id,
                List.of(variant("updated", 100)));

        mockMvc.perform(put("/flag-values/{id}", fvId)
                        .header("Authorization", TestHelper.bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                // Environment should remain the original one (env2 is ignored)
                .andExpect(jsonPath("$.environmentId").value(envId))
                .andExpect(jsonPath("$.variants[0].value").value("updated"));
    }

    @Test
    void updateFlagValue_invalidUuid_returnsBadRequest() throws Exception {
        FlagValueDto request = buildFlagValueRequest(stringFlagId, envId,
                List.of(variant("val", 100)));

        mockMvc.perform(put("/flag-values/{id}", "not-a-uuid")
                        .header("Authorization", TestHelper.bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Invalid value")));
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Private helpers
    // ═══════════════════════════════════════════════════════════════════

    private String createTestFlagValue(String flagId, String environmentId,
                                       List<FlagValueVariantDto> variants) throws Exception {
        return TestHelper.createFlagValue(mockMvc, adminToken, flagId, environmentId, variants);
    }

    private FlagValueDto buildFlagValueRequest(String flagId, String environmentId,
                                               List<FlagValueVariantDto> variants) {
        return FlagValueDto.builder()
                .flagId(UUID.fromString(flagId))
                .environmentId(UUID.fromString(environmentId))
                .variants(variants)
                .build();
    }

    private FlagValueVariantDto variant(String value, int percentage) {
        return FlagValueVariantDto.builder()
                .value(value)
                .percentage(percentage)
                .build();
    }
}
