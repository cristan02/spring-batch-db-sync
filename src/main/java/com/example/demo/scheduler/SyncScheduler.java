package com.example.demo.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SyncScheduler {

    private final JobOperator jobOperator;
    private final Job syncJob;

    @Scheduled(cron = "0 */2 * * * *")
    public void runSync() {
        try {
            JobParameters params = new JobParametersBuilder()
                    .addLong("startAt", System.currentTimeMillis())
                    .toJobParameters();

            jobOperator.start(syncJob, params);  // ← new API: takes Job object + JobParameters
            log.info("Sync job triggered at {}", System.currentTimeMillis());

        } catch (Exception e) {
            log.error("Sync job failed: {}", e.getMessage());
        }
    }
}