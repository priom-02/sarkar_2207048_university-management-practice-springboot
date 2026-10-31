package com.example.university_management.config;

import com.example.university_management.entity.Admin;
import com.example.university_management.repository.AdminRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AdminInitializer {

    private static final Logger log = LoggerFactory.getLogger(AdminInitializer.class);

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminInitializer(AdminRepository adminRepository, PasswordEncoder passwordEncoder) {
        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Bean
    public CommandLineRunner initializeAdmin() {
        return args -> {
            if (adminRepository.count() > 0) {
                log.info("Admin account already exists. Skipping initialization.");
                return;
            }

            String adminName = System.getenv("ADMIN_NAME");
            String adminEmail = System.getenv("ADMIN_EMAIL");
            String adminPassword = System.getenv("ADMIN_PASSWORD");

            if (adminName == null || adminName.isBlank() ||
                adminEmail == null || adminEmail.isBlank() ||
                adminPassword == null || adminPassword.isBlank()) {
                throw new IllegalStateException(
                    "Cannot initialize Admin account: Environment variables ADMIN_NAME, ADMIN_EMAIL, " +
                    "and ADMIN_PASSWORD must all be defined and non-empty."
                );
            }

            Admin admin = Admin.builder()
                    .name(adminName.trim())
                    .email(adminEmail.trim())
                    .password(passwordEncoder.encode(adminPassword))
                    .build();

            adminRepository.save(admin);
            log.info("Initial Admin account successfully initialized for email: {}", adminEmail.trim());
        };
    }
}

