package com.example.demo.controller;

import com.example.demo.entity.Mentor;
import com.example.demo.repository.MentorRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/mentors")
public class MentorController {

    private final MentorRepository mentorRepository;

    public MentorController(MentorRepository mentorRepository) {
        this.mentorRepository = mentorRepository;
    }

    // Get all mentors
    @GetMapping
    public List<Mentor> getAllMentors() {
        return mentorRepository.findAll();
    }

    // Get mentor by ID
    @GetMapping("/{id}")
    public Mentor getMentorById(@PathVariable Long id) {
        return mentorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Mentor not found"));
    }
}