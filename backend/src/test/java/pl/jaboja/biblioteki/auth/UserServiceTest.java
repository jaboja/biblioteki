package pl.jaboja.biblioteki.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User adminUser;
    private User regularUser;
    private User targetUser;

    @BeforeEach
    void setUp() {
        adminUser = new User(1L, "admin", "encodedPassword", "Admin User", true, "ADMIN");
        regularUser = new User(2L, "user1", "encodedPassword", "Regular User", true, "USER");
        targetUser = new User(3L, "user2", "encodedPassword", "Target User", true, "USER");
    }

    @Test
    void findByUsername_ShouldReturnUser() {
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(regularUser));

        Optional<User> result = userService.findByUsername("user1");

        assertTrue(result.isPresent());
        assertEquals("user1", result.get().getUsername());
    }

    @Test
    void findByUsername_ShouldReturnEmpty() {
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());

        Optional<User> result = userService.findByUsername("nonexistent");

        assertTrue(result.isEmpty());
    }

    @Test
    void findById_ShouldReturnUser() {
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(regularUser));

        Optional<User> result = userService.findById(2L);

        assertTrue(result.isPresent());
        assertEquals(2L, result.get().getId());
    }

    @Test
    void getAllUsers_ShouldReturnAllUsers() {
        when(userRepository.findAll()).thenReturn(List.of(regularUser, targetUser));

        List<User> result = userService.getAllUsers();

        assertEquals(2, result.size());
    }

    @Test
    void createUser_ShouldEncodePasswordAndSave() {
        User newUser = new User("newuser", "password123", "New User", true, "USER");
        when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(newUser);

        User created = userService.createUser(newUser);

        assertNotNull(created);
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createUser_WithExistingUsernameShouldThrow() {
        User newUser = new User("user1", "password123", "New User", true, "USER");
        when(userRepository.findByUsername("user1")).thenReturn(Optional.of(regularUser));

        assertThrows(IllegalArgumentException.class, () -> {
            userService.createUser(newUser);
        });

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateUser_ShouldEncodePasswordIfProvided() {
        User user = new User(2L, "user1", "oldPassword", "Regular User", true, "USER");
        when(userRepository.save(any(User.class))).thenReturn(user);

        User result = userService.updateUser(user, "newPassword");

        assertNotNull(result);
        verify(passwordEncoder).encode("newPassword");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void updateUser_ShouldNotEncodePasswordIfNull() {
        User user = new User(2L, "user1", "oldPassword", "Regular User", true, "USER");
        when(userRepository.save(any(User.class))).thenReturn(user);

        User result = userService.updateUser(user, null);

        assertNotNull(result);
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void updateUser_LastAdminCannotDisableSelf() {
        User lastAdmin = new User(1L, "admin", "encodedPassword", "Admin User", false, "ADMIN");
        lenient().when(userRepository.save(any(User.class))).thenReturn(lastAdmin);
        when(userRepository.countByRolesContainingAndEnabledTrue("ADMIN")).thenReturn(1L);

        assertThrows(AccessDeniedException.class, () -> {
            userService.updateUser(lastAdmin, null);
        });

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateUser_LastAdminCannotDisableOtherAdmin() {
        User lastAdmin = new User(1L, "admin", "encodedPassword", "Admin User", true, "ADMIN");
        User otherAdmin = new User(2L, "admin2", "encodedPassword", "Other Admin", false, "ADMIN");
        lenient().when(userRepository.save(any(User.class))).thenReturn(otherAdmin);
        when(userRepository.countByRolesContainingAndEnabledTrue("ADMIN")).thenReturn(1L);

        assertThrows(AccessDeniedException.class, () -> {
            userService.updateUser(otherAdmin, null);
        });

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateUser_NonAdminCanDisableSelf() {
        User user = new User(2L, "user1", "encodedPassword", "Regular User", false, "USER");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userRepository.countByRolesContainingAndEnabledTrue("ADMIN")).thenReturn(1L);

        User result = userService.updateUser(user, null);
        
        assertNotNull(result);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void disableUser_ShouldSetEnabledToFalse() {
        when(userRepository.findById(3L)).thenReturn(Optional.of(targetUser));
        when(userRepository.save(any(User.class))).thenReturn(targetUser);

        User result = userService.disableUser(3L);

        assertNotNull(result);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void disableUser_NonExistentUserShouldThrow() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> {
            userService.disableUser(999L);
        });
    }

    @Test
    void disableUser_AlreadyDisabledShouldThrow() {
        User disabledUser = new User(3L, "user2", "encodedPassword", "Target User", false, "USER");
        when(userRepository.findById(3L)).thenReturn(Optional.of(disabledUser));

        assertThrows(UsernameNotFoundException.class, () -> {
            userService.disableUser(3L);
        });
    }

    @Test
    void disableUser_LastAdminShouldThrow() {
        User lastAdmin = new User(1L, "admin", "encodedPassword", "Admin User", true, "ADMIN");
        when(userRepository.findById(1L)).thenReturn(Optional.of(lastAdmin));
        when(userRepository.countByRolesContainingAndEnabledTrue("ADMIN")).thenReturn(1L);

        assertThrows(AccessDeniedException.class, () -> {
            userService.disableUser(1L);
        });

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void enableUser_ShouldSetEnabledToTrue() {
        User disabledUser = new User(3L, "user2", "encodedPassword", "Target User", false, "USER");
        when(userRepository.findById(3L)).thenReturn(Optional.of(disabledUser));
        when(userRepository.save(any(User.class))).thenReturn(disabledUser);

        User result = userService.enableUser(3L);

        assertNotNull(result);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void enableUser_NonExistentUserShouldThrow() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> {
            userService.enableUser(999L);
        });
    }

    @Test
    void deleteUser_ShouldDeleteFromDatabase() {
        when(userRepository.findById(3L)).thenReturn(Optional.of(targetUser));

        assertDoesNotThrow(() -> {
            userService.deleteUser(3L);
        });

        verify(userRepository).delete(any(User.class));
    }

    @Test
    void deleteUser_NonExistentUserShouldThrow() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> {
            userService.deleteUser(999L);
        });
    }

    @Test
    void deleteUser_LastAdminShouldThrow() {
        User lastAdmin = new User(1L, "admin", "encodedPassword", "Admin User", true, "ADMIN");
        when(userRepository.findById(1L)).thenReturn(Optional.of(lastAdmin));
        when(userRepository.countByRolesContainingAndEnabledTrue("ADMIN")).thenReturn(1L);

        assertThrows(AccessDeniedException.class, () -> {
            userService.deleteUser(1L);
        });

        verify(userRepository, never()).delete(any(User.class));
    }

    @Test
    void isLastAdmin_ShouldReturnTrueWhenOnlyOneAdmin() {
        when(userRepository.countByRolesContainingAndEnabledTrue("ADMIN")).thenReturn(1L);

        boolean result = userService.isLastAdmin(adminUser);

        assertTrue(result);
    }

    @Test
    void isLastAdmin_ShouldReturnFalseWhenMultipleAdmins() {
        when(userRepository.countByRolesContainingAndEnabledTrue("ADMIN")).thenReturn(2L);

        boolean result = userService.isLastAdmin(adminUser);

        assertFalse(result);
    }

    @Test
    void isLastAdmin_ShouldReturnFalseWhenUserIsNotAdmin() {
        boolean result = userService.isLastAdmin(regularUser);

        assertFalse(result);
    }

    @Test
    void isLastAdmin_ShouldReturnFalseWhenUserIsNull() {
        boolean result = userService.isLastAdmin(null);

        assertFalse(result);
    }

    @Test
    void loadUserByUsername_ShouldReturnUser() {
        when(userRepository.findByUsername("user1")).thenReturn(Optional.of(regularUser));

        UserDetails result = userService.loadUserByUsername("user1");

        assertNotNull(result);
        assertEquals("user1", result.getUsername());
    }

    @Test
    void loadUserByUsername_ShouldThrowWhenNotFound() {
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> {
            userService.loadUserByUsername("nonexistent");
        });
    }

    @Test
    void authenticate_ShouldReturnUserWhenCredentialsValid() {
        when(userRepository.findByUsername("user1")).thenReturn(Optional.of(regularUser));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);

        User result = userService.authenticate("user1", "password123");

        assertNotNull(result);
        assertEquals("user1", result.getUsername());
    }

    @Test
    void authenticate_ShouldThrowWhenUserNotFound() {
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        assertThrows(org.springframework.security.authentication.BadCredentialsException.class, () -> {
            userService.authenticate("nonexistent", "password123");
        });
    }

    @Test
    void authenticate_ShouldThrowWhenPasswordInvalid() {
        when(userRepository.findByUsername("user1")).thenReturn(Optional.of(regularUser));
        when(passwordEncoder.matches("wrongpassword", "encodedPassword")).thenReturn(false);

        assertThrows(org.springframework.security.authentication.BadCredentialsException.class, () -> {
            userService.authenticate("user1", "wrongpassword");
        });
    }

    @Test
    void authenticate_ShouldThrowWhenUserDisabled() {
        User disabledUser = new User(3L, "user2", "encodedPassword", "Target User", false, "USER");
        when(userRepository.findByUsername("user2")).thenReturn(Optional.of(disabledUser));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);

        assertThrows(org.springframework.security.authentication.BadCredentialsException.class, () -> {
            userService.authenticate("user2", "password123");
        });
    }
}
