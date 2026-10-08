package pl.jaboja.biblioteki.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for User entities.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findById(Long id);
    
    long countByRolesContainingAndEnabledTrue(String role);
    long countByDeletedFalse();
    
    List<User> findByDeletedFalse();
    List<User> findByRolesContainingAndDeletedFalse(String role);
}
