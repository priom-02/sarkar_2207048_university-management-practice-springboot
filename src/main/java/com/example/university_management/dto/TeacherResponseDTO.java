package com.example.university_management.dto;

import com.example.university_management.entity.Teacher;
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
public class TeacherResponseDTO {

    private Long id;
    private String teacherId;
    private String name;
    private String email;
    private String department;
    private String designation;

    public static TeacherResponseDTO fromEntity(Teacher teacher) {
        return TeacherResponseDTO.builder()
                .id(teacher.getId())
                .teacherId(teacher.getTeacherId())
                .name(teacher.getName())
                .email(teacher.getEmail())
                .department(teacher.getDepartment())
                .designation(teacher.getDesignation())
                .build();
    }
}

