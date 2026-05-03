package com.example.demo.controller;

import com.example.demo.service.SyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sync")
@RequiredArgsConstructor
public class SyncController {

    private final JobOperator jobOperator;  // ← replaced JobLauncher
    private final Job syncJob;
    private final SyncService syncService;

    @PostMapping("/run")
    public ResponseEntity<String> triggerSync() {
        try {
            JobParameters params = new JobParametersBuilder()
                    .addLong("startAt", System.currentTimeMillis())
                    .toJobParameters();

            jobOperator.start(syncJob, params);  // ← new API, no JobExecution returned
            return ResponseEntity.ok("Sync job triggered successfully!");

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Job failed: " + e.getMessage());
        }
    }

    @GetMapping("/status")
    public ResponseEntity<String> syncStatus() {
        int count = syncService.getSyncCount();
        return ResponseEntity.ok("DB2 currently has " + count + " synced rows.");
    }
}