package com.example.university_management.service;

import com.example.university_management.dto.LoginRequestDTO;
import com.example.university_management.dto.LoginResponseDTO;
import com.example.university_management.entity.Admin;
import com.example.university_management.entity.Student;
import com.example.university_management.entity.Teacher;
import com.example.university_management.repository.AdminRepository;
import com.example.university_management.repository.StudentRepository;
import com.example.university_management.repository.TeacherRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class AuthService {

    private final AdminRepository adminRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(AdminRepository adminRepository,
                       TeacherRepository teacherRepository,
                       StudentRepository studentRepository,
                       PasswordEncoder passwordEncoder) {
        this.adminRepository = adminRepository;
        this.teacherRepository = teacherRepository;
        this.studentRepository = studentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public LoginResponseDTO login(LoginRequestDTO request) {
        if (request == null || request.getEmail() == null || request.getPassword() == null) {
            throw new BadCredentialsException("Invalid email or password.");
        }

        String email = request.getEmail().trim();
        String rawPassword = request.getPassword();

        Optional<Admin> adminOpt = adminRepository.findByEmail(email);
        Optional<Teacher> teacherOpt = teacherRepository.findByEmail(email);
        Optional<Student> studentOpt = studentRepository.findByEmail(email);

        int matchCount = (adminOpt.isPresent() ? 1 : 0)
                       + (teacherOpt.isPresent() ? 1 : 0)
                       + (studentOpt.isPresent() ? 1 : 0);

        if (matchCount > 1) {
            throw new IllegalStateException("Ambiguous account state: Email '" + email + "' belongs to multiple account roles.");
        }

        if (adminOpt.isPresent()) {
            Admin admin = adminOpt.get();
            verifyPassword(rawPassword, admin.getPassword());
            return LoginResponseDTO.builder()
                    .message("Login successful")
                    .name(admin.getName())
                    .email(admin.getEmail())
                    .role("ADMIN")
                    .build();
        }

        if (teacherOpt.isPresent()) {
            Teacher teacher = teacherOpt.get();
            verifyPassword(rawPassword, teacher.getPassword());
            return LoginResponseDTO.builder()
                    .message("Login successful")
                    .name(teacher.getName())
                    .email(teacher.getEmail())
                    .role("TEACHER")
                    .build();
        }

        if (studentOpt.isPresent()) {
            Student student = studentOpt.get();
            verifyPassword(rawPassword, student.getPassword());
            return LoginResponseDTO.builder()
                    .message("Login successful")
                    .name(student.getName())
                    .email(student.getEmail())
                    .role("STUDENT")
                    .build();
        }

        throw new BadCredentialsException("Invalid email or password.");
    }

    private void verifyPassword(String rawPassword, String encodedPassword) {
        if (!passwordEncoder.matches(rawPassword, encodedPassword)) {
            throw new BadCredentialsException("Invalid email or password.");
        }
    }
}

