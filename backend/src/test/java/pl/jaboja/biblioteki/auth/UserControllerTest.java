package pl.jaboja.biblioteki.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(pl.jaboja.biblioteki.TestDataConfig.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private UserService userService;

    private User adminUser;
    private User regularUser;
    private User targetUser;

    @BeforeEach
    void setUp() {
        adminUser = new User(1L, "admin", "encodedPassword", "Admin User", true, "ADMIN");
        regularUser = new User(2L, "user1", "encodedPassword", "Regular User", true, "USER");
        targetUser = new User(3L, "user2", "encodedPassword", "Target User", true, "USER");
        
        // Configure mock to return appropriate users
        when(userService.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(userService.findByUsername("user1")).thenReturn(Optional.of(regularUser));
        when(userService.findByUsername("user2")).thenReturn(Optional.of(targetUser));
    }

    // ==================== GET /api/users ====================

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void getAllUsers_AdminShouldGetAllUsers() throws Exception {
        when(userService.getAllUsers()).thenReturn(List.of(regularUser, targetUser));

        mockMvc.perform(get("/api/users"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()", is(2)))
            .andExpect(jsonPath("$[0].username", is("user1")))
            .andExpect(jsonPath("$[1].username", is("user2")));
    }

    @Test
    @WithMockUser(username = "user1", roles = {"USER"})
    void getAllUsers_NonAdminShouldBeForbidden() throws Exception {
        mockMvc.perform(get("/api/users"))
            .andExpect(status().isForbidden());
    }

    @Test
    void getAllUsers_UnauthenticatedShouldBeUnauthorized() throws Exception {
        mockMvc.perform(get("/api/users"))
            .andExpect(status().isUnauthorized());
    }

    // ==================== GET /api/users/{id} ====================

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void getUserById_AdminShouldGetUser() throws Exception {
        when(userService.findById(3L)).thenReturn(Optional.of(targetUser));

        mockMvc.perform(get("/api/users/3"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username", is("user2")));
    }

    @Test
    @WithMockUser(username = "user1", roles = {"USER"})
    void getUserById_SelfShouldGetOwnUser() throws Exception {
        when(userService.findByUsername("user1")).thenReturn(Optional.of(regularUser));
        when(userService.findById(2L)).thenReturn(Optional.of(regularUser));

        mockMvc.perform(get("/api/users/2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username", is("user1")));
    }

    @Test
    @WithMockUser(username = "user1", roles = {"USER"})
    void getUserById_NonAdminCannotGetOtherUser() throws Exception {
        when(userService.findByUsername("user1")).thenReturn(Optional.of(regularUser));
        when(userService.findById(3L)).thenReturn(Optional.of(targetUser));

        mockMvc.perform(get("/api/users/3"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void getUserById_NonExistentUserShouldReturnNotFound() throws Exception {
        when(userService.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/users/999"))
            .andExpect(status().isNotFound());
    }

    @Test
    void getUserById_UnauthenticatedShouldBeUnauthorized() throws Exception {
        mockMvc.perform(get("/api/users/1"))
            .andExpect(status().isUnauthorized());
    }

    // ==================== GET /api/users/me ====================

    @Test
    @WithMockUser(username = "user1", roles = {"USER"})
    void getCurrentUserProfile_ShouldReturnUserProfile() throws Exception {
        when(userService.findByUsername("user1")).thenReturn(Optional.of(regularUser));

        mockMvc.perform(get("/api/users/me"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username", is("user1")))
            .andExpect(jsonPath("$.fullName", is("Regular User")));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void getCurrentUserProfile_AdminShouldGetOwnProfile() throws Exception {
        when(userService.findByUsername("admin")).thenReturn(Optional.of(adminUser));

        mockMvc.perform(get("/api/users/me"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username", is("admin")))
            .andExpect(jsonPath("$.roles", is("ADMIN")));
    }

    @Test
    void getCurrentUserProfile_UnauthenticatedShouldBeUnauthorized() throws Exception {
        mockMvc.perform(get("/api/users/me"))
            .andExpect(status().isUnauthorized());
    }

    // ==================== POST /api/users ====================

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void createUser_AdminShouldCreateUser() throws Exception {
        UserRequest request = new UserRequest("newuser", "password123", "New User", "USER", true);
        User createdUser = new User(4L, "newuser", "encodedPassword", "New User", true, "USER");

        when(userService.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(userService.createUser(any(User.class))).thenReturn(createdUser);

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username", is("newuser")))
            .andExpect(jsonPath("$.fullName", is("New User")));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void createUser_WithMinimalData() throws Exception {
        UserRequest request = new UserRequest("newuser", "password123", null, null, null);
        User createdUser = new User(4L, "newuser", "encodedPassword", null, true, "USER");

        when(userService.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(userService.createUser(any(User.class))).thenReturn(createdUser);

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username", is("newuser")));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void createUser_WithExistingUsernameShouldFail() throws Exception {
        UserRequest request = new UserRequest("user1", "password123", "New User", "USER", true);

        when(userService.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(userService.createUser(any(User.class))).thenThrow(new IllegalArgumentException("Username already exists"));

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void createUser_WithEmptyUsernameShouldFail() throws Exception {
        UserRequest request = new UserRequest("", "password123", "New User", "USER", true);

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void createUser_WithNullUsernameShouldFail() throws Exception {
        UserRequest request = new UserRequest(null, "password123", "New User", "USER", true);

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void createUser_WithNullPasswordShouldFail() throws Exception {
        UserRequest request = new UserRequest("newuser", null, "New User", "USER", true);

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "user1", roles = {"USER"})
    void createUser_NonAdminShouldBeForbidden() throws Exception {
        UserRequest request = new UserRequest("newuser", "password123", "New User", "USER", true);

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden());
    }

    @Test
    void createUser_UnauthenticatedShouldBeUnauthorized() throws Exception {
        UserRequest request = new UserRequest("newuser", "password123", "New User", "USER", true);

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized());
    }

    // ==================== PUT /api/users/{id} ====================

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void updateUser_AdminShouldUpdateAnyUser() throws Exception {
        UserRequest request = new UserRequest("newusername", "newpassword", "New Full Name", "ADMIN,USER", true);
        User updatedUser = new User(3L, "newusername", "encodedPassword", "New Full Name", true, "ADMIN,USER");

        when(userService.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(userService.findById(3L)).thenReturn(Optional.of(targetUser));
        when(userService.updateUser(any(User.class), anyString())).thenReturn(updatedUser);

        mockMvc.perform(put("/api/users/3")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username", is("newusername")))
            .andExpect(jsonPath("$.roles", is("ADMIN,USER")));
    }

    @Test
    @WithMockUser(username = "user1", roles = {"USER"})
    void updateUser_SelfShouldUpdateOwnProfile() throws Exception {
        UserRequest request = new UserRequest("newusername", "newpassword", "New Full Name", "USER", true);
        User updatedUser = new User(2L, "newusername", "encodedPassword", "New Full Name", true, "USER");

        when(userService.findByUsername("user1")).thenReturn(Optional.of(regularUser));
        when(userService.findById(2L)).thenReturn(Optional.of(regularUser));
        when(userService.updateUser(any(User.class), anyString())).thenReturn(updatedUser);

        mockMvc.perform(put("/api/users/2")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username", is("newusername")));
    }

    @Test
    @WithMockUser(username = "user1", roles = {"USER"})
    void updateUser_SelfCannotUpdateRoles() throws Exception {
        UserRequest request = new UserRequest("user1", null, null, "ADMIN", true);

        when(userService.findByUsername("user1")).thenReturn(Optional.of(regularUser));
        when(userService.findById(2L)).thenReturn(Optional.of(regularUser));
        when(userService.updateUser(any(User.class), anyString()))
            .thenThrow(new AccessDeniedException("You cannot update your own roles"));

        mockMvc.perform(put("/api/users/2")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "user1", roles = {"USER"})
    void updateUser_NonAdminCannotUpdateOtherUser() throws Exception {
        UserRequest request = new UserRequest("newusername", null, null, "USER", true);

        when(userService.findByUsername("user1")).thenReturn(Optional.of(regularUser));
        when(userService.findById(3L)).thenReturn(Optional.of(targetUser));

        mockMvc.perform(put("/api/users/3")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void updateUser_NonExistentUserShouldReturnNotFound() throws Exception {
        UserRequest request = new UserRequest("newusername", null, null, "USER", true);

        when(userService.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(userService.findById(999L)).thenReturn(Optional.empty());
        when(userService.updateUser(any(User.class), anyString()))
            .thenThrow(new UsernameNotFoundException("User not found"));

        mockMvc.perform(put("/api/users/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound());
    }

    @Test
    void updateUser_UnauthenticatedShouldBeUnauthorized() throws Exception {
        UserRequest request = new UserRequest("newusername", null, null, "USER", true);

        mockMvc.perform(put("/api/users/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized());
    }

    // ==================== PUT /api/users/me ====================

    @Test
    @WithMockUser(username = "user1", roles = {"USER"})
    void updateSelf_ShouldUpdateOwnProfile() throws Exception {
        UserRequest.SelfUpdateRequest request = new UserRequest.SelfUpdateRequest("newpassword", "New Full Name", "newusername");
        User updatedUser = new User(2L, "newusername", "encodedPassword", "New Full Name", true, "USER");

        when(userService.findByUsername("user1")).thenReturn(Optional.of(regularUser));
        when(userService.findById(2L)).thenReturn(Optional.of(regularUser));
        when(userService.updateUser(any(User.class), anyString())).thenReturn(updatedUser);

        mockMvc.perform(put("/api/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username", is("newusername")))
            .andExpect(jsonPath("$.fullName", is("New Full Name")));
    }

    @Test
    @WithMockUser(username = "user1", roles = {"USER"})
    void updateSelf_WithEmptyFields() throws Exception {
        UserRequest.SelfUpdateRequest request = new UserRequest.SelfUpdateRequest(null, null, null);
        User updatedUser = new User(2L, "user1", "encodedPassword", "Regular User", true, "USER");

        when(userService.findByUsername("user1")).thenReturn(Optional.of(regularUser));
        when(userService.findById(2L)).thenReturn(Optional.of(regularUser));
        when(userService.updateUser(any(User.class), anyString())).thenReturn(updatedUser);

        mockMvc.perform(put("/api/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk());
    }

    @Test
    void updateSelf_UnauthenticatedShouldBeUnauthorized() throws Exception {
        UserRequest.SelfUpdateRequest request = new UserRequest.SelfUpdateRequest("newpassword", "New Full Name", "newusername");

        mockMvc.perform(put("/api/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized());
    }

    // ==================== POST /api/users/{id}/disable ====================

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void disableUser_AdminShouldDisableAnyUser() throws Exception {
        User disabledUser = new User(3L, "user2", "encodedPassword", "Target User", false, "USER");

        when(userService.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(userService.findById(3L)).thenReturn(Optional.of(targetUser));
        when(userService.disableUser(3L)).thenReturn(disabledUser);

        mockMvc.perform(post("/api/users/3/disable"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.enabled", is(false)));
    }

    @Test
    @WithMockUser(username = "user1", roles = {"USER"})
    void disableUser_SelfShouldDisableSelf() throws Exception {
        User disabledUser = new User(2L, "user1", "encodedPassword", "Regular User", false, "USER");

        when(userService.findByUsername("user1")).thenReturn(Optional.of(regularUser));
        when(userService.findById(2L)).thenReturn(Optional.of(regularUser));
        when(userService.disableUser(2L)).thenReturn(disabledUser);

        mockMvc.perform(post("/api/users/2/disable"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.enabled", is(false)));
    }

    @Test
    @WithMockUser(username = "user1", roles = {"USER"})
    void disableUser_NonAdminCannotDisableOtherUser() throws Exception {
        when(userService.findByUsername("user1")).thenReturn(Optional.of(regularUser));
        when(userService.findById(3L)).thenReturn(Optional.of(targetUser));
        when(userService.disableUser(3L))
            .thenThrow(new AccessDeniedException("You can only disable your own account"));

        mockMvc.perform(post("/api/users/3/disable"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void disableUser_LastAdminShouldBeForbidden() throws Exception {
        when(userService.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(userService.findById(1L)).thenReturn(Optional.of(adminUser));
        when(userService.disableUser(1L))
            .thenThrow(new AccessDeniedException("Cannot disable the last admin account"));

        mockMvc.perform(post("/api/users/1/disable"))
            .andExpect(status().isForbidden());
    }

    @Test
    void disableUser_UnauthenticatedShouldBeUnauthorized() throws Exception {
        mockMvc.perform(post("/api/users/1/disable"))
            .andExpect(status().isUnauthorized());
    }

    // ==================== Last Admin Protection Tests ====================

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void updateUser_LastAdminCannotDisableSelfViaEdit() throws Exception {
        UserRequest request = new UserRequest("admin", null, null, "ADMIN", false);

        when(userService.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(userService.findById(1L)).thenReturn(Optional.of(adminUser));
        when(userService.updateUser(any(User.class), anyString()))
            .thenThrow(new AccessDeniedException("Cannot disable the last admin account"));

        mockMvc.perform(put("/api/users/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void updateUser_LastAdminCannotDisableOtherAdminViaEdit() throws Exception {
        User otherAdmin = new User(2L, "admin2", "encodedPassword", "Other Admin", true, "ADMIN");
        UserRequest request = new UserRequest("admin2", null, null, "ADMIN", false);

        when(userService.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(userService.findById(2L)).thenReturn(Optional.of(otherAdmin));
        when(userService.updateUser(any(User.class), anyString()))
            .thenThrow(new AccessDeniedException("Cannot disable the last admin account"));

        mockMvc.perform(put("/api/users/2")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden());
    }

    // ==================== POST /api/users/{id}/enable ====================

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void enableUser_AdminShouldEnableUser() throws Exception {
        User enabledUser = new User(3L, "user2", "encodedPassword", "Target User", true, "USER");

        when(userService.enableUser(3L)).thenReturn(enabledUser);

        mockMvc.perform(post("/api/users/3/enable"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.enabled", is(true)));
    }

    @Test
    @WithMockUser(username = "user1", roles = {"USER"})
    void enableUser_NonAdminShouldBeForbidden() throws Exception {
        mockMvc.perform(post("/api/users/3/enable"))
            .andExpect(status().isForbidden());
    }

    @Test
    void enableUser_UnauthenticatedShouldBeUnauthorized() throws Exception {
        mockMvc.perform(post("/api/users/1/enable"))
            .andExpect(status().isUnauthorized());
    }

    // ==================== POST /api/users/me/disable ====================

    @Test
    @WithMockUser(username = "user1", roles = {"USER"})
    void disableSelf_ShouldWork() throws Exception {
        User disabledUser = new User(2L, "user1", "encodedPassword", "Regular User", false, "USER");

        when(userService.findByUsername("user1")).thenReturn(Optional.of(regularUser));
        when(userService.findById(2L)).thenReturn(Optional.of(regularUser));
        when(userService.disableUser(2L)).thenReturn(disabledUser);

        mockMvc.perform(post("/api/users/me/disable"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.enabled", is(false)));
    }

    @Test
    void disableSelf_UnauthenticatedShouldBeUnauthorized() throws Exception {
        mockMvc.perform(post("/api/users/me/disable"))
            .andExpect(status().isUnauthorized());
    }

    // ==================== DELETE /api/users/{id} ====================

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void deleteUser_AdminShouldDeleteAnyUser() throws Exception {
        when(userService.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(userService.findById(3L)).thenReturn(Optional.of(targetUser));
        doNothing().when(userService).deleteUser(3L);

        mockMvc.perform(delete("/api/users/3"))
            .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "user1", roles = {"USER"})
    void deleteUser_SelfShouldDeleteSelf() throws Exception {
        when(userService.findByUsername("user1")).thenReturn(Optional.of(regularUser));
        when(userService.findById(2L)).thenReturn(Optional.of(regularUser));
        doNothing().when(userService).deleteUser(2L);

        mockMvc.perform(delete("/api/users/2"))
            .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "user1", roles = {"USER"})
    void deleteUser_NonAdminCannotDeleteOtherUser() throws Exception {
        when(userService.findByUsername("user1")).thenReturn(Optional.of(regularUser));
        when(userService.findById(3L)).thenReturn(Optional.of(targetUser));
        doThrow(new AccessDeniedException("You can only delete your own account"))
            .when(userService).deleteUser(3L);

        mockMvc.perform(delete("/api/users/3"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void deleteUser_LastAdminShouldBeForbidden() throws Exception {
        when(userService.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(userService.findById(1L)).thenReturn(Optional.of(adminUser));
        doThrow(new AccessDeniedException("Cannot delete the last admin account"))
            .when(userService).deleteUser(1L);

        mockMvc.perform(delete("/api/users/1"))
            .andExpect(status().isForbidden());
    }

    @Test
    void deleteUser_UnauthenticatedShouldBeUnauthorized() throws Exception {
        mockMvc.perform(delete("/api/users/1"))
            .andExpect(status().isUnauthorized());
    }

    // ==================== DELETE /api/users/me ====================

    @Test
    @WithMockUser(username = "user1", roles = {"USER"})
    void deleteSelf_ShouldWork() throws Exception {
        when(userService.findByUsername("user1")).thenReturn(Optional.of(regularUser));
        when(userService.findById(2L)).thenReturn(Optional.of(regularUser));
        doNothing().when(userService).deleteUser(2L);

        mockMvc.perform(delete("/api/users/me"))
            .andExpect(status().isNoContent());
    }

    @Test
    void deleteSelf_UnauthenticatedShouldBeUnauthorized() throws Exception {
        mockMvc.perform(delete("/api/users/me"))
            .andExpect(status().isUnauthorized());
    }

    // ==================== Authorization Tests ====================

    @Test
    void allEndpoints_UnauthenticatedShouldBeUnauthorized() throws Exception {
        mockMvc.perform(get("/api/users")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/users/me")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/users/1")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/users")).andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/users/1")).andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/users/me")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/users/1/disable")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/users/1/enable")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/users/me/disable")).andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/users/1")).andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/users/me")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "user1", roles = {"USER"})
    void userEndpoints_NonAdminAccessTests() throws Exception {
        mockMvc.perform(get("/api/users")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/users/3")).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/users")).andExpect(status().isForbidden());
        mockMvc.perform(put("/api/users/3")).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/users/3/disable")).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/users/3/enable")).andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/users/3")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "user1", roles = {"USER"})
    void userEndpoints_SelfAccessTests() throws Exception {
        when(userService.findByUsername("user1")).thenReturn(Optional.of(regularUser));
        when(userService.findById(2L)).thenReturn(Optional.of(regularUser));
        when(userService.updateUser(any(User.class), anyString())).thenReturn(regularUser);
        when(userService.disableUser(2L)).thenReturn(regularUser);
        doNothing().when(userService).deleteUser(2L);

        mockMvc.perform(get("/api/users/me")).andExpect(status().isOk());
        mockMvc.perform(get("/api/users/2")).andExpect(status().isOk());
        mockMvc.perform(put("/api/users/me")).andExpect(status().isOk());
        mockMvc.perform(post("/api/users/me/disable")).andExpect(status().isOk());
        mockMvc.perform(delete("/api/users/me")).andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void adminEndpoints_FullAccessTests() throws Exception {
        when(userService.getAllUsers()).thenReturn(List.of(regularUser, targetUser));
        when(userService.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(userService.findById(3L)).thenReturn(Optional.of(targetUser));
        when(userService.createUser(any(User.class))).thenReturn(targetUser);
        when(userService.updateUser(any(User.class), anyString())).thenReturn(targetUser);
        when(userService.disableUser(3L)).thenReturn(targetUser);
        when(userService.enableUser(3L)).thenReturn(targetUser);
        doNothing().when(userService).deleteUser(3L);

        mockMvc.perform(get("/api/users")).andExpect(status().isOk());
        mockMvc.perform(get("/api/users/3")).andExpect(status().isOk());
        mockMvc.perform(post("/api/users")).andExpect(status().isOk());
        mockMvc.perform(put("/api/users/3")).andExpect(status().isOk());
        mockMvc.perform(post("/api/users/3/disable")).andExpect(status().isOk());
        mockMvc.perform(post("/api/users/3/enable")).andExpect(status().isOk());
        mockMvc.perform(delete("/api/users/3")).andExpect(status().isNoContent());
    }
}
