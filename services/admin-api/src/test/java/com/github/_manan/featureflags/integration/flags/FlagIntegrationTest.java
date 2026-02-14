package com.github._manan.featureflags.integration.flags;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github._manan.featureflags.config.TestConfig;
import com.github._manan.featureflags.dto.FlagDto;
import com.github._manan.featureflags.dto.FlagValueVariantDto;
import com.github._manan.featureflags.entity.FlagType;
import com.github._manan.featureflags.support.TestHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestConfig.class)
@Transactional
class FlagIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        adminToken = TestHelper.registerAdminAndGetToken(mockMvc);
    }

    @Test
    void createFlag_withValidRequest_returnsCreated() throws Exception {
        FlagDto request = FlagDto.builder()
                .key("test-flag")
                .name("Test Flag")
                .description("A test flag")
                .type(FlagType.BOOLEAN)
                .defaultValue("true")
                .build();

        mockMvc.perform(post("/flags")
                        .header("Authorization", TestHelper.bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.key").value("test-flag"))
                .andExpect(jsonPath("$.name").value("Test Flag"))
                .andExpect(jsonPath("$.description").value("A test flag"))
                .andExpect(jsonPath("$.type").value("BOOLEAN"))
                .andExpect(jsonPath("$.defaultValue").value("true"))
                .andExpect(jsonPath("$.isActive").value(true));
    }

    @Test
    void createFlag_withDuplicateKey_returnsBadRequest() throws Exception {
        FlagDto request = FlagDto.builder()
                .key("duplicate-flag")
                .name("Duplicate Flag")
                .type(FlagType.STRING)
                .defaultValue("default")
                .build();

        mockMvc.perform(post("/flags")
                        .header("Authorization", TestHelper.bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/flags")
                        .header("Authorization", TestHelper.bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("already exists")));
    }

    @Test
    void createFlag_withInvalidBooleanDefault_returnsBadRequest() throws Exception {
        FlagDto request = FlagDto.builder()
                .key("bad-bool-flag")
                .name("Bad Boolean Flag")
                .type(FlagType.BOOLEAN)
                .defaultValue("not-a-boolean")
                .build();

        mockMvc.perform(post("/flags")
                        .header("Authorization", TestHelper.bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createFlag_withInvalidNumberDefault_returnsBadRequest() throws Exception {
        FlagDto request = FlagDto.builder()
                .key("bad-num-flag")
                .name("Bad Number Flag")
                .type(FlagType.NUMBER)
                .defaultValue("not-a-number")
                .build();

        mockMvc.perform(post("/flags")
                        .header("Authorization", TestHelper.bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createFlag_withValidNumberDefault_returnsCreated() throws Exception {
        FlagDto request = FlagDto.builder()
                .key("num-flag")
                .name("Number Flag")
                .type(FlagType.NUMBER)
                .defaultValue("42.5")
                .build();

        mockMvc.perform(post("/flags")
                        .header("Authorization", TestHelper.bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.defaultValue").value("42.5"));
    }

    @Test
    void createFlag_withMissingRequiredFields_returnsBadRequest() throws Exception {
        FlagDto request = FlagDto.builder().build();

        mockMvc.perform(post("/flags")
                        .header("Authorization", TestHelper.bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createFlag_withInvalidKeyFormat_returnsBadRequest() throws Exception {
        FlagDto request = FlagDto.builder()
                .key("Invalid_KEY!")
                .name("Bad Key Flag")
                .type(FlagType.STRING)
                .defaultValue("val")
                .build();

        mockMvc.perform(post("/flags")
                        .header("Authorization", TestHelper.bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAllFlags_returnsListOfFlags() throws Exception {
        createTestFlag("flag-one", "Flag One", FlagType.BOOLEAN, "true");
        createTestFlag("flag-two", "Flag Two", FlagType.STRING, "hello");

        mockMvc.perform(get("/flags")
                        .header("Authorization", TestHelper.bearerToken(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].key", containsInAnyOrder("flag-one", "flag-two")));
    }

    @Test
    void getAllFlags_withSearch_returnsFilteredFlags() throws Exception {
        createTestFlag("feature-login", "Login Feature", FlagType.BOOLEAN, "false");
        createTestFlag("feature-signup", "Signup Feature", FlagType.BOOLEAN, "true");

        mockMvc.perform(get("/flags")
                        .param("search", "login")
                        .header("Authorization", TestHelper.bearerToken(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].key").value("feature-login"));
    }

    @Test
    void getAllFlags_emptyDb_returnsEmptyList() throws Exception {
        mockMvc.perform(get("/flags")
                        .header("Authorization", TestHelper.bearerToken(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getFlagById_withExistingFlag_returnsFlag() throws Exception {
        String flagId = createTestFlag("get-flag", "Get Flag", FlagType.STRING, "value");

        mockMvc.perform(get("/flags/{id}", flagId)
                        .header("Authorization", TestHelper.bearerToken(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.key").value("get-flag"))
                .andExpect(jsonPath("$.name").value("Get Flag"));
    }

    @Test
    void getFlagById_withNonExistentId_returnsNotFound() throws Exception {
        mockMvc.perform(get("/flags/{id}", "00000000-0000-0000-0000-000000000000")
                        .header("Authorization", TestHelper.bearerToken(adminToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateFlag_withValidRequest_returnsUpdatedFlag() throws Exception {
        String flagId = createTestFlag("update-flag", "Original Name", FlagType.STRING, "original");

        FlagDto updateRequest = FlagDto.builder()
                .name("Updated Name")
                .description("Updated description")
                .defaultValue("updated")
                .build();

        mockMvc.perform(patch("/flags/{id}", flagId)
                        .header("Authorization", TestHelper.bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Name"))
                .andExpect(jsonPath("$.description").value("Updated description"))
                .andExpect(jsonPath("$.defaultValue").value("updated"))
                .andExpect(jsonPath("$.key").value("update-flag")); // key should NOT change
    }

    @Test
    void updateFlag_partialUpdate_onlyChangesProvidedFields() throws Exception {
        String flagId = createTestFlag("partial-flag", "Original", FlagType.STRING, "original");

        FlagDto updateRequest = FlagDto.builder()
                .name("New Name Only")
                .build();

        mockMvc.perform(patch("/flags/{id}", flagId)
                        .header("Authorization", TestHelper.bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Name Only"))
                .andExpect(jsonPath("$.defaultValue").value("original")); // unchanged
    }

    @Test
    void deleteFlag_withExistingFlag_returnsNoContent() throws Exception {
        String flagId = createTestFlag("delete-flag", "Delete Me", FlagType.BOOLEAN, "false");

        mockMvc.perform(delete("/flags/{id}", flagId)
                        .header("Authorization", TestHelper.bearerToken(adminToken)))
                .andExpect(status().isNoContent());

        // Verify soft-deleted: GET returns 404
        mockMvc.perform(get("/flags/{id}", flagId)
                        .header("Authorization", TestHelper.bearerToken(adminToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteFlag_withNonExistentId_returnsNotFound() throws Exception {
        mockMvc.perform(delete("/flags/{id}", "00000000-0000-0000-0000-000000000000")
                        .header("Authorization", TestHelper.bearerToken(adminToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void flagEndpoints_withoutAuth_returnsForbidden() throws Exception {
        mockMvc.perform(get("/flags"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/flags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void flagEndpoints_withGuestRole_returnsForbidden() throws Exception {
        // admin already registered in @BeforeEach, so second user becomes GUEST
        String guestToken = TestHelper.registerAndGetToken(
                mockMvc, "guest@test.com", "password123", "Guest", "User");

        mockMvc.perform(get("/flags")
                        .header("Authorization", TestHelper.bearerToken(guestToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateFlag_nonExistentId_returnsNotFound() throws Exception {
        FlagDto request = FlagDto.builder().name("Ghost").build();

        mockMvc.perform(patch("/flags/{id}", "00000000-0000-0000-0000-000000000000")
                        .header("Authorization", TestHelper.bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateFlag_invalidBooleanDefaultValue_returnsBadRequest() throws Exception {
        String flagId = createTestFlag("bool-upd", "Bool Update", FlagType.BOOLEAN, "true");

        FlagDto request = FlagDto.builder().defaultValue("maybe").build();

        mockMvc.perform(patch("/flags/{id}", flagId)
                        .header("Authorization", TestHelper.bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("BOOLEAN")));
    }

    @Test
    void updateFlag_invalidNumberDefaultValue_returnsBadRequest() throws Exception {
        String flagId = createTestFlag("num-upd", "Num Update", FlagType.NUMBER, "42");

        FlagDto request = FlagDto.builder().defaultValue("not-a-number").build();

        mockMvc.perform(patch("/flags/{id}", flagId)
                        .header("Authorization", TestHelper.bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("NUMBER")));
    }

    @Test
    void updateFlag_validBooleanDefaultValue_succeeds() throws Exception {
        String flagId = createTestFlag("bool-ok", "Bool OK", FlagType.BOOLEAN, "true");

        FlagDto request = FlagDto.builder().defaultValue("false").build();

        mockMvc.perform(patch("/flags/{id}", flagId)
                        .header("Authorization", TestHelper.bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.defaultValue").value("false"));
    }

    @Test
    void deleteFlag_cascadesDeactivationOfFlagValues() throws Exception {
        String flagId = createTestFlag("cascade-flag", "Cascade Flag", FlagType.STRING, "val");
        String envId = TestHelper.createEnvironment(mockMvc, adminToken,
                "cascade-env", "Cascade Env", null);
        String fvId = TestHelper.createFlagValue(mockMvc, adminToken, flagId, envId,
                List.of(FlagValueVariantDto.builder().value("val").percentage(100).build()));

        // Delete the flag
        mockMvc.perform(delete("/flags/{id}", flagId)
                        .header("Authorization", TestHelper.bearerToken(adminToken)))
                .andExpect(status().isNoContent());

        // Flag value should be soft-deleted too
        mockMvc.perform(get("/flag-values/{id}", fvId)
                        .header("Authorization", TestHelper.bearerToken(adminToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateFlag_keyIsImmutable_staysUnchanged() throws Exception {
        String flagId = createTestFlag("immutable-key", "Immutable", FlagType.STRING, "val");

        FlagDto request = FlagDto.builder()
                .key("new-key")
                .name("Changed Name")
                .build();

        mockMvc.perform(patch("/flags/{id}", flagId)
                        .header("Authorization", TestHelper.bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.key").value("immutable-key"))
                .andExpect(jsonPath("$.name").value("Changed Name"));
    }

    @Test
    void updateFlag_typeIsImmutable_staysUnchanged() throws Exception {
        String flagId = createTestFlag("type-immutable", "Type Immutable", FlagType.BOOLEAN, "true");

        // Send type=NUMBER but it should be ignored
        FlagDto request = FlagDto.builder()
                .type(FlagType.NUMBER)
                .name("Still Boolean")
                .build();

        mockMvc.perform(patch("/flags/{id}", flagId)
                        .header("Authorization", TestHelper.bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("BOOLEAN"))
                .andExpect(jsonPath("$.name").value("Still Boolean"));
    }

    @Test
    void getAllFlags_searchByName_returnsFiltered() throws Exception {
        createTestFlag("flag-alpha", "Alpha Feature", FlagType.BOOLEAN, "true");
        createTestFlag("flag-beta", "Beta Feature", FlagType.BOOLEAN, "false");

        mockMvc.perform(get("/flags")
                        .param("search", "Alpha")
                        .header("Authorization", TestHelper.bearerToken(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Alpha Feature"));
    }

    @Test
    void getAllFlags_searchByDescription_returnsFiltered() throws Exception {
        createTestFlag("desc-flag-1", "Flag 1", FlagType.STRING, "val");
        // Set description via update
        FlagDto flag2 = FlagDto.builder()
                .key("desc-flag-2")
                .name("Flag 2")
                .description("uniquedescription")
                .type(FlagType.STRING)
                .defaultValue("val")
                .build();
        mockMvc.perform(post("/flags")
                        .header("Authorization", TestHelper.bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(flag2)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/flags")
                        .param("search", "uniquedescription")
                        .header("Authorization", TestHelper.bearerToken(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].key").value("desc-flag-2"));
    }

    @Test
    void getAllFlags_searchCaseInsensitive_returnsMatch() throws Exception {
        createTestFlag("case-flag", "Case Sensitive Flag", FlagType.BOOLEAN, "true");

        mockMvc.perform(get("/flags")
                        .param("search", "CASE SENSITIVE")
                        .header("Authorization", TestHelper.bearerToken(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void getAllFlags_searchNoMatch_returnsEmptyList() throws Exception {
        createTestFlag("some-flag", "Some Flag", FlagType.BOOLEAN, "true");

        mockMvc.perform(get("/flags")
                        .param("search", "nonexistent-term-xyz")
                        .header("Authorization", TestHelper.bearerToken(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getFlagById_invalidUuidFormat_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/flags/{id}", "not-a-uuid")
                        .header("Authorization", TestHelper.bearerToken(adminToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Invalid value")));
    }

    @Test
    void createFlag_softDeletedKeyReuse_succeeds() throws Exception {
        String flagId = createTestFlag("reuse-key", "Original", FlagType.STRING, "val");

        // Soft-delete
        mockMvc.perform(delete("/flags/{id}", flagId)
                        .header("Authorization", TestHelper.bearerToken(adminToken)))
                .andExpect(status().isNoContent());

        // Re-create with same key
        FlagDto request = FlagDto.builder()
                .key("reuse-key")
                .name("Reused Key Flag")
                .type(FlagType.STRING)
                .defaultValue("new-val")
                .build();

        mockMvc.perform(post("/flags")
                        .header("Authorization", TestHelper.bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.key").value("reuse-key"));
    }

    @Test
    void createFlag_malformedJson_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/flags")
                        .header("Authorization", TestHelper.bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{bad json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Malformed JSON")));
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Private helpers
    // ═══════════════════════════════════════════════════════════════════

    private String createTestFlag(String key, String name, FlagType type,
                                  String defaultValue) throws Exception {
        FlagDto request = FlagDto.builder()
                .key(key)
                .name(name)
                .type(type)
                .defaultValue(defaultValue)
                .build();

        MvcResult result = mockMvc.perform(post("/flags")
                        .header("Authorization", TestHelper.bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id").asText();
    }
}
