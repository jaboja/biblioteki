package pl.jaboja.biblioteki.auth;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.List;

/**
 * Service for user authentication and management.
 */
@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @NullMarked
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    public User authenticate(String username, String password) throws BadCredentialsException {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BadCredentialsException("Invalid username or password");
        }

        if (!user.isEnabled()) {
            throw new BadCredentialsException("User is disabled");
        }

        return user;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public java.util.Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    /**
     * Create new user
     */
    public User createUser(User user) {
        // Check if username already exists
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }

        // Encode password and set default values
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    /**
     * Update user
     */
    @Transactional
    public User updateUser(User user, String password) {
        if (password != null) {
            user.setPassword(passwordEncoder.encode(password));
        }
        
        // Check if this is the last admin being disabled
        if (user.getRoles().contains("ADMIN") && !user.isEnabled()) {
            if (isLastAdmin(user)) {
                throw new AccessDeniedException("Cannot disable the last admin account");
            }
        }
        
        return userRepository.save(user);
    }

    /**
     * Disable user
     */
    @Transactional
    public User disableUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + id));

        if (!user.isEnabled()) {
            throw new UsernameNotFoundException("User is already disabled");
        }

        // Check if this is the last admin
        if (isLastAdmin(user)) {
            throw new AccessDeniedException("Cannot disable the last admin account");
        }

        user.setEnabled(false);
        return userRepository.save(user);
    }

    /**
     * Enable user
     */
    @Transactional
    public User enableUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + id));

        user.setEnabled(true);
        return userRepository.save(user);
    }

    /**
     * Delete user from database
     */
    public void deleteUser(Long id) {
        User userToDelete = userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // Check if this is the last admin
        if (isLastAdmin(userToDelete)) {
            throw new AccessDeniedException("Cannot delete the last admin account");
        }

        userRepository.delete(userToDelete);
    }

    public boolean isLastAdmin(User user) {
        if (user == null) return false;

        // Check if this user is admin
        boolean isAdmin = user.getRoles().contains("ADMIN");
        if (!isAdmin) return false;

        // Count all admin users
        long adminCount = userRepository.countByRolesContainingAndEnabledTrue("ADMIN");
        return adminCount <= 1;
    }
}
