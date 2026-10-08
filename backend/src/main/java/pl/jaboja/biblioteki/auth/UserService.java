package pl.jaboja.biblioteki.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    public User authenticate(String username, String password) {
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

    public User createUser(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    public java.util.Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public List<User> getAllUsers() {
        return userRepository.findByDeletedFalse();
    }

    public User createUser(User user, User createdBy) {
        if (!createdBy.getRoles().contains("ADMIN")) {
            throw new AccessDeniedException("Only admins can create users");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setDeleted(false);
        return userRepository.save(user);
    }

    public User updateUser(Long id, User updatedUser, User requester) {
        User existingUser = userRepository.findById(id)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (existingUser.isDeleted()) {
            throw new UsernameNotFoundException("User has been deleted");
        }

        boolean isAdmin = requester.getRoles().contains("ADMIN");
        boolean isSelf = requester.getId().equals(id);

        if (!isAdmin && !isSelf) {
            throw new AccessDeniedException("You can only update your own profile");
        }

        if (isSelf && !isAdmin) {
            existingUser.setUsername(updatedUser.getUsername() != null ? updatedUser.getUsername() : existingUser.getUsername());
            existingUser.setFullName(updatedUser.getFullName() != null ? updatedUser.getFullName() : existingUser.getFullName());
            if (updatedUser.getPassword() != null && !updatedUser.getPassword().isEmpty()) {
                existingUser.setPassword(passwordEncoder.encode(updatedUser.getPassword()));
            }
        } else {
            existingUser.setUsername(updatedUser.getUsername() != null ? updatedUser.getUsername() : existingUser.getUsername());
            existingUser.setFullName(updatedUser.getFullName() != null ? updatedUser.getFullName() : existingUser.getFullName());
            existingUser.setRoles(updatedUser.getRoles() != null ? updatedUser.getRoles() : existingUser.getRoles());
            existingUser.setEnabled(updatedUser.isEnabled());
            if (updatedUser.getPassword() != null && !updatedUser.getPassword().isEmpty()) {
                existingUser.setPassword(passwordEncoder.encode(updatedUser.getPassword()));
            }
        }

        return userRepository.save(existingUser);
    }

    public void deleteUserHard(Long id, User requester) {
        User userToDelete = userRepository.findById(id)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (userToDelete.isDeleted()) {
            throw new UsernameNotFoundException("User already deleted");
        }

        if (!requester.getRoles().contains("ADMIN") && !requester.getId().equals(id)) {
            throw new AccessDeniedException("You can only delete yourself");
        }

        if (userToDelete.getRoles().contains("ADMIN")) {
            long adminCount = userRepository.countByRolesContainingAndEnabledTrue("ADMIN");
            if (adminCount <= 1) {
                throw new AccessDeniedException("Cannot delete the last admin account");
            }
        }

        userToDelete.setDeleted(true);
        userRepository.delete(userToDelete);
    }

    public void deleteUserSoft(Long id, User requester) {
        User userToDelete = userRepository.findById(id)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (userToDelete.isDeleted()) {
            throw new UsernameNotFoundException("User already deleted");
        }

        if (!requester.getRoles().contains("ADMIN") && !requester.getId().equals(id)) {
            throw new AccessDeniedException("You can only delete yourself");
        }

        if (userToDelete.getRoles().contains("ADMIN")) {
            long adminCount = userRepository.countByRolesContainingAndEnabledTrue("ADMIN");
            if (adminCount <= 1) {
                throw new AccessDeniedException("Cannot disable the last admin account");
            }
        }

        userToDelete.setEnabled(false);
        userRepository.save(userToDelete);
    }

    public boolean isLastAdmin(User user) {
        if (!user.getRoles().contains("ADMIN")) {
            return false;
        }
        long adminCount = userRepository.countByRolesContainingAndEnabledTrue("ADMIN");
        return adminCount <= 1;
    }

    public List<User> getAllActiveUsers() {
        return userRepository.findByDeletedFalse();
    }
}
