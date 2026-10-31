package com.example.university_management.dto;

import com.example.university_management.entity.Student;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentResponseDTO {

    private Long id;
    private String studentId;
    private String name;
    private String email;
    private String department;

    public static StudentResponseDTO fromEntity(Student student) {
        return StudentResponseDTO.builder()
                .id(student.getId())
                .studentId(student.getStudentId())
                .name(student.getName())
                .email(student.getEmail())
                .department(student.getDepartment())
                .build();
    }
}

