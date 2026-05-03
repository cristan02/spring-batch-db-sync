package com.example.demo.service;

import com.example.demo.dto.StudentMentorDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class SyncService {

    private final JdbcTemplate db1JdbcTemplate;
    private final JdbcTemplate db2JdbcTemplate;

    public SyncService(
            @Qualifier("db1JdbcTemplate") JdbcTemplate db1JdbcTemplate,
            @Qualifier("db2JdbcTemplate") JdbcTemplate db2JdbcTemplate
    ) {
        this.db1JdbcTemplate = db1JdbcTemplate;
        this.db2JdbcTemplate = db2JdbcTemplate;
    }

    public List<StudentMentorDTO> readFromDB1() {
        String sql = """
            SELECT s.id, s.name, s.email,
                   m.id AS mentor_id, m.name AS mentor_name, m.expertise
            FROM student s
            JOIN mentor m ON s.mentor_id = m.id
            """;

        return db1JdbcTemplate.query(sql, (rs, rowNum) -> new StudentMentorDTO(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("email"),
                rs.getLong("mentor_id"),
                rs.getString("mentor_name"),
                rs.getString("expertise")
        ));
    }

    public void writeToDB2(List<StudentMentorDTO> rows) {
        db2JdbcTemplate.execute("TRUNCATE TABLE student_mentor_sync");

        String sql = """
            INSERT INTO student_mentor_sync
              (student_id, student_name, student_email, mentor_id, mentor_name, expertise, synced_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

        db2JdbcTemplate.batchUpdate(sql, rows, rows.size(), (ps, dto) -> {
            ps.setLong(1,   dto.getStudentId());
            ps.setString(2, dto.getStudentName());
            ps.setString(3, dto.getStudentEmail());
            ps.setLong(4,   dto.getMentorId());
            ps.setString(5, dto.getMentorName());
            ps.setString(6, dto.getExpertise());
            ps.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now()));
        });

        log.info("Synced {} rows to DB2 at {}", rows.size(), LocalDateTime.now());
    }

    public int getSyncCount() {
        Integer count = db2JdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM student_mentor_sync", Integer.class
        );
        return count != null ? count : 0;
    }
}
