package com.example.demo.config;

import com.example.demo.batch.StudentMentorProcessor;
import com.example.demo.batch.StudentMentorReader;
import com.example.demo.batch.StudentMentorWriter;
import com.example.demo.dto.StudentMentorDTO;
import com.example.demo.entity.StudentMentorSync;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.Job;                    // ✅ New package in Batch 6
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;                  // ✅ New package in Batch 6
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.listener.StepExecutionListener;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ConnectionCallback;
import org.jspecify.annotations.NonNull;

import java.sql.Statement;

import javax.sql.DataSource;

@Configuration
// ✅ No @EnableBatchProcessing — auto-configured in Spring Boot 4
public class BatchConfig {

    private final StudentMentorReader studentMentorReader;
    private final StudentMentorProcessor studentMentorProcessor;
    private final StudentMentorWriter studentMentorWriter;
    private final DataSource db2DataSource;
    private final JdbcTemplate db2JdbcTemplate;

    public BatchConfig(
            StudentMentorReader studentMentorReader,
            StudentMentorProcessor studentMentorProcessor,
            StudentMentorWriter studentMentorWriter,
            @Qualifier("db2DataSource") DataSource db2DataSource,
            @Qualifier("db2JdbcTemplate") JdbcTemplate db2JdbcTemplate
    ) {
        this.studentMentorReader = studentMentorReader;
        this.studentMentorProcessor = studentMentorProcessor;
        this.studentMentorWriter = studentMentorWriter;
        this.db2DataSource = db2DataSource;
        this.db2JdbcTemplate = db2JdbcTemplate;
    }

    @Bean
    public JdbcTransactionManager batchTransactionManager() {
        return new JdbcTransactionManager(db2DataSource);
    }

    @Bean
    public Job syncJob(JobRepository jobRepository, Step syncStep) {
        return new JobBuilder(jobRepository)   // ✅ No name param in Batch 6
                .start(syncStep)
                .build();
    }

    @Bean
    public StepExecutionListener clearStudentMentorSyncTableListener() {
        return new StepExecutionListener() {
            @Override
            public void beforeStep(@NonNull StepExecution stepExecution) {
                db2JdbcTemplate.execute((ConnectionCallback<Void>) connection -> {
                    try (Statement statement = connection.createStatement()) {
                        String sql = "TRUNCATE TABLE " + "student_mentor_sync";
                        statement.executeUpdate(sql);
                    }
                    return null;
                });
            }

            @Override
            public ExitStatus afterStep(@NonNull StepExecution stepExecution) {
                return ExitStatus.COMPLETED;
            }
        };
    }

    @Bean
    public Step syncStep(JobRepository jobRepository) {
        return new StepBuilder("syncStep", jobRepository)
                .<StudentMentorDTO, StudentMentorSync>chunk(10)
                .transactionManager(batchTransactionManager())
                .listener(clearStudentMentorSyncTableListener())
                .reader(studentMentorReader.reader())
                .processor(studentMentorProcessor)
                .writer(studentMentorWriter.writer())
                .build();
    }
}