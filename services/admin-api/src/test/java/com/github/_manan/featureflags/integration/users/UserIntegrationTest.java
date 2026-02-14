package com.github._manan.featureflags.integration.users;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github._manan.featureflags.config.TestConfig;
import com.github._manan.featureflags.dto.RegisterRequest;
import com.github._manan.featureflags.dto.UpdateUserRequest;
import com.github._manan.featureflags.entity.Role;
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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestConfig.class)
@Transactional
class UserIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String adminToken;
    private String adminUserId;
    private String guestToken;
    private String guestUserId;

    @BeforeEach
    void setUp() throws Exception {
        // First user = ADMIN
        String[] adminData = TestHelper.registerAdminAndGetTokenAndId(mockMvc);
        adminToken = adminData[0];
        adminUserId = adminData[1];

        // Second user = GUEST
        MvcResult guestResult = mockMvc.perform(
                        post("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(RegisterRequest.builder()
                                        .email(TestHelper.GUEST_EMAIL)
                                        .password(TestHelper.GUEST_PASSWORD)
                                        .firstName(TestHelper.GUEST_FIRST_NAME)
                                        .lastName(TestHelper.GUEST_LAST_NAME)
                                        .build())))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode guestRoot = objectMapper.readTree(guestResult.getResponse().getContentAsString());
        guestToken = guestRoot.get("token").asText();
        guestUserId = guestRoot.get("user").get("id").asText();
    }

    // ═══════════════════════════════════════════════════════════════════
    //  GET /users
    // ═══════════════════════════════════════════════════════════════════

    @Test
    void getAllUsers_returnsAllRegisteredUsers() throws Exception {
        mockMvc.perform(get("/users")
                        .header("Authorization", TestHelper.bearerToken(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].email",
                        containsInAnyOrder(TestHelper.ADMIN_EMAIL, TestHelper.GUEST_EMAIL)));
    }

    @Test
    void getAllUsers_withoutAuth_returnsForbidden() throws Exception {
        mockMvc.perform(get("/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAllUsers_withGuestRole_returnsForbidden() throws Exception {
        mockMvc.perform(get("/users")
                        .header("Authorization", TestHelper.bearerToken(guestToken)))
                .andExpect(status().isForbidden());
    }

    // ═══════════════════════════════════════════════════════════════════
    //  GET /users/{id}
    // ═══════════════════════════════════════════════════════════════════

    @Test
    void getUserById_existingUser_returnsUser() throws Exception {
        mockMvc.perform(get("/users/{id}", adminUserId)
                        .header("Authorization", TestHelper.bearerToken(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(adminUserId))
                .andExpect(jsonPath("$.email").value(TestHelper.ADMIN_EMAIL))
                .andExpect(jsonPath("$.firstName").value(TestHelper.ADMIN_FIRST_NAME))
                .andExpect(jsonPath("$.lastName").value(TestHelper.ADMIN_LAST_NAME))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.enabled").value(true));
    }

    @Test
    void getUserById_nonExistentId_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/users/{id}", "00000000-0000-0000-0000-000000000000")
                        .header("Authorization", TestHelper.bearerToken(adminToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("User not found")));
    }

    @Test
    void getUserById_invalidUuidFormat_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/users/{id}", "not-a-uuid")
                        .header("Authorization", TestHelper.bearerToken(adminToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Invalid value")));
    }

    @Test
    void getUserById_withoutAuth_returnsForbidden() throws Exception {
        mockMvc.perform(get("/users/{id}", adminUserId))
                .andExpect(status().isForbidden());
    }

    // ═══════════════════════════════════════════════════════════════════
    //  PATCH /users/{id}
    // ═══════════════════════════════════════════════════════════════════

    @Test
    void updateUser_firstName_returnsUpdatedUser() throws Exception {
        UpdateUserRequest request = UpdateUserRequest.builder()
                .firstName("NewFirst")
                .build();

        mockMvc.perform(patch("/users/{id}", guestUserId)
                        .header("Authorization", TestHelper.bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("NewFirst"))
                .andExpect(jsonPath("$.lastName").value(TestHelper.GUEST_LAST_NAME));
    }

    @Test
    void updateUser_lastName_returnsUpdatedUser() throws Exception {
        UpdateUserRequest request = UpdateUserRequest.builder()
                .lastName("NewLast")
                .build();

        mockMvc.perform(patch("/users/{id}", guestUserId)
                        .header("Authorization", TestHelper.bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastName").value("NewLast"))
                .andExpect(jsonPath("$.firstName").value(TestHelper.GUEST_FIRST_NAME));
    }

    @Test
    void updateUser_roleToAdmin_returnsUpdatedRole() throws Exception {
        UpdateUserRequest request = UpdateUserRequest.builder()
                .role(Role.ADMIN)
                .build();

        mockMvc.perform(patch("/users/{id}", guestUserId)
                        .header("Authorization", TestHelper.bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void updateUser_disableAnotherUser_succeeds() throws Exception {
        UpdateUserRequest request = UpdateUserRequest.builder()
                .enabled(false)
                .build();

        mockMvc.perform(patch("/users/{id}", guestUserId)
                        .header("Authorization", TestHelper.bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));
    }

    @Test
    void updateUser_partialUpdate_onlyChangesProvidedFields() throws Exception {
        UpdateUserRequest request = UpdateUserRequest.builder()
                .firstName("Changed")
                .build();

        mockMvc.perform(patch("/users/{id}", guestUserId)
                        .header("Authorization", TestHelper.bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Changed"))
                .andExpect(jsonPath("$.lastName").value(TestHelper.GUEST_LAST_NAME))
                .andExpect(jsonPath("$.role").value("GUEST"))
                .andExpect(jsonPath("$.enabled").value(true));
    }

    @Test
    void updateUser_disableSelf_returnsConflict() throws Exception {
        UpdateUserRequest request = UpdateUserRequest.builder()
                .enabled(false)
                .build();

        mockMvc.perform(patch("/users/{id}", adminUserId)
                        .header("Authorization", TestHelper.bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Cannot change enabled status of your own account"));
    }

    @Test
    void updateUser_nonExistentUser_returnsBadRequest() throws Exception {
        UpdateUserRequest request = UpdateUserRequest.builder()
                .firstName("Ghost")
                .build();

        mockMvc.perform(patch("/users/{id}", "00000000-0000-0000-0000-000000000000")
                        .header("Authorization", TestHelper.bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("User not found")));
    }

    @Test
    void updateUser_firstNameTooLong_returnsBadRequest() throws Exception {
        UpdateUserRequest request = UpdateUserRequest.builder()
                .firstName("A".repeat(101))
                .build();

        mockMvc.perform(patch("/users/{id}", guestUserId)
                        .header("Authorization", TestHelper.bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateUser_lastNameTooLong_returnsBadRequest() throws Exception {
        UpdateUserRequest request = UpdateUserRequest.builder()
                .lastName("B".repeat(101))
                .build();

        mockMvc.perform(patch("/users/{id}", guestUserId)
                        .header("Authorization", TestHelper.bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateUser_withoutAuth_returnsForbidden() throws Exception {
        mockMvc.perform(patch("/users/{id}", guestUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateUser_withGuestRole_returnsForbidden() throws Exception {
        UpdateUserRequest request = UpdateUserRequest.builder()
                .firstName("Hacker")
                .build();

        mockMvc.perform(patch("/users/{id}", guestUserId)
                        .header("Authorization", TestHelper.bearerToken(guestToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ═══════════════════════════════════════════════════════════════════
    //  DELETE /users/{id}
    // ═══════════════════════════════════════════════════════════════════

    @Test
    void deleteUser_anotherUser_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/users/{id}", guestUserId)
                        .header("Authorization", TestHelper.bearerToken(adminToken)))
                .andExpect(status().isNoContent());

        // Verify hard-deleted
        mockMvc.perform(get("/users/{id}", guestUserId)
                        .header("Authorization", TestHelper.bearerToken(adminToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("User not found")));
    }

    @Test
    void deleteUser_self_returnsConflict() throws Exception {
        mockMvc.perform(delete("/users/{id}", adminUserId)
                        .header("Authorization", TestHelper.bearerToken(adminToken)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Cannot delete your own account"));
    }

    @Test
    void deleteUser_nonExistentUser_returnsBadRequest() throws Exception {
        mockMvc.perform(delete("/users/{id}", "00000000-0000-0000-0000-000000000000")
                        .header("Authorization", TestHelper.bearerToken(adminToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("User not found")));
    }

    @Test
    void deleteUser_deletedUserDisappearsFromList() throws Exception {
        mockMvc.perform(delete("/users/{id}", guestUserId)
                        .header("Authorization", TestHelper.bearerToken(adminToken)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/users")
                        .header("Authorization", TestHelper.bearerToken(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].email").value(TestHelper.ADMIN_EMAIL));
    }

    @Test
    void deleteUser_withoutAuth_returnsForbidden() throws Exception {
        mockMvc.perform(delete("/users/{id}", guestUserId))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteUser_withGuestRole_returnsForbidden() throws Exception {
        mockMvc.perform(delete("/users/{id}", guestUserId)
                        .header("Authorization", TestHelper.bearerToken(guestToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateUser_multipleFieldsAtOnce_allApplied() throws Exception {
        UpdateUserRequest request = UpdateUserRequest.builder()
                .firstName("Multi")
                .lastName("Update")
                .role(Role.ADMIN)
                .enabled(true)
                .build();

        mockMvc.perform(patch("/users/{id}", guestUserId)
                        .header("Authorization", TestHelper.bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Multi"))
                .andExpect(jsonPath("$.lastName").value("Update"))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.enabled").value(true));
    }

    @Test
    void getUserById_guestUser_returnsCorrectRole() throws Exception {
        mockMvc.perform(get("/users/{id}", guestUserId)
                        .header("Authorization", TestHelper.bearerToken(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("GUEST"))
                .andExpect(jsonPath("$.email").value(TestHelper.GUEST_EMAIL));
    }

    @Test
    void updateUser_enableSelf_returnsConflict() throws Exception {
        // Both enabling and disabling self are blocked
        UpdateUserRequest request = UpdateUserRequest.builder()
                .enabled(true)
                .build();

        mockMvc.perform(patch("/users/{id}", adminUserId)
                        .header("Authorization", TestHelper.bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Cannot change enabled status of your own account"));
    }

    @Test
    void updateUser_emptyFirstName_returnsBadRequest() throws Exception {
        UpdateUserRequest request = UpdateUserRequest.builder()
                .firstName("")
                .build();

        mockMvc.perform(patch("/users/{id}", guestUserId)
                        .header("Authorization", TestHelper.bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateUser_emptyLastName_returnsBadRequest() throws Exception {
        UpdateUserRequest request = UpdateUserRequest.builder()
                .lastName("")
                .build();

        mockMvc.perform(patch("/users/{id}", guestUserId)
                        .header("Authorization", TestHelper.bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateUser_invalidRoleValue_returnsBadRequest() throws Exception {
        mockMvc.perform(patch("/users/{id}", guestUserId)
                        .header("Authorization", TestHelper.bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\": \"SUPERADMIN\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateUser_emptyBody_returnsOriginalUser() throws Exception {
        mockMvc.perform(patch("/users/{id}", guestUserId)
                        .header("Authorization", TestHelper.bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value(TestHelper.GUEST_FIRST_NAME))
                .andExpect(jsonPath("$.lastName").value(TestHelper.GUEST_LAST_NAME))
                .andExpect(jsonPath("$.role").value("GUEST"))
                .andExpect(jsonPath("$.enabled").value(true));
    }

    @Test
    void deleteUser_verifyGetByIdReturns400() throws Exception {
        mockMvc.perform(delete("/users/{id}", guestUserId)
                        .header("Authorization", TestHelper.bearerToken(adminToken)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/users/{id}", guestUserId)
                        .header("Authorization", TestHelper.bearerToken(adminToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("User not found")));
    }

    @Test
    void updateUser_changeRoleToGuest_succeeds() throws Exception {
        // First promote guest to admin
        UpdateUserRequest promote = UpdateUserRequest.builder().role(Role.ADMIN).build();
        mockMvc.perform(patch("/users/{id}", guestUserId)
                        .header("Authorization", TestHelper.bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(promote)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));

        // Then demote back to guest
        UpdateUserRequest demote = UpdateUserRequest.builder().role(Role.GUEST).build();
        mockMvc.perform(patch("/users/{id}", guestUserId)
                        .header("Authorization", TestHelper.bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(demote)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("GUEST"));
    }

    @Test
    void updateUser_reEnableDisabledUser_succeeds() throws Exception {
        // Disable guest
        UpdateUserRequest disable = UpdateUserRequest.builder().enabled(false).build();
        mockMvc.perform(patch("/users/{id}", guestUserId)
                        .header("Authorization", TestHelper.bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(disable)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));

        // Re-enable
        UpdateUserRequest enable = UpdateUserRequest.builder().enabled(true).build();
        mockMvc.perform(patch("/users/{id}", guestUserId)
                        .header("Authorization", TestHelper.bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(enable)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true));
    }

    @Test
    void getAllUsers_responseStructure_containsTimestamps() throws Exception {
        mockMvc.perform(get("/users")
                        .header("Authorization", TestHelper.bearerToken(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].createdAt").isNotEmpty())
                .andExpect(jsonPath("$[0].updatedAt").isNotEmpty());
    }

    @Test
    void updateUser_malformedJson_returnsBadRequest() throws Exception {
        mockMvc.perform(patch("/users/{id}", guestUserId)
                        .header("Authorization", TestHelper.bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{bad json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Malformed JSON")));
    }
}
