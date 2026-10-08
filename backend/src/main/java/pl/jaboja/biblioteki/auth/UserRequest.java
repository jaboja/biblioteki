package pl.jaboja.biblioteki.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO for user creation and update requests.
 */
public record UserRequest(
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    String username,

    @Size(min = 6, message = "Password must be at least 6 characters")
    String password,

    @Size(max = 100, message = "Full name must be at most 100 characters")
    String fullName,

    String roles,

    Boolean enabled
) {
    // For user self-update (limited fields)
    public record SelfUpdateRequest(
        @Size(min = 6, message = "Password must be at least 6 characters")
        String password,

        @Size(max = 100, message = "Full name must be at most 100 characters")
        String fullName,

        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        String username
    ) {}

    // For admin create/update (full access)
    public record AdminRequest(
        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        String username,

        @NotBlank(message = "Password is required")
        @Size(min = 6, message = "Password must be at least 6 characters")
        String password,

        @Size(max = 100, message = "Full name must be at most 100 characters")
        String fullName,

        String roles,

        Boolean enabled
    ) {}
}
