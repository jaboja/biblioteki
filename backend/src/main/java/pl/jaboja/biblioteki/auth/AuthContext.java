package pl.jaboja.biblioteki.auth;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

/**
 * Kontekst autentykacji - pozwala na pobranie aktualnie zalogowanego użytkownika.
 */
@Component
public class AuthContext {

    /**
     * Zwraca aktualnie zalogowanego użytkownika.
     *
     * @return UserDetails lub null jeśli nikt nie jest zalogowany
     */
    public UserDetails getCurrentUserDetails() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails) {
            return (UserDetails) authentication.getPrincipal();
        }
        return null;
    }

    /**
     * Zwraca encję User dla aktualnie zalogowanego użytkownika.
     *
     * @param userService serwis użytkowników
     * @return User lub null jeśli nikt nie jest zalogowany
     */
    public User getCurrentUser(UserService userService) {
        UserDetails userDetails = getCurrentUserDetails();
        if (userDetails != null) {
            return userService.findByUsername(userDetails.getUsername()).orElse(null);
        }
        return null;
    }

    /**
     * Sprawdza czy użytkownik jest zalogowany.
     *
     * @return true jeśli użytkownik jest zalogowany
     */
    public boolean isAuthenticated() {
        return getCurrentUserDetails() != null;
    }
}
