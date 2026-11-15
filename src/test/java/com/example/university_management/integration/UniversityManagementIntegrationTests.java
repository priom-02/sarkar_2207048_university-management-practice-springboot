package com.example.university_management.integration;

import com.example.university_management.dto.LoginRequestDTO;
import com.example.university_management.dto.LoginResponseDTO;
import com.example.university_management.entity.Student;
import com.example.university_management.entity.Teacher;
import com.example.university_management.repository.AdminRepository;
import com.example.university_management.repository.StudentRepository;
import com.example.university_management.repository.TeacherRepository;
import com.example.university_management.service.AuthService;
import com.example.university_management.service.StudentService;
import com.example.university_management.service.TeacherService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 3 Comprehensive Integration Tests testing Service, Repository, Database, and PasswordEncoder interactions.
 */
@SpringBootTest
public class UniversityManagementIntegrationTests {

    @Autowired
    private StudentService studentService;

    @Autowired
    private TeacherService teacherService;

    @Autowired
    private AuthService authService;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    @AfterEach
    void cleanTestData() {
        studentRepository.findByStudentId("IT_STU_1").ifPresent(studentRepository::delete);
        teacherRepository.findByTeacherId("IT_TCH_1").ifPresent(teacherRepository::delete);
    }

    // 1. Integration Test 1: Student registration, database persistence & password encryption
    @Test
    @DisplayName("Integration Test 1: Student registration persists with BCrypt hash and enforces uniqueness")
    void testStudentRegistrationAndDatabasePersistenceIntegration() {
        Student student = Student.builder()
                .studentId("IT_STU_1")
                .name("Integration Student")
                .email("it.student@test.com")
                .department("Computer Science")
                .password("IntegPass123")
                .build();

        Student saved = studentService.registerStudent(student);

        assertNotNull(saved.getId());
        assertTrue(saved.getPassword().startsWith("$2a$"), "Password must be BCrypt hashed in DB");
        assertTrue(passwordEncoder.matches("IntegPass123", saved.getPassword()));

        // Verify retrieval from repository
        Student found = studentRepository.findByStudentId("IT_STU_1").orElse(null);
        assertNotNull(found);
        assertEquals("Integration Student", found.getName());

        // Verify uniqueness constraint enforcement
        Student duplicate = Student.builder()
                .studentId("IT_STU_DUP")
                .name("Duplicate Email")
                .email("it.student@test.com")
                .department("CSE")
                .password("Pass456")
                .build();
        assertThrows(IllegalArgumentException.class, () -> studentService.registerStudent(duplicate));
    }

    // 2. Integration Test 2: Teacher CRUD lifecycle with profile update
    @Test
    @DisplayName("Integration Test 2: Teacher registration, update, and deletion in database")
    void testTeacherCrudLifecycleIntegration() {
        Teacher teacher = Teacher.builder()
                .teacherId("IT_TCH_1")
                .name("Integration Teacher")
                .email("it.teacher@test.com")
                .department("Electrical Engineering")
                .designation("Assistant Professor")
                .password("TeacherSecret123")
                .build();

        Teacher saved = teacherService.registerTeacher(teacher);
        String initialHash = saved.getPassword();

        // Update profile
        Teacher updateData = Teacher.builder()
                .teacherId("IT_TCH_1")
                .name("Integration Teacher Senior")
                .email("it.teacher@test.com")
                .department("EEE")
                .designation("Professor")
                .build();

        Teacher updated = teacherService.updateTeacher(saved.getId(), updateData);
        assertEquals("Integration Teacher Senior", updated.getName());
        assertEquals("Professor", updated.getDesignation());
        assertEquals(initialHash, updated.getPassword(), "Password hash must remain intact");

        // Delete
        teacherService.deleteTeacher(saved.getId());
        assertFalse(teacherRepository.findByTeacherId("IT_TCH_1").isPresent());
    }

    // 3. Integration Test 3: Multi-role authentication & session credentials verification
    @Test
    @DisplayName("Integration Test 3: Multi-role authentication across Admin, Teacher, and Student")
    void testMultiRoleAuthenticationIntegration() {
        // 1. Authenticate Admin
        LoginResponseDTO adminLogin = authService.login(LoginRequestDTO.builder()
                .email("admin@university.com")
                .password("AdminPassword123")
                .build());
        assertEquals("ADMIN", adminLogin.getRole());

        // 2. Register temporary student and test authentication
        Student student = studentService.registerStudent(Student.builder()
                .studentId("IT_STU_1")
                .name("Auth Test Student")
                .email("it.student@test.com")
                .department("Physics")
                .password("StuPass123")
                .build());

        LoginResponseDTO studentLogin = authService.login(LoginRequestDTO.builder()
                .email("it.student@test.com")
                .password("StuPass123")
                .build());
        assertEquals("STUDENT", studentLogin.getRole());
        assertEquals("Auth Test Student", studentLogin.getName());

        // 3. Verify bad credentials rejection
        assertThrows(BadCredentialsException.class, () -> authService.login(LoginRequestDTO.builder()
                .email("it.student@test.com")
                .password("WrongPassword")
                .build()));
    }
}

