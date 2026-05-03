package com.example.demo.batch;

import com.example.demo.dto.StudentMentorDTO;
import com.example.demo.entity.StudentMentorSync;
import org.springframework.batch.infrastructure.item.ItemProcessor;  // ✅ Fixed import for Boot 4
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class StudentMentorProcessor implements ItemProcessor<StudentMentorDTO, StudentMentorSync> {

    @Override
    public StudentMentorSync process(StudentMentorDTO dto) {
        StudentMentorSync sync = new StudentMentorSync();
        sync.setStudentId(dto.getStudentId());
        sync.setStudentName(dto.getStudentName());
        sync.setStudentEmail(dto.getStudentEmail());
        sync.setMentorId(dto.getMentorId());
        sync.setMentorName(dto.getMentorName());
        sync.setExpertise(dto.getExpertise());
        sync.setSyncedAt(LocalDateTime.now());
        return sync;
    }
}