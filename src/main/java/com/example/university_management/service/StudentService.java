package com.example.university_management.service;

import com.example.university_management.entity.Student;
import com.example.university_management.repository.StudentRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class StudentService {

    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;

    public StudentService(StudentRepository studentRepository, PasswordEncoder passwordEncoder) {
        this.studentRepository = studentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Student registerStudent(Student student) {
        if (student.getPassword() == null || student.getPassword().isBlank()) {
            throw new IllegalArgumentException("Password is required.");
        }

        if (studentRepository.existsByEmail(student.getEmail())) {
            throw new IllegalArgumentException("A student with email '" + student.getEmail() + "' already exists.");
        }

        if (studentRepository.existsByStudentId(student.getStudentId())) {
            throw new IllegalArgumentException("A student with ID '" + student.getStudentId() + "' already exists.");
        }

        student.setPassword(passwordEncoder.encode(student.getPassword()));
        return studentRepository.save(student);
    }

    @Transactional(readOnly = true)
    public List<Student> getAllStudents() {
        return studentRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Student getStudentById(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Student not found with id: " + id));
    }

    public Student updateStudent(Long id, Student updatedStudent) {
        Student existingStudent = getStudentById(id);

        if (!existingStudent.getEmail().equals(updatedStudent.getEmail())
                && studentRepository.existsByEmail(updatedStudent.getEmail())) {
            throw new IllegalArgumentException("Email '" + updatedStudent.getEmail() + "' is already in use by another student.");
        }

        if (!existingStudent.getStudentId().equals(updatedStudent.getStudentId())
                && studentRepository.existsByStudentId(updatedStudent.getStudentId())) {
            throw new IllegalArgumentException("Student ID '" + updatedStudent.getStudentId() + "' is already in use by another student.");
        }

        existingStudent.setName(updatedStudent.getName());
        existingStudent.setEmail(updatedStudent.getEmail());
        existingStudent.setStudentId(updatedStudent.getStudentId());
        existingStudent.setDepartment(updatedStudent.getDepartment());

        // Password is deliberately not overwritten during profile updates
        return studentRepository.save(existingStudent);
    }

    public void deleteStudent(Long id) {
        Student student = getStudentById(id);
        studentRepository.delete(student);
    }
}

