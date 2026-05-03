package com.example.demo.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class StudentMentorSync {

    private Long id;

    private Long studentId;
    private String studentName;
    private String studentEmail;
    private Long mentorId;
    private String mentorName;
    private String expertise;

    private LocalDateTime syncedAt;
}
