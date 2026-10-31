package com.example.university_management;

import com.example.university_management.dto.AdminRequestDTO;
import com.example.university_management.dto.LoginRequestDTO;
import com.example.university_management.dto.LoginResponseDTO;
import com.example.university_management.entity.Admin;
import com.example.university_management.entity.Student;
import com.example.university_management.entity.Teacher;
import com.example.university_management.repository.AdminRepository;
import com.example.university_management.repository.StudentRepository;
import com.example.university_management.repository.TeacherRepository;
import com.example.university_management.service.AdminService;
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

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class UniversityManagementServiceTests {

    @Autowired
    private StudentService studentService;

    @Autowired
    private TeacherService teacherService;

    @Autowired
    private AdminService adminService;

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
    void cleanUp() {
        studentRepository.deleteAll();
        teacherRepository.deleteAll();
        adminRepository.findByEmail("secondary.admin@university.com").ifPresent(adminRepository::delete);
    }

    @Test
    @DisplayName("Verify password hashing and student registration")
    void testStudentRegistrationAndPasswordHashing() {
        Student student = Student.builder()
                .studentId("STU_AUDIT_1")
                .name("Alice Auditor")
                .email("alice.audit@university.com")
                .department("Computer Science")
                .password("PlainPass123")
                .build();

        Student saved = studentService.registerStudent(student);

        assertNotNull(saved.getId());
        assertEquals("STU_AUDIT_1", saved.getStudentId());
        assertNotEquals("PlainPass123", saved.getPassword());
        assertTrue(saved.getPassword().startsWith("$2a$"), "Password must be BCrypt hashed");
        assertTrue(passwordEncoder.matches("PlainPass123", saved.getPassword()));
    }

    @Test
    @DisplayName("Verify duplicate student rejection")
    void testDuplicateStudentRejection() {
        Student student1 = Student.builder()
                .studentId("STU_AUDIT_DUP")
                .name("Student One")
                .email("dup.student@university.com")
                .department("Engineering")
                .password("Pass123")
                .build();
        studentService.registerStudent(student1);

        Student student2 = Student.builder()
                .studentId("STU_AUDIT_DUP")
                .name("Student Two")
                .email("other.student@university.com")
                .department("Engineering")
                .password("Pass456")
                .build();

        assertThrows(IllegalArgumentException.class, () -> studentService.registerStudent(student2));
    }

    @Test
    @DisplayName("Verify teacher registration and profile update preserves password")
    void testTeacherRegistrationAndUpdate() {
        Teacher teacher = Teacher.builder()
                .teacherId("TCH_AUDIT_1")
                .name("Dr. Audit")
                .email("audit.prof@university.com")
                .department("Physics")
                .designation("Associate Professor")
                .password("TeacherSecret")
                .build();

        Teacher saved = teacherService.registerTeacher(teacher);
        String initialHashedPassword = saved.getPassword();

        Teacher updatePayload = Teacher.builder()
                .teacherId("TCH_AUDIT_1")
                .name("Dr. Audit Senior")
                .email("audit.prof@university.com")
                .department("Applied Physics")
                .designation("Professor")
                .build();

        Teacher updated = teacherService.updateTeacher(saved.getId(), updatePayload);

        assertEquals("Dr. Audit Senior", updated.getName());
        assertEquals("Applied Physics", updated.getDepartment());
        assertEquals("Professor", updated.getDesignation());
        assertEquals(initialHashedPassword, updated.getPassword(), "Password must not be modified during profile update");
    }

    @Test
    @DisplayName("Verify multi-role authentication through AuthService")
    void testMultiRoleAuthentication() {
        // Register a student
        studentService.registerStudent(Student.builder()
                .studentId("STU_AUTH_1")
                .name("Auth Student")
                .email("student.auth@university.com")
                .department("Math")
                .password("StudentPass123")
                .build());

        // Register a teacher
        teacherService.registerTeacher(Teacher.builder()
                .teacherId("TCH_AUTH_1")
                .name("Auth Teacher")
                .email("teacher.auth@university.com")
                .department("Math")
                .designation("Lecturer")
                .password("TeacherPass123")
                .build());

        // 1. Authenticate Student
        LoginResponseDTO studentLogin = authService.login(LoginRequestDTO.builder()
                .email("student.auth@university.com")
                .password("StudentPass123")
                .build());
        assertEquals("STUDENT", studentLogin.getRole());
        assertEquals("Auth Student", studentLogin.getName());

        // 2. Authenticate Teacher
        LoginResponseDTO teacherLogin = authService.login(LoginRequestDTO.builder()
                .email("teacher.auth@university.com")
                .password("TeacherPass123")
                .build());
        assertEquals("TEACHER", teacherLogin.getRole());
        assertEquals("Auth Teacher", teacherLogin.getName());

        // 3. Authenticate Admin
        LoginResponseDTO adminLogin = authService.login(LoginRequestDTO.builder()
                .email("admin@university.com")
                .password("AdminPassword123")
                .build());
        assertEquals("ADMIN", adminLogin.getRole());
        assertEquals("System Admin", adminLogin.getName());

        // 4. Bad credentials test
        assertThrows(BadCredentialsException.class, () -> authService.login(LoginRequestDTO.builder()
                .email("admin@university.com")
                .password("WrongPassword")
                .build()));
    }

    @Test
    @DisplayName("Verify only one Admin exists in the database")
    void testSingleAdminConstraint() {
        List<Admin> admins = adminRepository.findAll();
        assertEquals(1, admins.size(), "There must be exactly one initial Admin account");
        Admin admin = admins.get(0);
        assertEquals("admin@university.com", admin.getEmail());
        assertTrue(admin.getPassword().startsWith("$2a$"), "Admin password must be BCrypt hashed");
    }

    @Test
    @DisplayName("Verify Admin CRUD operations and safeguard against deleting the only admin")
    void testAdminCrudOperations() {
        // 1. Create a secondary admin
        AdminRequestDTO createRequest = AdminRequestDTO.builder()
                .name("Secondary Admin")
                .email("secondary.admin@university.com")
                .password("SecPass123")
                .build();
        Admin created = adminService.createAdmin(createRequest);

        assertNotNull(created.getId());
        assertEquals("Secondary Admin", created.getName());
        assertEquals("secondary.admin@university.com", created.getEmail());
        assertTrue(created.getPassword().startsWith("$2a$"), "Password must be BCrypt hashed");
        assertTrue(passwordEncoder.matches("SecPass123", created.getPassword()));

        // 2. Read all and read by ID
        List<Admin> allAdmins = adminService.getAllAdmins();
        assertTrue(allAdmins.size() >= 2);

        Admin fetched = adminService.getAdminById(created.getId());
        assertEquals("Secondary Admin", fetched.getName());

        // 3. Update admin
        AdminRequestDTO updateRequest = AdminRequestDTO.builder()
                .name("Secondary Admin Senior")
                .email("secondary.admin@university.com")
                .build();
        Admin updated = adminService.updateAdmin(created.getId(), updateRequest);
        assertEquals("Secondary Admin Senior", updated.getName());
        assertTrue(passwordEncoder.matches("SecPass123", updated.getPassword()), "Password preserved on update");

        // 4. Delete the secondary admin
        adminService.deleteAdmin(created.getId());
        assertFalse(adminRepository.existsByEmail("secondary.admin@university.com"));

        // 5. Deleting the last remaining admin must throw IllegalStateException
        Admin initialAdmin = adminRepository.findByEmail("admin@university.com").orElseThrow();
        assertThrows(IllegalStateException.class, () -> adminService.deleteAdmin(initialAdmin.getId()));
    }
}

