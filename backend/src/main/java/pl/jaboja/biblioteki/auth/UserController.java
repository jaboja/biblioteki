package pl.jaboja.biblioteki.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * REST controller for user management operations.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Validated
@Tag(name = "User Management", description = "Zarządzanie użytkownikami systemu")
public class UserController {

    private final UserService userService;

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            var principal = authentication.getPrincipal();
            if (principal instanceof User && ((User) principal).isEnabled()) {
                return (User) principal;
            }
        }
        throw new AccessDeniedException("Not authenticated");
    }

    private Long getCurrentUserId() {
        return getCurrentUser().getId();
    }

    private User getCurrentUserFromDatabase() {
        User currentUser = getCurrentUser();
        return userService.findByUsername(currentUser.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    private boolean isNeitherAdminNorSelf(Long id) {
        User currentUser = getCurrentUserFromDatabase();

        boolean isAdmin = currentUser.getRoles().contains("ADMIN");
        boolean isSelf = currentUser.getId().equals(id);

        return !(isAdmin || isSelf);
    }

    /**
     * Get all users - admin only
     */
    @Operation(
        summary = "Pobierz wszystkich użytkowników",
        description = "Tylko dla administratorów"
    )
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        List<UserResponse> responses = users.stream()
            .map(this::toUserResponse)
            .toList();
        return ResponseEntity.ok(responses);
    }

    /**
     * Get user by ID
     */
    @Operation(
        summary = "Pobierz użytkownika po ID",
        description = "Użytkownik może pobrać swoje własne dane, administrator może pobrać dowolne"
    )
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(
            @Parameter(description = "ID użytkownika")
            @PathVariable Long id) {
        if (isNeitherAdminNorSelf(id)) {
            throw new AccessDeniedException("You can only access your own user information");
        }

        User user = userService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND, "User not found"));
        return ResponseEntity.ok(this.toUserResponse(user));
    }

    /**
     * Get current user profile
     */
    @Operation(
        summary = "Pobierz własny profil",
        description = "Zwraca dane aktualnie zalogowanego użytkownika"
    )
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUserProfile() {
        User user = getCurrentUserFromDatabase();
        return ResponseEntity.ok(toUserResponse(user));
    }

    /**
     * Create new user - admin only
     */
    @Operation(
        summary = "Utwórz nowego użytkownika",
        description = "Tylko administrator może tworzyć nowych użytkowników"
    )
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> createUser(
            @Parameter(description = "Dane nowego użytkownika")
            @RequestBody UserRequest request) {
        User user = new User();
        user.setUsername(request.username());
        user.setPassword(request.password());
        user.setFullName(request.fullName());
        user.setEnabled(request.enabled() == null || request.enabled());

        // Set roles - default to USER if not specified
        if (request.roles() != null && !request.roles().isEmpty()) {
            user.setRoles(request.roles());
        } else {
            user.setRoles("USER");
        }

        User created = userService.createUser(user);
        return ResponseEntity.ok(toUserResponse(created));
    }

    /**
     * Update user - user can update themselves, admin can update anyone
     */
    @Operation(
        summary = "Zaktualizuj użytkownika",
        description = "Użytkownik może zaktualizować swój własny profil (username, password, fullName). Administrator może zaktualizować dowolnego użytkownika. Administrator może dodatkowo zaktualizować role i status konta."
    )
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @Parameter(description = "ID użytkownika")
            @PathVariable Long id,
            @Parameter(description = "Dane do aktualizacji")
            @RequestBody UserRequest request) {
        User user = getCurrentUserFromDatabase();

        boolean isAdmin = user.getRoles().contains("ADMIN");
        boolean isSelf = user.getId().equals(id);

        if (!(isAdmin || isSelf)) {
            throw new AccessDeniedException("You can only update your own profile");
        }

        if (request.username() != null) {
            user.setUsername(request.username());
        }
        if (request.fullName() != null) {
            user.setFullName(request.fullName());
        }
        if (request.enabled() != null) {
            user.setEnabled(request.enabled());
        }
        if (request.roles() != null) {
            if (!isAdmin) {
                throw new AccessDeniedException("You cannot update your own roles");
            }
            user.setRoles(request.roles());
        }

        User result = userService.updateUser(user, request.password());
        return ResponseEntity.ok(toUserResponse(result));
    }

    /**
     * Update own profile - limited to username, password, fullName
     */
    @Operation(
        summary = "Zaktualizuj własny profil",
        description = "Tylko username, password i fullName mogą być zaktualizowane"
    )
    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateSelf(
            @Parameter(description = "Dane do aktualizacji")
            @RequestBody UserRequest.SelfUpdateRequest request) {
        User user = getCurrentUserFromDatabase();
        if (request.username() != null) {
            user.setUsername(request.username());
        }
        if (request.fullName() != null) {
            user.setFullName(request.fullName());
        }
        User result = userService.updateUser(user, request.password());
        return ResponseEntity.ok(toUserResponse(result));
    }

    /**
     * Disable user
     */
    @Operation(
        summary = "Dezaktywuj użytkownika",
        description = "Użytkownik może dezaktywować swoje własne konto. Administrator może deaktywować dowolne konto. Nie można deaktywować ostatniego konta administratora."
    )
    @PostMapping("/{id}/disable")
    public ResponseEntity<UserResponse> disableUser(
            @Parameter(description = "ID użytkownika")
            @PathVariable Long id) {
        if (isNeitherAdminNorSelf(id)) {
            throw new AccessDeniedException("You can only disable your own account");
        }

        User disabledUser = userService.disableUser(id);
        return ResponseEntity.ok(toUserResponse(disabledUser));
    }

    /**
     * Enable user - admin only
     */
    @Operation(
        summary = "Aktywuj użytkownika",
        description = "Tylko administrator może aktywować użytkowników"
    )
    @PostMapping("/{id}/enable")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> enableUser(
            @Parameter(description = "ID użytkownika")
            @PathVariable Long id) {
        User enabledUser = userService.enableUser(id);
        return ResponseEntity.ok(toUserResponse(enabledUser));
    }

    /**
     * Soft delete own account
     */
    @Operation(
        summary = "Dezaktywuj własne konto",
        description = "Dezaktywacja własnego konta (możliwe ponowne aktywowanie przez admin)"
    )
    @PostMapping("/me/disable")
    public ResponseEntity<UserResponse> disableSelf() {
        Long currentUserId = getCurrentUserId();
        User disabledUser = userService.disableUser(currentUserId);
        return ResponseEntity.ok(toUserResponse(disabledUser));
    }

    /**
     * Hard delete user
     */
    @Operation(
        summary = "Usun użytkownika (trwałe usunięcie)",
        description = "Użytkownik może usunąć swoje własne konto. Administrator może usunąć dowolne konto. Nie można usunąć ostatniego konta administratora."
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(
            @Parameter(description = "ID użytkownika")
            @PathVariable Long id) {
        if (isNeitherAdminNorSelf(id)) {
            throw new AccessDeniedException("You can only delete your own account");
        }

        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Delete own account
     */
    @Operation(
        summary = "Usun własne konto",
        description = "Trwałe usunięcie własnego konta"
    )
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteSelf() {
        Long currentUserId = getCurrentUserId();
        userService.deleteUser(currentUserId);
        return ResponseEntity.noContent().build();
    }

    private UserResponse toUserResponse(User user) {
        return new UserResponse(
            user.getId(),
            user.getUsername(),
            user.getFullName(),
            user.isEnabled(),
            user.getRoles()
        );
    }
}
