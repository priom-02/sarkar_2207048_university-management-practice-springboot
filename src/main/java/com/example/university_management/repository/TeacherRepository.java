package com.example.university_management.repository;

import com.example.university_management.entity.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TeacherRepository extends JpaRepository<Teacher, Long> {

    Optional<Teacher> findByEmail(String email);

    Optional<Teacher> findByTeacherId(String teacherId);

    boolean existsByEmail(String email);

    boolean existsByTeacherId(String teacherId);
}

