package com.example.demo.batch;

import com.example.demo.dto.StudentMentorDTO;
import org.springframework.batch.infrastructure.item.database.JdbcCursorItemReader;
import org.springframework.batch.infrastructure.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class StudentMentorReader {

    private final JdbcTemplate db1JdbcTemplate;

    public StudentMentorReader(@Qualifier("db1JdbcTemplate") JdbcTemplate db1JdbcTemplate) {
        this.db1JdbcTemplate = db1JdbcTemplate;
    }

    public JdbcCursorItemReader<StudentMentorDTO> reader() {

        String sql = """
            SELECT s.id, s.name, s.email,
                   m.id AS mentor_id, m.name AS mentor_name, m.expertise
            FROM student s
            JOIN mentor m ON s.mentor_id = m.id
            """;

        return new JdbcCursorItemReaderBuilder<StudentMentorDTO>()
                .name("studentMentorReader")
                .dataSource(db1JdbcTemplate.getDataSource())
                .sql(sql)
                .rowMapper((rs, rowNum) -> new StudentMentorDTO(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getLong("mentor_id"),
                        rs.getString("mentor_name"),
                        rs.getString("expertise")
                ))
                .build();
    }
}
