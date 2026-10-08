package pl.jaboja.biblioteki.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for user management operations.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User management")
public class UserController {

    private final UserService userService;

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            return (User) authentication.getPrincipal();
        }
        throw new AccessDeniedException("Not authenticated");
    }

    private User getCurrentUserFromDatabase() {
        User currentUser = getCurrentUser();
        return userService.findByUsername(currentUser.getUsername())
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    @Operation(summary = "Get all users", description = "Available only for administrators")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<User> users = userService.getAllActiveUsers();
        List<UserResponse> responses = users.stream()
            .map(this::toUserResponse)
            .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "Get current user profile", description = "Returns profile of logged in user")
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUserProfile() {
        User user = getCurrentUserFromDatabase();
        return ResponseEntity.ok(toUserResponse(user));
    }

    @Operation(summary = "Get user by ID", description = "Available only for administrators")
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        User user = userService.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                org.springframework.http.HttpStatus.NOT_FOUND, "User not found"));
        return ResponseEntity.ok(toUserResponse(user));
    }

    @Operation(summary = "Create a new user", description = "Available only for administrators")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> createUser(@RequestBody UserRequest.AdminRequest request) {
        User currentUser = getCurrentUserFromDatabase();
        
        User user = new User();
        user.setUsername(request.username());
        user.setPassword(request.password());
        user.setFullName(request.fullName());
        user.setRoles(request.roles() != null ? request.roles() : "USER");
        user.setEnabled(request.enabled() != null ? request.enabled() : true);
        user.setDeleted(false);
        
        User created = userService.createUser(user, currentUser);
        return ResponseEntity.ok(toUserResponse(created));
    }

    @Operation(summary = "Update user", description = "Admin can update any user, user can update self")
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long id,
            @RequestBody UserRequest.AdminRequest request) {
        
        User currentUser = getCurrentUserFromDatabase();
        
        User updatedUser = new User();
        updatedUser.setUsername(request.username());
        updatedUser.setPassword(request.password());
        updatedUser.setFullName(request.fullName());
        updatedUser.setRoles(request.roles());
        updatedUser.setEnabled(request.enabled());
        
        User result = userService.updateUser(id, updatedUser, currentUser);
        return ResponseEntity.ok(toUserResponse(result));
    }

    @Operation(summary = "Update self", description = "User can update own data: username, password, fullName")
    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateSelf(@RequestBody UserRequest.SelfUpdateRequest request) {
        User currentUser = getCurrentUserFromDatabase();
        
        User updatedUser = new User();
        updatedUser.setUsername(request.username());
        updatedUser.setPassword(request.password());
        updatedUser.setFullName(request.fullName());
        
        User result = userService.updateUser(currentUser.getId(), updatedUser, currentUser);
        return ResponseEntity.ok(toUserResponse(result));
    }

    @Operation(summary = "Soft delete user", description = "Disables user account (soft delete)")
    @DeleteMapping("/{id}/soft")
    public ResponseEntity<Void> softDeleteUser(@PathVariable Long id) {
        User currentUser = getCurrentUserFromDatabase();
        userService.deleteUserSoft(id, currentUser);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Soft delete self", description = "Disables own account")
    @DeleteMapping("/me/soft")
    public ResponseEntity<Void> softDeleteSelf() {
        User currentUser = getCurrentUserFromDatabase();
        userService.deleteUserSoft(currentUser.getId(), currentUser);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Hard delete user", description = "Completely removes user from database")
    @DeleteMapping("/{id}/hard")
    public ResponseEntity<Void> hardDeleteUser(@PathVariable Long id) {
        User currentUser = getCurrentUserFromDatabase();
        userService.deleteUserHard(id, currentUser);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Hard delete self", description = "Completely removes own account from database")
    @DeleteMapping("/me/hard")
    public ResponseEntity<Void> hardDeleteSelf() {
        User currentUser = getCurrentUserFromDatabase();
        userService.deleteUserHard(currentUser.getId(), currentUser);
        return ResponseEntity.noContent().build();
    }

    private UserResponse toUserResponse(User user) {
        return new UserResponse(
            user.getId(),
            user.getUsername(),
            user.getFullName(),
            user.isEnabled(),
            user.getRoles(),
            user.isDeleted()
        );
    }

    public record UserResponse(
        Long id,
        String username,
        String fullName,
        boolean enabled,
        String roles,
        boolean deleted
    ) {}
}
