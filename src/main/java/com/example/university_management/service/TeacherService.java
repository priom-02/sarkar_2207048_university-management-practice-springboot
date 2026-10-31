package com.example.university_management.service;

import com.example.university_management.entity.Teacher;
import com.example.university_management.repository.TeacherRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class TeacherService {

    private final TeacherRepository teacherRepository;
    private final PasswordEncoder passwordEncoder;

    public TeacherService(TeacherRepository teacherRepository, PasswordEncoder passwordEncoder) {
        this.teacherRepository = teacherRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Teacher registerTeacher(Teacher teacher) {
        if (teacher.getPassword() == null || teacher.getPassword().isBlank()) {
            throw new IllegalArgumentException("Password is required.");
        }

        if (teacherRepository.existsByEmail(teacher.getEmail())) {
            throw new IllegalArgumentException("A teacher with email '" + teacher.getEmail() + "' already exists.");
        }

        if (teacherRepository.existsByTeacherId(teacher.getTeacherId())) {
            throw new IllegalArgumentException("A teacher with ID '" + teacher.getTeacherId() + "' already exists.");
        }

        teacher.setPassword(passwordEncoder.encode(teacher.getPassword()));
        return teacherRepository.save(teacher);
    }

    @Transactional(readOnly = true)
    public List<Teacher> getAllTeachers() {
        return teacherRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Teacher getTeacherById(Long id) {
        return teacherRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Teacher not found with id: " + id));
    }

    public Teacher updateTeacher(Long id, Teacher updatedTeacher) {
        Teacher existingTeacher = getTeacherById(id);

        if (!existingTeacher.getEmail().equals(updatedTeacher.getEmail())
                && teacherRepository.existsByEmail(updatedTeacher.getEmail())) {
            throw new IllegalArgumentException("Email '" + updatedTeacher.getEmail() + "' is already in use by another teacher.");
        }

        if (!existingTeacher.getTeacherId().equals(updatedTeacher.getTeacherId())
                && teacherRepository.existsByTeacherId(updatedTeacher.getTeacherId())) {
            throw new IllegalArgumentException("Teacher ID '" + updatedTeacher.getTeacherId() + "' is already in use by another teacher.");
        }

        existingTeacher.setName(updatedTeacher.getName());
        existingTeacher.setEmail(updatedTeacher.getEmail());
        existingTeacher.setTeacherId(updatedTeacher.getTeacherId());
        existingTeacher.setDepartment(updatedTeacher.getDepartment());
        existingTeacher.setDesignation(updatedTeacher.getDesignation());

        // Password is deliberately not overwritten during profile updates
        return teacherRepository.save(existingTeacher);
    }

    public void deleteTeacher(Long id) {
        Teacher teacher = getTeacherById(id);
        teacherRepository.delete(teacher);
    }
}

