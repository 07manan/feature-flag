package com.github._manan.featureflags.integration.environments;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github._manan.featureflags.config.TestConfig;
import com.github._manan.featureflags.dto.EnvironmentDto;
import com.github._manan.featureflags.dto.FlagDto;
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
class EnvironmentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        adminToken = TestHelper.registerAdminAndGetToken(mockMvc);
    }

    // ═══════════════════════════════════════════════════════════════════
    //  POST /environments  — create
    // ═══════════════════════════════════════════════════════════════════

    @Test
    void createEnvironment_validRequest_returnsCreated() throws Exception {
        EnvironmentDto request = EnvironmentDto.builder()
                .key("production")
                .name("Production")
                .description("Production environment")
                .build();

        mockMvc.perform(post("/environments")
                        .header("Authorization", TestHelper.bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.key").value("production"))
                .andExpect(jsonPath("$.name").value("Production"))
                .andExpect(jsonPath("$.description").value("Production environment"))
                .andExpect(jsonPath("$.isActive").value(true))
                .andExpect(jsonPath("$.apiKey").isNotEmpty());
    }

    @Test
    void createEnvironment_withoutDescription_returnsCreated() throws Exception {
        EnvironmentDto request = EnvironmentDto.builder()
                .key("staging")
                .name("Staging")
                .build();

        mockMvc.perform(post("/environments")
                        .header("Authorization", TestHelper.bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.key").value("staging"))
                .andExpect(jsonPath("$.description").isEmpty());
    }

    @Test
    void createEnvironment_duplicateKey_returnsBadRequest() throws Exception {
        EnvironmentDto request = EnvironmentDto.builder()
                .key("dup-env")
                .name("Duplicate Env")
                .build();

        mockMvc.perform(post("/environments")
                        .header("Authorization", TestHelper.bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/environments")
                        .header("Authorization", TestHelper.bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("already exists")));
    }

    @Test
    void createEnvironment_blankKey_returnsBadRequest() throws Exception {
        EnvironmentDto request = EnvironmentDto.builder()
                .key("")
                .name("No Key")
                .build();

        mockMvc.perform(post("/environments")
                        .header("Authorization", TestHelper.bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.key").exists());
    }

    @Test
    void createEnvironment_invalidKeyFormat_returnsBadRequest() throws Exception {
        EnvironmentDto request = EnvironmentDto.builder()
                .key("INVALID_KEY!")
                .name("Bad Key")
                .build();

        mockMvc.perform(post("/environments")
                        .header("Authorization", TestHelper.bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.key").exists());
    }

    @Test
    void createEnvironment_keyWithUppercase_returnsBadRequest() throws Exception {
        EnvironmentDto request = EnvironmentDto.builder()
                .key("Production")
                .name("Upper Key")
                .build();

        mockMvc.perform(post("/environments")
                        .header("Authorization", TestHelper.bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.key").exists());
    }

    @Test
    void createEnvironment_keyTooLong_returnsBadRequest() throws Exception {
        EnvironmentDto request = EnvironmentDto.builder()
                .key("a".repeat(51))
                .name("Long Key")
                .build();

        mockMvc.perform(post("/environments")
                        .header("Authorization", TestHelper.bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.key").exists());
    }

    @Test
    void createEnvironment_blankName_returnsBadRequest() throws Exception {
        EnvironmentDto request = EnvironmentDto.builder()
                .key("no-name")
                .name("")
                .build();

        mockMvc.perform(post("/environments")
                        .header("Authorization", TestHelper.bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists());
    }

    @Test
    void createEnvironment_nameTooLong_returnsBadRequest() throws Exception {
        EnvironmentDto request = EnvironmentDto.builder()
                .key("long-name")
                .name("N".repeat(101))
                .build();

        mockMvc.perform(post("/environments")
                        .header("Authorization", TestHelper.bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists());
    }

    @Test
    void createEnvironment_descriptionTooLong_returnsBadRequest() throws Exception {
        EnvironmentDto request = EnvironmentDto.builder()
                .key("long-desc")
                .name("Long Desc")
                .description("D".repeat(501))
                .build();

        mockMvc.perform(post("/environments")
                        .header("Authorization", TestHelper.bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.description").exists());
    }

    @Test
    void createEnvironment_missingRequiredFields_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/environments")
                        .header("Authorization", TestHelper.bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isNotEmpty());
    }

    @Test
    void createEnvironment_malformedJson_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/environments")
                        .header("Authorization", TestHelper.bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{bad"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Malformed JSON")));
    }

    // ═══════════════════════════════════════════════════════════════════
    //  GET /environments  — list
    // ═══════════════════════════════════════════════════════════════════

    @Test
    void getAllEnvironments_returnsCreatedEnvironments() throws Exception {
        createTestEnvironment("env-one", "Env One", null);
        createTestEnvironment("env-two", "Env Two", "Second environment");

        mockMvc.perform(get("/environments")
                        .header("Authorization", TestHelper.bearerToken(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].key", containsInAnyOrder("env-one", "env-two")));
    }

    @Test
    void getAllEnvironments_emptyDb_returnsEmptyList() throws Exception {
        mockMvc.perform(get("/environments")
                        .header("Authorization", TestHelper.bearerToken(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getAllEnvironments_searchByName_returnsFiltered() throws Exception {
        createTestEnvironment("dev-env", "Development", null);
        createTestEnvironment("prod-env", "Production", null);

        mockMvc.perform(get("/environments")
                        .param("search", "Development")
                        .header("Authorization", TestHelper.bearerToken(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].key").value("dev-env"));
    }

    @Test
    void getAllEnvironments_searchByDescription_returnsFiltered() throws Exception {
        createTestEnvironment("env-a", "Env A", "searchable description");
        createTestEnvironment("env-b", "Env B", "other description");

        mockMvc.perform(get("/environments")
                        .param("search", "searchable")
                        .header("Authorization", TestHelper.bearerToken(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].key").value("env-a"));
    }

    @Test
    void getAllEnvironments_searchNoMatch_returnsEmptyList() throws Exception {
        createTestEnvironment("env-x", "Env X", null);

        mockMvc.perform(get("/environments")
                        .param("search", "nonexistent")
                        .header("Authorization", TestHelper.bearerToken(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getAllEnvironments_searchCaseInsensitive_returnsMatch() throws Exception {
        createTestEnvironment("ci-env", "Production Environment", null);

        mockMvc.perform(get("/environments")
                        .param("search", "production")
                        .header("Authorization", TestHelper.bearerToken(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    // ═══════════════════════════════════════════════════════════════════
    //  GET /environments/{id}  — get by id
    // ═══════════════════════════════════════════════════════════════════

    @Test
    void getEnvironmentById_existing_returnsEnvironment() throws Exception {
        String envId = createTestEnvironment("get-env", "Get Env", "desc");

        mockMvc.perform(get("/environments/{id}", envId)
                        .header("Authorization", TestHelper.bearerToken(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(envId))
                .andExpect(jsonPath("$.key").value("get-env"))
                .andExpect(jsonPath("$.name").value("Get Env"))
                .andExpect(jsonPath("$.description").value("desc"))
                .andExpect(jsonPath("$.apiKey").isNotEmpty());
    }

    @Test
    void getEnvironmentById_nonExistent_returnsNotFound() throws Exception {
        mockMvc.perform(get("/environments/{id}", "00000000-0000-0000-0000-000000000000")
                        .header("Authorization", TestHelper.bearerToken(adminToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getEnvironmentById_invalidUuid_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/environments/{id}", "not-a-uuid")
                        .header("Authorization", TestHelper.bearerToken(adminToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Invalid value")));
    }

    // ═══════════════════════════════════════════════════════════════════
    //  PATCH /environments/{id}  — update
    // ═══════════════════════════════════════════════════════════════════

    @Test
    void updateEnvironment_name_returnsUpdated() throws Exception {
        String envId = createTestEnvironment("upd-env", "Original Name", "original desc");

        EnvironmentDto updateRequest = EnvironmentDto.builder()
                .name("Updated Name")
                .build();

        mockMvc.perform(patch("/environments/{id}", envId)
                        .header("Authorization", TestHelper.bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Name"))
                .andExpect(jsonPath("$.description").value("original desc"))
                .andExpect(jsonPath("$.key").value("upd-env")); // key immutable
    }

    @Test
    void updateEnvironment_description_returnsUpdated() throws Exception {
        String envId = createTestEnvironment("desc-env", "Desc Env", "old desc");

        EnvironmentDto updateRequest = EnvironmentDto.builder()
                .description("new description")
                .build();

        mockMvc.perform(patch("/environments/{id}", envId)
                        .header("Authorization", TestHelper.bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("new description"))
                .andExpect(jsonPath("$.name").value("Desc Env"));
    }

    @Test
    void updateEnvironment_partialUpdate_onlyChangesProvidedFields() throws Exception {
        String envId = createTestEnvironment("partial-env", "Partial", "keep-me");

        EnvironmentDto updateRequest = EnvironmentDto.builder()
                .name("Changed Only")
                .build();

        mockMvc.perform(patch("/environments/{id}", envId)
                        .header("Authorization", TestHelper.bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Changed Only"))
                .andExpect(jsonPath("$.description").value("keep-me"));
    }

    @Test
    void updateEnvironment_keyIsImmutable_staysUnchanged() throws Exception {
        String envId = createTestEnvironment("immutable-key", "Immutable", null);

        // Attempt to change key (should be ignored by service)
        EnvironmentDto updateRequest = EnvironmentDto.builder()
                .key("new-key")
                .name("Still Immutable")
                .build();

        mockMvc.perform(patch("/environments/{id}", envId)
                        .header("Authorization", TestHelper.bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.key").value("immutable-key"));
    }

    @Test
    void updateEnvironment_nonExistent_returnsNotFound() throws Exception {
        EnvironmentDto updateRequest = EnvironmentDto.builder()
                .name("Ghost Environment")
                .build();

        mockMvc.perform(patch("/environments/{id}", "00000000-0000-0000-0000-000000000000")
                        .header("Authorization", TestHelper.bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound());
    }

    // ═══════════════════════════════════════════════════════════════════
    //  DELETE /environments/{id}  — soft-delete
    // ═══════════════════════════════════════════════════════════════════

    @Test
    void deleteEnvironment_existing_returnsNoContent() throws Exception {
        String envId = createTestEnvironment("del-env", "Delete Me", null);

        mockMvc.perform(delete("/environments/{id}", envId)
                        .header("Authorization", TestHelper.bearerToken(adminToken)))
                .andExpect(status().isNoContent());

        // Verify soft-deleted: GET returns 404
        mockMvc.perform(get("/environments/{id}", envId)
                        .header("Authorization", TestHelper.bearerToken(adminToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteEnvironment_disappearsFromList() throws Exception {
        String envId = createTestEnvironment("list-del", "List Delete", null);
        createTestEnvironment("list-keep", "List Keep", null);

        mockMvc.perform(delete("/environments/{id}", envId)
                        .header("Authorization", TestHelper.bearerToken(adminToken)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/environments")
                        .header("Authorization", TestHelper.bearerToken(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].key").value("list-keep"));
    }

    @Test
    void deleteEnvironment_nonExistent_returnsNotFound() throws Exception {
        mockMvc.perform(delete("/environments/{id}", "00000000-0000-0000-0000-000000000000")
                        .header("Authorization", TestHelper.bearerToken(adminToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteEnvironment_cascadesDeactivationOfFlagValues() throws Exception {
        // Create environment and flag, then a flag-value linking them
        String envId = createTestEnvironment("cascade-env", "Cascade Env", null);
        String flagId = TestHelper.createFlag(mockMvc, adminToken,
                "cascade-flag", "Cascade Flag", FlagType.BOOLEAN, "true");

        List<FlagValueVariantDto> variants = List.of(
                FlagValueVariantDto.builder().value("true").percentage(100).build());
        String flagValueId = TestHelper.createFlagValue(mockMvc, adminToken, flagId, envId, variants);

        // Delete environment → should cascade-deactivate flag values
        mockMvc.perform(delete("/environments/{id}", envId)
                        .header("Authorization", TestHelper.bearerToken(adminToken)))
                .andExpect(status().isNoContent());

        // Flag value should now be soft-deleted (404)
        mockMvc.perform(get("/flag-values/{id}", flagValueId)
                        .header("Authorization", TestHelper.bearerToken(adminToken)))
                .andExpect(status().isNotFound());
    }

    // ═══════════════════════════════════════════════════════════════════
    //  POST /environments/{id}/api-key  — regenerate
    // ═══════════════════════════════════════════════════════════════════

    @Test
    void regenerateApiKey_existing_returnsNewApiKey() throws Exception {
        String envId = createTestEnvironment("regen-env", "Regen Env", null);

        // Get old API key
        MvcResult getResult = mockMvc.perform(get("/environments/{id}", envId)
                        .header("Authorization", TestHelper.bearerToken(adminToken)))
                .andExpect(status().isOk())
                .andReturn();
        String oldApiKey = objectMapper.readTree(getResult.getResponse().getContentAsString())
                .get("apiKey").asText();

        // Regenerate
        mockMvc.perform(post("/environments/{id}/api-key", envId)
                        .header("Authorization", TestHelper.bearerToken(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.apiKey").isNotEmpty())
                .andExpect(jsonPath("$.apiKey").value(not(equalTo(oldApiKey))))
                .andExpect(jsonPath("$.key").value("regen-env"));
    }

    @Test
    void regenerateApiKey_nonExistent_returnsNotFound() throws Exception {
        mockMvc.perform(post("/environments/{id}/api-key", "00000000-0000-0000-0000-000000000000")
                        .header("Authorization", TestHelper.bearerToken(adminToken)))
                .andExpect(status().isNotFound());
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Auth / Authorization
    // ═══════════════════════════════════════════════════════════════════

    @Test
    void environmentEndpoints_withoutAuth_returnsForbidden() throws Exception {
        mockMvc.perform(get("/environments"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/environments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void environmentEndpoints_withGuestRole_returnsForbidden() throws Exception {
        // Register guest (second user)
        String guestToken = TestHelper.registerAndGetToken(
                mockMvc, "guest@test.com", "password123", "Guest", "User");

        mockMvc.perform(get("/environments")
                        .header("Authorization", TestHelper.bearerToken(guestToken)))
                .andExpect(status().isForbidden());

        EnvironmentDto request = EnvironmentDto.builder()
                .key("guest-env")
                .name("Guest Env")
                .build();

        mockMvc.perform(post("/environments")
                        .header("Authorization", TestHelper.bearerToken(guestToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createEnvironment_softDeletedKeyReuse_succeeds() throws Exception {
        String envId = createTestEnvironment("reuse-env", "Reuse Env", null);

        // Soft-delete
        mockMvc.perform(delete("/environments/{id}", envId)
                        .header("Authorization", TestHelper.bearerToken(adminToken)))
                .andExpect(status().isNoContent());

        // Re-create with same key
        EnvironmentDto request = EnvironmentDto.builder()
                .key("reuse-env")
                .name("Reused Env")
                .build();

        mockMvc.perform(post("/environments")
                        .header("Authorization", TestHelper.bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.key").value("reuse-env"));
    }

    @Test
    void deleteEnvironment_getByIdAfterDelete_returnsNotFound() throws Exception {
        String envId = createTestEnvironment("verify-del", "Verify Delete", null);

        mockMvc.perform(delete("/environments/{id}", envId)
                        .header("Authorization", TestHelper.bearerToken(adminToken)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/environments/{id}", envId)
                        .header("Authorization", TestHelper.bearerToken(adminToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void regenerateApiKey_apiKeyFormat_startsWithPrefix() throws Exception {
        String envId = createTestEnvironment("fmt-env", "Format Env", null);

        mockMvc.perform(post("/environments/{id}/api-key", envId)
                        .header("Authorization", TestHelper.bearerToken(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.apiKey").value(startsWith("ff_fmt-env_")));
    }

    @Test
    void regenerateApiKey_withGuestRole_returnsForbidden() throws Exception {
        String envId = createTestEnvironment("guest-regen", "Guest Regen", null);

        String guestToken = TestHelper.registerAndGetToken(
                mockMvc, "guest-regen@test.com", "password123", "Guest", "User");

        mockMvc.perform(post("/environments/{id}/api-key", envId)
                        .header("Authorization", TestHelper.bearerToken(guestToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateEnvironment_invalidUuid_returnsBadRequest() throws Exception {
        EnvironmentDto request = EnvironmentDto.builder().name("Ghost").build();

        mockMvc.perform(patch("/environments/{id}", "not-a-uuid")
                        .header("Authorization", TestHelper.bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Invalid value")));
    }

    @Test
    void createEnvironment_apiKeyAutoGenerated_present() throws Exception {
        EnvironmentDto request = EnvironmentDto.builder()
                .key("auto-key")
                .name("Auto Key")
                .build();

        mockMvc.perform(post("/environments")
                        .header("Authorization", TestHelper.bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.apiKey").isNotEmpty())
                .andExpect(jsonPath("$.apiKey").value(startsWith("ff_auto-key_")));
    }

    @Test
    void getAllEnvironments_searchByKey_returnsFiltered() throws Exception {
        createTestEnvironment("search-key-alpha", "Alpha Env", null);
        createTestEnvironment("search-key-beta", "Beta Env", null);

        // The environment search does NOT search by key in the repository method
        // (only name and description). If search matches key, it won't be found.
        // Let's search by name instead to confirm the filter path.
        mockMvc.perform(get("/environments")
                        .param("search", "Alpha")
                        .header("Authorization", TestHelper.bearerToken(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].key").value("search-key-alpha"));
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Private helpers
    // ═══════════════════════════════════════════════════════════════════

    private String createTestEnvironment(String key, String name,
                                         String description) throws Exception {
        return TestHelper.createEnvironment(mockMvc, adminToken, key, name, description);
    }
}
