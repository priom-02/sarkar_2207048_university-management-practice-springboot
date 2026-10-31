package com.example.university_management.config;

import com.example.university_management.entity.Admin;
import com.example.university_management.entity.Student;
import com.example.university_management.entity.Teacher;
import com.example.university_management.repository.AdminRepository;
import com.example.university_management.repository.StudentRepository;
import com.example.university_management.repository.TeacherRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

import java.util.Optional;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public UserDetailsService userDetailsService(AdminRepository adminRepository,
                                                 TeacherRepository teacherRepository,
                                                 StudentRepository studentRepository) {
        return email -> {
            Optional<Admin> admin = adminRepository.findByEmail(email);
            if (admin.isPresent()) {
                return User.builder()
                        .username(admin.get().getEmail())
                        .password(admin.get().getPassword())
                        .roles("ADMIN")
                        .build();
            }

            Optional<Teacher> teacher = teacherRepository.findByEmail(email);
            if (teacher.isPresent()) {
                return User.builder()
                        .username(teacher.get().getEmail())
                        .password(teacher.get().getPassword())
                        .roles("TEACHER")
                        .build();
            }

            Optional<Student> student = studentRepository.findByEmail(email);
            if (student.isPresent()) {
                return User.builder()
                        .username(student.get().getEmail())
                        .password(student.get().getPassword())
                        .roles("STUDENT")
                        .build();
            }

            throw new UsernameNotFoundException("User not found with email: " + email);
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF for REST APIs so Postman and HTTP clients can interact freely
            .csrf(csrf -> csrf.disable())
            // Custom authentication entry point to return 401 with a helpful message when not logged in
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Authentication required. Please log in first via /api/auth/login.\"}");
                })
            )
            // Configure session-based SecurityContext repository
            .securityContext(securityContext -> securityContext
                .securityContextRepository(securityContextRepository())
            )
            // Session creation policy: create session when required for authenticated users
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
            )
            // Role-Based Access Control (RBAC) authorization rules
            .authorizeHttpRequests(auth -> auth
                // Public authentication endpoints (login, logout)
                .requestMatchers("/api/auth/**").permitAll()

                // Admin management endpoints (ROLE_ADMIN only)
                .requestMatchers("/api/admin", "/api/admin/**", "/api/admins", "/api/admins/**").hasRole("ADMIN")

                // Student management endpoints: accessible ONLY by ADMIN and STUDENT for full CRUD (Create, Read, Update, Delete)
                .requestMatchers("/api/students", "/api/students/**").hasAnyRole("ADMIN", "STUDENT")

                // Teacher management endpoints: accessible ONLY by ADMIN and TEACHER for full CRUD (Create, Read, Update, Delete)
                .requestMatchers("/api/teachers", "/api/teachers/**").hasAnyRole("ADMIN", "TEACHER")

                // All other endpoints require authentication
                .anyRequest().authenticated()
            )
            // Standard session logout configuration
            .logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID")
                .logoutSuccessHandler((request, response, authentication) -> {
                    response.setStatus(HttpServletResponse.SC_OK);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"message\":\"Logged out successfully\"}");
                })
            );

        return http.build();
    }
}
