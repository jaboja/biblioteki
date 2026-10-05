package pl.jaboja.biblioteki.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Configuration for authentication setup.
 * Initializes default admin user if none exists.
 */
@Configuration
@RequiredArgsConstructor
public class AuthConfig {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner initDefaultUsers() {
        return args -> {
            if (userRepository.count() == 0) {
                User admin = new User();
                admin.setUsername("admin");
                admin.setPassword(passwordEncoder.encode("admin123"));
                admin.setFullName("Administrator");
                admin.setEnabled(true);
                admin.setRoles("ADMIN,USER");
                userRepository.save(admin);
                
                User user = new User();
                user.setUsername("user");
                user.setPassword(passwordEncoder.encode("user123"));
                user.setFullName("Regular User");
                user.setEnabled(true);
                user.setRoles("USER");
                userRepository.save(user);
                
                System.out.println("Created default users: admin/admin123, user/user123");
            }
        };
    }
}
