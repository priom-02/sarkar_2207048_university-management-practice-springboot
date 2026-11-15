package com.example.university_management.unit;

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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 6 Isolated Unit Tests using Mockito (No Spring Context or Database required).
 */
@ExtendWith(MockitoExtension.class)
public class UniversityManagementUnitTests {

    // --- Mocks for StudentService ---
    @Mock
    private StudentRepository studentRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private StudentService studentService;

    // --- Mocks for TeacherService and AuthService ---
    @Mock
    private TeacherRepository teacherRepository;

    @Mock
    private AdminRepository adminRepository;

    // 1. Unit Test 1: Student registration with password hashing
    @Test
    @DisplayName("Unit Test 1: Student registration successfully encodes password and saves")
    void testStudentRegistration_Success() {
        Student input = Student.builder()
                .studentId("STU_U1")
                .name("Unit Student")
                .email("student.unit@test.com")
                .department("CSE")
                .password("PlainPass123")
                .build();

        when(studentRepository.existsByEmail("student.unit@test.com")).thenReturn(false);
        when(studentRepository.existsByStudentId("STU_U1")).thenReturn(false);
        when(passwordEncoder.encode("PlainPass123")).thenReturn("$2a$10$hashedPassword");
        when(studentRepository.save(any(Student.class))).thenAnswer(invocation -> {
            Student s = invocation.getArgument(0);
            s.setId(100L);
            return s;
        });

        Student saved = studentService.registerStudent(input);

        assertNotNull(saved.getId());
        assertEquals("$2a$10$hashedPassword", saved.getPassword());
        verify(passwordEncoder, times(1)).encode("PlainPass123");
        verify(studentRepository, times(1)).save(input);
    }

    // 2. Unit Test 2: Student registration duplicate email throws exception
    @Test
    @DisplayName("Unit Test 2: Student registration with duplicate email throws IllegalArgumentException")
    void testStudentRegistration_DuplicateEmail_ThrowsException() {
        Student input = Student.builder()
                .studentId("STU_U2")
                .name("Duplicate Student")
                .email("dup@test.com")
                .department("CSE")
                .password("Pass123")
                .build();

        when(studentRepository.existsByEmail("dup@test.com")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> studentService.registerStudent(input));
        assertTrue(ex.getMessage().contains("already exists"));
        verify(studentRepository, never()).save(any());
    }

    // 3. Unit Test 3: Student registration blank password throws exception
    @Test
    @DisplayName("Unit Test 3: Student registration with blank password throws IllegalArgumentException")
    void testStudentRegistration_BlankPassword_ThrowsException() {
        Student input = Student.builder()
                .studentId("STU_U3")
                .name("No Pass Student")
                .email("nopass@test.com")
                .department("CSE")
                .password("   ")
                .build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> studentService.registerStudent(input));
        assertEquals("Password is required.", ex.getMessage());
        verify(studentRepository, never()).save(any());
    }

    // 4. Unit Test 4: Teacher registration with password hashing
    @Test
    @DisplayName("Unit Test 4: Teacher registration successfully encodes password and saves")
    void testTeacherRegistration_Success() {
        TeacherService localTeacherService = new TeacherService(teacherRepository, passwordEncoder);

        Teacher input = Teacher.builder()
                .teacherId("TCH_U1")
                .name("Unit Teacher")
                .email("teacher.unit@test.com")
                .department("Physics")
                .designation("Lecturer")
                .password("TeacherSecret")
                .build();

        when(teacherRepository.existsByEmail("teacher.unit@test.com")).thenReturn(false);
        when(teacherRepository.existsByTeacherId("TCH_U1")).thenReturn(false);
        when(passwordEncoder.encode("TeacherSecret")).thenReturn("$2a$10$teacherHashed");
        when(teacherRepository.save(any(Teacher.class))).thenAnswer(invocation -> {
            Teacher t = invocation.getArgument(0);
            t.setId(200L);
            return t;
        });

        Teacher saved = localTeacherService.registerTeacher(input);

        assertNotNull(saved.getId());
        assertEquals("$2a$10$teacherHashed", saved.getPassword());
        verify(passwordEncoder, times(1)).encode("TeacherSecret");
        verify(teacherRepository, times(1)).save(input);
    }

    // 5. Unit Test 5: Teacher profile update preserves original password
    @Test
    @DisplayName("Unit Test 5: Teacher profile update updates fields and preserves existing password")
    void testTeacherUpdate_PreservesPassword() {
        TeacherService localTeacherService = new TeacherService(teacherRepository, passwordEncoder);

        Teacher existing = Teacher.builder()
                .id(1L)
                .teacherId("TCH_U2")
                .name("Dr. Old")
                .email("teacher.old@test.com")
                .department("Math")
                .designation("Assistant Professor")
                .password("$2a$10$originalHashedPassword")
                .build();

        Teacher updatePayload = Teacher.builder()
                .teacherId("TCH_U2")
                .name("Dr. New")
                .email("teacher.old@test.com")
                .department("Applied Math")
                .designation("Professor")
                .build();

        when(teacherRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(teacherRepository.save(any(Teacher.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Teacher updated = localTeacherService.updateTeacher(1L, updatePayload);

        assertEquals("Dr. New", updated.getName());
        assertEquals("Applied Math", updated.getDepartment());
        assertEquals("Professor", updated.getDesignation());
        assertEquals("$2a$10$originalHashedPassword", updated.getPassword(), "Password must not be changed during profile update");
    }

    // 6. Unit Test 6: AuthService login authentication success for Student
    @Test
    @DisplayName("Unit Test 6: AuthService validates password and returns role upon login")
    void testAuthService_Login_Success() {
        AuthService localAuthService = new AuthService(adminRepository, teacherRepository, studentRepository, passwordEncoder);

        Student student = Student.builder()
                .id(50L)
                .name("Alice Auth")
                .email("alice.auth@test.com")
                .password("$2a$10$aliceHashed")
                .build();

        when(adminRepository.findByEmail("alice.auth@test.com")).thenReturn(Optional.empty());
        when(teacherRepository.findByEmail("alice.auth@test.com")).thenReturn(Optional.empty());
        when(studentRepository.findByEmail("alice.auth@test.com")).thenReturn(Optional.of(student));
        when(passwordEncoder.matches("AliceRawPass", "$2a$10$aliceHashed")).thenReturn(true);

        LoginRequestDTO req = LoginRequestDTO.builder()
                .email("alice.auth@test.com")
                .password("AliceRawPass")
                .build();

        LoginResponseDTO res = localAuthService.login(req);

        assertNotNull(res);
        assertEquals("STUDENT", res.getRole());
        assertEquals("Alice Auth", res.getName());
        assertEquals("alice.auth@test.com", res.getEmail());
    }
}

