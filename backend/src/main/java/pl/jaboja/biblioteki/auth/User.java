package pl.jaboja.biblioteki.auth;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.stream.Stream;

/**
 * User entity for authentication.
 */
@Entity
@Table(name = "app_users")
@Getter
@Setter
public class User implements UserDetails {
    
    public User() {}  // No-arg constructor for JPA

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(name = "full_name")
    private String fullName;

    @Column
    private boolean enabled = true;

    @Column
    private String roles = "USER";  // Comma-separated roles

    @Override
    @NullMarked
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Stream.of(roles.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
            .toList();
    }

    // All-args constructor for convenience
    public User(Long id, String username, String password, String fullName, boolean enabled, String roles) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.enabled = enabled;
        this.roles = roles;
    }

    // Constructor without id (for creating new users)
    public User(String username, String password, String fullName, boolean enabled, String roles) {
        this(null, username, password, fullName, enabled, roles);
    }
}
