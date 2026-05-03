package com.example.demo.batch;

import com.example.demo.entity.StudentMentorSync;
import org.springframework.batch.infrastructure.item.database.JdbcBatchItemWriter;
import org.springframework.batch.infrastructure.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class StudentMentorWriter {

    private final DataSource db2DataSource;

    public StudentMentorWriter(@Qualifier("db2DataSource") DataSource db2DataSource) {
        this.db2DataSource = db2DataSource;
    }

    @Bean
    public JdbcBatchItemWriter<StudentMentorSync> writer() {

        String sql = """
            INSERT INTO student_mentor_sync
              (student_id, student_name, student_email, mentor_id, mentor_name, expertise, synced_at)
            VALUES (:studentId, :studentName, :studentEmail, :mentorId, :mentorName, :expertise, :syncedAt)
            """;

        return new JdbcBatchItemWriterBuilder<StudentMentorSync>()
                .dataSource(db2DataSource)
                .sql(sql)
                .beanMapped()
                .build();
    }
}