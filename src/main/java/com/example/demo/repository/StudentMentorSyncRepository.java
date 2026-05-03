package com.example.demo.repository;

/**
 * `student_mentor_sync` is managed through JDBC against DB2, not JPA.
 * Keeping this as a plain interface prevents Spring Data JPA from trying
 * to create or manage the table in DB1.
 */
public interface StudentMentorSyncRepository {
}