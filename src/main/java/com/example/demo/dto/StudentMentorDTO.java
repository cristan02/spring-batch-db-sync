package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StudentMentorDTO {
    private Long studentId;
    private String studentName;
    private String studentEmail;
    private Long mentorId;
    private String mentorName;
    private String expertise;
}