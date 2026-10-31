package com.example.university_management.controller;

import com.example.university_management.dto.LoginRequestDTO;
import com.example.university_management.dto.LoginResponseDTO;
import com.example.university_management.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO loginRequest,
                                                  HttpServletRequest request,
                                                  HttpServletResponse response) {
        LoginResponseDTO loginResponse = authService.login(loginRequest);

        // Establish an authenticated Spring Security context
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                loginResponse.getEmail(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + loginResponse.getRole()))
        );

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        // Persist the security context into the HTTP session
        securityContextRepository.saveContext(context, request, response);

        return ResponseEntity.ok(loginResponse);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(HttpServletRequest request,
                                                      HttpServletResponse response) {
        // Clear security context
        SecurityContextHolder.clearContext();

        // Invalidate HTTP session if active
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        // Save empty context
        SecurityContext emptyContext = SecurityContextHolder.createEmptyContext();
        securityContextRepository.saveContext(emptyContext, request, response);

        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }
}
