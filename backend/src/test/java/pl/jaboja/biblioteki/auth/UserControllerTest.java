package pl.jaboja.biblioteki.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@AutoConfigureEmbeddedDatabase(provider = AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY)
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
        void getAllUsers_AdminShouldGetAllUsers() throws Exception {
        when(userService.getAllUsers()).thenReturn(List.of(regularUser, targetUser));

        mockMvc.perform(get("/api/users").with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()", is(2)))
            .andExpect(jsonPath("$[0].username", is("user1")))
            .andExpect(jsonPath("$[1].username", is("user2")));
    }

    @Test
        void getAllUsers_NonAdminShouldBeForbidden() throws Exception {
        mockMvc.perform(get("/api/users").with(user("user1").roles("USER")))
            .andExpect(status().isForbidden());
    }

    @Test
    void getAllUsers_UnauthenticatedShouldBeForbidden() throws Exception {
        mockMvc.perform(get("/api/users"))
            .andExpect(status().isForbidden());
    }

    // ==================== GET /api/users/{id} ====================

    @Test
        void getUserById_AdminShouldGetUser() throws Exception {
        when(userService.findById(3L)).thenReturn(Optional.of(targetUser));

        mockMvc.perform(get("/api/users/3").with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username", is("user2")));
    }

    @Test
        void getUserById_SelfShouldGetOwnUser() throws Exception {
        when(userService.findByUsername("user1")).thenReturn(Optional.of(regularUser));
        when(userService.findById(2L)).thenReturn(Optional.of(regularUser));

        mockMvc.perform(get("/api/users/2").with(user("user1").roles("USER")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username", is("user1")));
    }

    @Test
        void getUserById_NonAdminCannotGetOtherUser() throws Exception {
        when(userService.findByUsername("user1")).thenReturn(Optional.of(regularUser));
        when(userService.findById(3L)).thenReturn(Optional.of(targetUser));

        mockMvc.perform(get("/api/users/3").with(user("user1").roles("USER")))
            .andExpect(status().isForbidden());
    }

    @Test
        void getUserById_NonExistentUserShouldReturnNotFound() throws Exception {
        when(userService.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/users/999").with(user("admin").roles("ADMIN")))
            .andExpect(status().isNotFound());
    }

    @Test
    void getUserById_UnauthenticatedShouldBeForbidden() throws Exception {
        mockMvc.perform(get("/api/users/1"))
            .andExpect(status().isForbidden());
    }

    // ==================== GET /api/users/me ====================

    @Test
        void getCurrentUserProfile_ShouldReturnUserProfile() throws Exception {
        when(userService.findByUsername("user1")).thenReturn(Optional.of(regularUser));

        mockMvc.perform(get("/api/users/me").with(user("user1").roles("USER")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username", is("user1")))
            .andExpect(jsonPath("$.fullName", is("Regular User")));
    }

    @Test
        void getCurrentUserProfile_AdminShouldGetOwnProfile() throws Exception {
        when(userService.findByUsername("admin")).thenReturn(Optional.of(adminUser));

        mockMvc.perform(get("/api/users/me").with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username", is("admin")))
            .andExpect(jsonPath("$.roles", is("ADMIN")));
    }

    @Test
    void getCurrentUserProfile_UnauthenticatedShouldBeForbidden() throws Exception {
        mockMvc.perform(get("/api/users/me"))
            .andExpect(status().isForbidden());
    }

    // ==================== POST /api/users ====================

    @Test
        void createUser_AdminShouldCreateUser() throws Exception {
        UserRequest request = new UserRequest("newuser", "password123", "New User", "USER", true);
        User createdUser = new User(4L, "newuser", "encodedPassword", "New User", true, "USER");

        when(userService.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(userService.createUser(any(User.class))).thenReturn(createdUser);

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)).with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username", is("newuser")))
            .andExpect(jsonPath("$.fullName", is("New User")));
    }

    @Test
        void createUser_WithMinimalData() throws Exception {
        UserRequest request = new UserRequest("newuser", "password123", null, null, null);
        User createdUser = new User(4L, "newuser", "encodedPassword", null, true, "USER");

        when(userService.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(userService.createUser(any(User.class))).thenReturn(createdUser);

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)).with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username", is("newuser")));
    }

    @Test
        void createUser_WithExistingUsernameShouldFail() throws Exception {
        UserRequest request = new UserRequest("user1", "password123", "New User", "USER", true);

        when(userService.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(userService.createUser(any(User.class))).thenThrow(new IllegalArgumentException("Username already exists"));

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)).with(user("admin").roles("ADMIN")))
            .andExpect(status().isBadRequest());
    }

    @Test
        void createUser_WithEmptyUsernameShouldFail() throws Exception {
        UserRequest request = new UserRequest("", "password123", "New User", "USER", true);

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)).with(user("admin").roles("ADMIN")))
            .andExpect(status().isBadRequest());
    }

    @Test
        void createUser_WithNullUsernameShouldFail() throws Exception {
        UserRequest request = new UserRequest(null, "password123", "New User", "USER", true);

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)).with(user("admin").roles("ADMIN")))
            .andExpect(status().isBadRequest());
    }

    @Test
        void createUser_WithNullPasswordShouldFail() throws Exception {
        UserRequest request = new UserRequest("newuser", "", "New User", "USER", true);

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)).with(user("admin").roles("ADMIN")))
            .andExpect(status().isBadRequest());
    }

    @Test
        void createUser_NonAdminShouldBeForbidden() throws Exception {
        UserRequest request = new UserRequest("newuser", "password123", "New User", "USER", true);

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)).with(user("user1").roles("USER")))
            .andExpect(status().isForbidden());
    }

    @Test
    void createUser_UnauthenticatedShouldBeForbidden() throws Exception {
        UserRequest request = new UserRequest("newuser", "password123", "New User", "USER", true);

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden());
    }

    // ==================== PUT /api/users/{id} ====================

    @Test
        void updateUser_AdminShouldUpdateAnyUser() throws Exception {
        UserRequest request = new UserRequest("newusername", "newpassword", "New Full Name", "ADMIN,USER", true);
        User updatedUser = new User(3L, "newusername", "encodedPassword", "New Full Name", true, "ADMIN,USER");

        when(userService.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(userService.findById(3L)).thenReturn(Optional.of(targetUser));
        when(userService.updateUser(any(User.class), any())).thenReturn(updatedUser);

        mockMvc.perform(put("/api/users/3")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)).with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username", is("newusername")))
            .andExpect(jsonPath("$.roles", is("ADMIN,USER")));
    }

    @Test
        void updateUser_SelfShouldUpdateOwnProfile() throws Exception {
        UserRequest request = new UserRequest("newusername", "newpassword", "New Full Name", null, true);
        User updatedUser = new User(2L, "newusername", "encodedPassword", "New Full Name", true, "USER");

        when(userService.findByUsername("user1")).thenReturn(Optional.of(regularUser));
        when(userService.findById(2L)).thenReturn(Optional.of(regularUser));
        when(userService.updateUser(any(User.class), any())).thenReturn(updatedUser);

        mockMvc.perform(put("/api/users/2")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)).with(user("user1").roles("USER")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username", is("newusername")));
    }

    @Test
        void updateUser_SelfCannotUpdateRoles() throws Exception {
        UserRequest request = new UserRequest("user1", null, null, "ADMIN", true);

        when(userService.findByUsername("user1")).thenReturn(Optional.of(regularUser));
        when(userService.findById(2L)).thenReturn(Optional.of(regularUser));
        when(userService.updateUser(any(User.class), any()))
            .thenThrow(new AccessDeniedException("You cannot update your own roles"));

        mockMvc.perform(put("/api/users/2")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)).with(user("user1").roles("USER")))
            .andExpect(status().isForbidden());
    }

    @Test
        void updateUser_NonAdminCannotUpdateOtherUser() throws Exception {
        UserRequest request = new UserRequest("newusername", null, null, "USER", true);

        when(userService.findByUsername("user1")).thenReturn(Optional.of(regularUser));
        when(userService.findById(3L)).thenReturn(Optional.of(targetUser));

        mockMvc.perform(put("/api/users/3")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)).with(user("user1").roles("USER")))
            .andExpect(status().isForbidden());
    }

    @Test
        void updateUser_NonExistentUserShouldReturnNotFound() throws Exception {
        UserRequest request = new UserRequest("newusername", null, null, "USER", true);

        when(userService.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(userService.findById(999L)).thenReturn(Optional.empty());
        when(userService.updateUser(any(User.class), any()))
            .thenThrow(new UsernameNotFoundException("User not found"));

        mockMvc.perform(put("/api/users/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)).with(user("admin").roles("ADMIN")))
            .andExpect(status().isNotFound());
    }

    @Test
    void updateUser_UnauthenticatedShouldBeForbidden() throws Exception {
        UserRequest request = new UserRequest("newusername", null, null, "USER", true);

        mockMvc.perform(put("/api/users/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden());
    }

    // ==================== PUT /api/users/me ====================

    @Test
        void updateSelf_ShouldUpdateOwnProfile() throws Exception {
        UserRequest.SelfUpdateRequest request = new UserRequest.SelfUpdateRequest("newpassword", "New Full Name", "newusername");
        User updatedUser = new User(2L, "newusername", "encodedPassword", "New Full Name", true, "USER");

        when(userService.findByUsername("user1")).thenReturn(Optional.of(regularUser));
        when(userService.findById(2L)).thenReturn(Optional.of(regularUser));
        when(userService.updateUser(any(User.class), any())).thenReturn(updatedUser);

        mockMvc.perform(put("/api/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)).with(user("user1").roles("USER")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username", is("newusername")))
            .andExpect(jsonPath("$.fullName", is("New Full Name")));
    }

    @Test
        void updateSelf_WithEmptyFields() throws Exception {
        UserRequest.SelfUpdateRequest request = new UserRequest.SelfUpdateRequest(null, null, null);
        User updatedUser = new User(2L, "user1", "encodedPassword", "Regular User", true, "USER");

        when(userService.findByUsername("user1")).thenReturn(Optional.of(regularUser));
        when(userService.findById(2L)).thenReturn(Optional.of(regularUser));
        when(userService.updateUser(any(User.class), any())).thenReturn(updatedUser);

        mockMvc.perform(put("/api/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)).with(user("user1").roles("USER")))
            .andExpect(status().isOk());
    }

    @Test
    void updateSelf_UnauthenticatedShouldBeForbidden() throws Exception {
        UserRequest.SelfUpdateRequest request = new UserRequest.SelfUpdateRequest("newpassword", "New Full Name", "newusername");

        mockMvc.perform(put("/api/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden());
    }

    // ==================== POST /api/users/{id}/disable ====================

    @Test
        void disableUser_AdminShouldDisableAnyUser() throws Exception {
        User disabledUser = new User(3L, "user2", "encodedPassword", "Target User", false, "USER");

        when(userService.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(userService.findById(3L)).thenReturn(Optional.of(targetUser));
        when(userService.disableUser(3L)).thenReturn(disabledUser);

        mockMvc.perform(post("/api/users/3/disable").with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.enabled", is(false)));
    }

    @Test
        void disableUser_SelfShouldDisableSelf() throws Exception {
        User disabledUser = new User(2L, "user1", "encodedPassword", "Regular User", false, "USER");

        when(userService.findByUsername("user1")).thenReturn(Optional.of(regularUser));
        when(userService.findById(2L)).thenReturn(Optional.of(regularUser));
        when(userService.disableUser(2L)).thenReturn(disabledUser);

        mockMvc.perform(post("/api/users/2/disable").with(user("user1").roles("USER")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.enabled", is(false)));
    }

    @Test
        void disableUser_NonAdminCannotDisableOtherUser() throws Exception {
        when(userService.findByUsername("user1")).thenReturn(Optional.of(regularUser));
        when(userService.findById(3L)).thenReturn(Optional.of(targetUser));
        when(userService.disableUser(3L))
            .thenThrow(new AccessDeniedException("You can only disable your own account"));

        mockMvc.perform(post("/api/users/3/disable").with(user("user1").roles("USER")))
            .andExpect(status().isForbidden());
    }

    @Test
        void disableUser_LastAdminShouldBeForbidden() throws Exception {
        when(userService.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(userService.findById(1L)).thenReturn(Optional.of(adminUser));
        when(userService.disableUser(1L))
            .thenThrow(new AccessDeniedException("Cannot disable the last admin account"));

        mockMvc.perform(post("/api/users/1/disable").with(user("admin").roles("ADMIN")))
            .andExpect(status().isForbidden());
    }

    @Test
    void disableUser_UnauthenticatedShouldBeForbidden() throws Exception {
        mockMvc.perform(post("/api/users/1/disable"))
            .andExpect(status().isForbidden());
    }

    // ==================== Last Admin Protection Tests ====================

    @Test
        void updateUser_LastAdminCannotDisableSelfViaEdit() throws Exception {
        UserRequest request = new UserRequest("admin", null, null, "ADMIN", false);

        when(userService.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(userService.findById(1L)).thenReturn(Optional.of(adminUser));
        when(userService.updateUser(any(User.class), any()))
            .thenThrow(new AccessDeniedException("Cannot disable the last admin account"));

        mockMvc.perform(put("/api/users/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)).with(user("admin").roles("ADMIN")))
            .andExpect(status().isForbidden());
    }

    @Test
        void updateUser_LastAdminCannotDisableOtherAdminViaEdit() throws Exception {
        User otherAdmin = new User(2L, "admin2", "encodedPassword", "Other Admin", true, "ADMIN");
        UserRequest request = new UserRequest("admin2", null, null, "ADMIN", false);

        when(userService.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(userService.findById(2L)).thenReturn(Optional.of(otherAdmin));
        when(userService.updateUser(any(User.class), any()))
            .thenThrow(new AccessDeniedException("Cannot disable the last admin account"));

        mockMvc.perform(put("/api/users/2")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)).with(user("admin").roles("ADMIN")))
            .andExpect(status().isForbidden());
    }

    @Test
        void updateUser_NonAdminCanDisableSelfViaEdit() throws Exception {
        UserRequest request = new UserRequest("user1", null, null, null, false);

        when(userService.findByUsername("user1")).thenReturn(Optional.of(regularUser));
        when(userService.findById(2L)).thenReturn(Optional.of(regularUser));
        when(userService.updateUser(any(User.class), any())).thenReturn(regularUser);

        mockMvc.perform(put("/api/users/2")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)).with(user("user1").roles("USER")))
            .andExpect(status().isOk());
    }

    // ==================== POST /api/users/{id}/enable ====================

    @Test
        void enableUser_AdminShouldEnableUser() throws Exception {
        User enabledUser = new User(3L, "user2", "encodedPassword", "Target User", true, "USER");

        when(userService.enableUser(3L)).thenReturn(enabledUser);

        mockMvc.perform(post("/api/users/3/enable").with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.enabled", is(true)));
    }

    @Test
        void enableUser_NonAdminShouldBeForbidden() throws Exception {
        mockMvc.perform(post("/api/users/3/enable").with(user("user1").roles("USER")))
            .andExpect(status().isForbidden());
    }

    @Test
    void enableUser_UnauthenticatedShouldBeForbidden() throws Exception {
        mockMvc.perform(post("/api/users/1/enable"))
            .andExpect(status().isForbidden());
    }

    // ==================== POST /api/users/me/disable ====================

    @Test
        void disableSelf_ShouldWork() throws Exception {
        User disabledUser = new User(2L, "user1", "encodedPassword", "Regular User", false, "USER");

        when(userService.findByUsername("user1")).thenReturn(Optional.of(regularUser));
        when(userService.findById(2L)).thenReturn(Optional.of(regularUser));
        when(userService.disableUser(2L)).thenReturn(disabledUser);

        mockMvc.perform(post("/api/users/me/disable").with(user("user1").roles("USER")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.enabled", is(false)));
    }

    @Test
    void disableSelf_UnauthenticatedShouldBeForbidden() throws Exception {
        mockMvc.perform(post("/api/users/me/disable"))
            .andExpect(status().isForbidden());
    }

    // ==================== DELETE /api/users/{id} ====================

    @Test
        void deleteUser_AdminShouldDeleteAnyUser() throws Exception {
        when(userService.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(userService.findById(3L)).thenReturn(Optional.of(targetUser));
        doNothing().when(userService).deleteUser(3L);

        mockMvc.perform(delete("/api/users/3").with(user("admin").roles("ADMIN")))
            .andExpect(status().isNoContent());
    }

    @Test
        void deleteUser_SelfShouldDeleteSelf() throws Exception {
        when(userService.findByUsername("user1")).thenReturn(Optional.of(regularUser));
        when(userService.findById(2L)).thenReturn(Optional.of(regularUser));
        doNothing().when(userService).deleteUser(2L);

        mockMvc.perform(delete("/api/users/2").with(user("user1").roles("USER")))
            .andExpect(status().isNoContent());
    }

    @Test
        void deleteUser_NonAdminCannotDeleteOtherUser() throws Exception {
        when(userService.findByUsername("user1")).thenReturn(Optional.of(regularUser));
        when(userService.findById(3L)).thenReturn(Optional.of(targetUser));
        doThrow(new AccessDeniedException("You can only delete your own account"))
            .when(userService).deleteUser(3L);

        mockMvc.perform(delete("/api/users/3").with(user("user1").roles("USER")))
            .andExpect(status().isForbidden());
    }

    @Test
        void deleteUser_LastAdminShouldBeForbidden() throws Exception {
        when(userService.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(userService.findById(1L)).thenReturn(Optional.of(adminUser));
        doThrow(new AccessDeniedException("Cannot delete the last admin account"))
            .when(userService).deleteUser(1L);

        mockMvc.perform(delete("/api/users/1").with(user("admin").roles("ADMIN")))
            .andExpect(status().isForbidden());
    }

    @Test
    void deleteUser_UnauthenticatedShouldBeForbidden() throws Exception {
        mockMvc.perform(delete("/api/users/1"))
            .andExpect(status().isForbidden());
    }

    // ==================== DELETE /api/users/me ====================

    @Test
        void deleteSelf_ShouldWork() throws Exception {
        when(userService.findByUsername("user1")).thenReturn(Optional.of(regularUser));
        when(userService.findById(2L)).thenReturn(Optional.of(regularUser));
        doNothing().when(userService).deleteUser(2L);

        mockMvc.perform(delete("/api/users/me").with(user("user1").roles("USER")))
            .andExpect(status().isNoContent());
    }

    @Test
    void deleteSelf_UnauthenticatedShouldBeForbidden() throws Exception {
        mockMvc.perform(delete("/api/users/me"))
            .andExpect(status().isForbidden());
    }

    // ==================== Authorization Tests ====================

    @Test
    void allEndpoints_UnauthenticatedShouldBeForbidden() throws Exception {
        mockMvc.perform(get("/api/users")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/users/me")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/users/1")).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/users")).andExpect(status().isForbidden());
        mockMvc.perform(put("/api/users/1")).andExpect(status().isForbidden());
        mockMvc.perform(put("/api/users/me")).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/users/1/disable")).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/users/1/enable")).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/users/me/disable")).andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/users/1")).andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/users/me")).andExpect(status().isForbidden());
    }

    @Test
        void userEndpoints_NonAdminAccessTests() throws Exception {
        when(userService.findByUsername("user1")).thenReturn(Optional.of(regularUser));
        mockMvc.perform(get("/api/users").with(user("user1").roles("USER"))).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/users/3").with(user("user1").roles("USER"))).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/users").with(user("user1").roles("USER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UserRequest("user2", "password123", "Target User", null, true))))
            .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/users/3").with(user("user1").roles("USER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UserRequest("user2", null, null, null, null))))
            .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/users/3/disable").with(user("user1").roles("USER"))).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/users/3/enable").with(user("user1").roles("USER"))).andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/users/3").with(user("user1").roles("USER"))).andExpect(status().isForbidden());
    }

    @Test
        void userEndpoints_SelfAccessTests() throws Exception {
        when(userService.findByUsername("user1")).thenReturn(Optional.of(regularUser));
        when(userService.findById(2L)).thenReturn(Optional.of(regularUser));
        when(userService.updateUser(any(User.class), any())).thenReturn(regularUser);
        when(userService.disableUser(2L)).thenReturn(regularUser);
        doNothing().when(userService).deleteUser(2L);

        mockMvc.perform(get("/api/users/me").with(user("user1").roles("USER"))).andExpect(status().isOk());
        mockMvc.perform(get("/api/users/2").with(user("user1").roles("USER"))).andExpect(status().isOk());
        mockMvc.perform(put("/api/users/me").with(user("user1").roles("USER"))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new UserRequest.SelfUpdateRequest(null, null, null)))).andExpect(status().isOk());
        mockMvc.perform(post("/api/users/me/disable").with(user("user1").roles("USER"))).andExpect(status().isOk());
        mockMvc.perform(delete("/api/users/me").with(user("user1").roles("USER"))).andExpect(status().isNoContent());
    }

    @Test
        void adminEndpoints_FullAccessTests() throws Exception {
        when(userService.getAllUsers()).thenReturn(List.of(regularUser, targetUser));
        when(userService.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(userService.findById(3L)).thenReturn(Optional.of(targetUser));
        when(userService.createUser(any(User.class))).thenReturn(targetUser);
        when(userService.updateUser(any(User.class), any())).thenReturn(targetUser);
        when(userService.disableUser(3L)).thenReturn(targetUser);
        when(userService.enableUser(3L)).thenReturn(targetUser);
        doNothing().when(userService).deleteUser(3L);

        mockMvc.perform(get("/api/users").with(user("admin").roles("ADMIN"))).andExpect(status().isOk());
        mockMvc.perform(get("/api/users/3").with(user("admin").roles("ADMIN"))).andExpect(status().isOk());
        mockMvc.perform(post("/api/users").with(user("admin").roles("ADMIN"))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new UserRequest("user2", "password123", "Target User", "USER", true)))).andExpect(status().isOk());
        mockMvc.perform(put("/api/users/3").with(user("admin").roles("ADMIN"))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new UserRequest("user2", null, null, "USER", true)))).andExpect(status().isOk());
        mockMvc.perform(post("/api/users/3/disable").with(user("admin").roles("ADMIN"))).andExpect(status().isOk());
        mockMvc.perform(post("/api/users/3/enable").with(user("admin").roles("ADMIN"))).andExpect(status().isOk());
        mockMvc.perform(delete("/api/users/3").with(user("admin").roles("ADMIN"))).andExpect(status().isNoContent());
    }
}
