package com.example.university_management.service;

import com.example.university_management.dto.AdminRequestDTO;
import com.example.university_management.entity.Admin;
import com.example.university_management.repository.AdminRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class AdminService {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminService(AdminRepository adminRepository, PasswordEncoder passwordEncoder) {
        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Admin createAdmin(AdminRequestDTO request) {
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new IllegalArgumentException("Password is required.");
        }

        if (adminRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("An admin with email '" + request.getEmail() + "' already exists.");
        }

        Admin admin = Admin.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .build();

        return adminRepository.save(admin);
    }

    @Transactional(readOnly = true)
    public List<Admin> getAllAdmins() {
        return adminRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Admin getAdminById(Long id) {
        return adminRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Admin not found with id: " + id));
    }

    public Admin updateAdmin(Long id, AdminRequestDTO request) {
        Admin existingAdmin = getAdminById(id);

        if (!existingAdmin.getEmail().equals(request.getEmail())
                && adminRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email '" + request.getEmail() + "' is already in use by another admin.");
        }

        existingAdmin.setName(request.getName());
        existingAdmin.setEmail(request.getEmail());

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            existingAdmin.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        return adminRepository.save(existingAdmin);
    }

    public void deleteAdmin(Long id) {
        Admin admin = getAdminById(id);
        if (adminRepository.count() <= 1) {
            throw new IllegalStateException("Cannot delete the only remaining admin account.");
        }
        adminRepository.delete(admin);
    }
}

