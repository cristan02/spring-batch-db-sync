# Demo - Student/Mentor Sync Service

A Spring Boot application that exposes simple REST endpoints for viewing `Student` and `Mentor` data from one database (`db1`) and runs a batch synchronization job that copies a joined projection of student/mentor data into a reporting table in a second database (`db2`).

The project uses:

- **Spring Boot 4.x**
- **Java 21**
- **Spring Web** for REST endpoints
- **Spring Data JPA** for reading the main domain data
- **Spring Batch** for scheduled/manual synchronization
- **JdbcTemplate** for direct cross-database sync operations
- **MySQL** for both databases

---

## Table of Contents

1. [What the project does](#what-the-project-does)
2. [Architecture overview](#architecture-overview)
3. [Project structure](#project-structure)
4. [Data flow](#data-flow)
5. [REST API endpoints](#rest-api-endpoints)
6. [Batch sync job](#batch-sync-job)
7. [Database configuration](#database-configuration)
8. [Local setup](#local-setup)
9. [How to run](#how-to-run)
10. [Useful commands](#useful-commands)
11. [Notes and implementation details](#notes-and-implementation-details)

---

## What the project does

This application demonstrates a small but complete data-sync workflow:

- `db1` stores the main application data:
  - `student`
  - `mentor`
- `db2` stores a flattened synchronization table:
  - `student_mentor_sync`
- The app exposes read-only REST endpoints to inspect students and mentors.
- A batch job periodically reads joined student/mentor data from `db1` and writes it into `db2`.
- The sync table in `db2` is cleared before each run so the output always reflects the latest snapshot.

In short: **`db1` is the source of truth, and `db2` is the sync/reporting target.**

---

## Architecture overview

### High-level flow

1. Spring Boot starts and loads two data sources.
2. JPA uses the primary data source (`db1`) for `Student` and `Mentor` entities.
3. Spring Batch is configured to use the secondary data source (`db2`) for the synchronization job.
4. A scheduler triggers the batch job every 2 minutes.
5. A REST endpoint can also trigger the same job manually.
6. The job reads from `db1`, transforms the rows, truncates `student_mentor_sync`, and writes fresh records to `db2`.

### Design intent

This project is useful for demonstrating:

- multiple data sources in one Spring Boot app
- JPA vs JDBC usage side by side
- scheduled jobs
- manual job triggering through an API
- batch chunk processing
- data projection into a reporting table

---

## Project structure

```text
src/main/java/com/example/demo
├── DemoApplication.java
├── batch
│   ├── StudentMentorProcessor.java
│   ├── StudentMentorReader.java
│   └── StudentMentorWriter.java
├── config
│   ├── BatchConfig.java
│   └── DataSourceConfig.java
├── controller
│   ├── MentorController.java
│   ├── StudentController.java
│   └── SyncController.java
├── dto
│   └── StudentMentorDTO.java
├── entity
│   ├── Mentor.java
│   ├── Student.java
│   └── StudentMentorSync.java
├── repository
│   ├── MentorRepository.java
│   ├── StudentMentorSyncRepository.java
│   └── StudentRepository.java
├── scheduler
│   └── SyncScheduler.java
└── service
    └── SyncService.java
```

### Package-by-package explanation

#### `DemoApplication`
Application entry point. It enables Spring Boot auto-configuration and scheduling.

#### `config`
Contains infrastructure wiring:

- **`DataSourceConfig`**
  - creates two MySQL data sources
  - creates two `JdbcTemplate` beans
  - marks `db1` as the primary/default data source for JPA
- **`BatchConfig`**
  - configures the batch job, step, transaction manager, and table-clearing listener

#### `controller`
REST endpoints for accessing domain data and controlling the sync job.

#### `dto`
Contains `StudentMentorDTO`, a flattened row used during batch reading and processing.

#### `entity`
JPA entities for the main domain tables (`Student`, `Mentor`) plus the sync model (`StudentMentorSync`) written to `db2`.

#### `repository`
Spring Data JPA repositories for `Student` and `Mentor`. The sync table is intentionally **not** managed by JPA.

#### `scheduler`
Contains the scheduled trigger that runs the sync job every 2 minutes.

#### `service`
Contains `SyncService`, a direct JDBC helper for reading from `db1`, writing to `db2`, and reporting row counts.

#### `batch`
Spring Batch components:

- **Reader**: reads joined student/mentor rows from `db1`
- **Processor**: maps DTOs to `StudentMentorSync`
- **Writer**: writes processed rows to `db2`

---

## Data flow

### 1) Main application data in `db1`
The application stores students and mentors in normal relational form:

- `mentor` has many `student` records
- each `student` belongs to one `mentor`

This relationship is represented by JPA:

- `Student`
  - `id`
  - `name`
  - `email`
  - `mentor_id`
- `Mentor`
  - `id`
  - `name`
  - `expertise`

### 2) Flattened sync data in `db2`
The batch job creates a denormalized row structure in `student_mentor_sync`:

- `student_id`
- `student_name`
- `student_email`
- `mentor_id`
- `mentor_name`
- `expertise`
- `synced_at`

This table is intended for synchronization/reporting use cases.

### 3) Sync job pipeline
The job performs the following steps:

1. Read from `db1` using a SQL join across `student` and `mentor`
2. Convert each row into a DTO
3. Process the DTO into a `StudentMentorSync` object
4. Truncate `student_mentor_sync` in `db2`
5. Insert the fresh rows into `db2`

---

## REST API endpoints

### Students

#### `GET /students`
Returns all students from `db1`.

- Backed by: `StudentRepository`
- Returns: `List<Student>`

Example:

```bash
curl http://localhost:8080/students
```

#### `GET /students/{id}`
Returns one student by ID.

- Backed by: `StudentRepository`
- Returns: one `Student`
- If the student is not found, the controller throws a runtime exception

Example:

```bash
curl http://localhost:8080/students/1
```

### Mentors

#### `GET /mentors`
Returns all mentors from `db1`.

- Backed by: `MentorRepository`
- Returns: `List<Mentor>`

Example:

```bash
curl http://localhost:8080/mentors
```

#### `GET /mentors/{id}`
Returns one mentor by ID.

- Backed by: `MentorRepository`
- Returns: one `Mentor`
- If the mentor is not found, the controller throws a runtime exception

Example:

```bash
curl http://localhost:8080/mentors/1
```

### Sync

#### `POST /sync/run`
Manually triggers the batch sync job.

- Uses `JobOperator` to start the configured Spring Batch job
- Adds a `startAt` parameter with the current timestamp so each launch is unique
- Returns a success/failure text response

Example:

```bash
curl -X POST http://localhost:8080/sync/run
```

#### `GET /sync/status`
Returns how many rows currently exist in the sync table in `db2`.

- Backed by: `SyncService.getSyncCount()`
- Returns a message like:

```text
DB2 currently has 12 synced rows.
```

Example:

```bash
curl http://localhost:8080/sync/status
```

---

## Batch sync job

The batch job is defined in `BatchConfig` and runs a single step named `syncStep`.

### Job characteristics

- **Job name**: configured through Spring Batch bean wiring
- **Step name**: `syncStep`
- **Chunk size**: `10`
- **Triggering**:
  - scheduled automatically every 2 minutes
  - manually through `POST /sync/run`
- **Auto-run at startup**: disabled via `spring.batch.job.enabled=false`

### Reader

`StudentMentorReader` reads a joined dataset from `db1`:

Reader SQL example:

- `SELECT s.id, s.name, s.email, m.id AS mentor_id, m.name AS mentor_name, m.expertise`
- `FROM student s`
- `JOIN mentor m ON s.mentor_id = m.id`

It uses `JdbcCursorItemReader` so the data is streamed row by row from the source database.

### Processor

`StudentMentorProcessor` maps each `StudentMentorDTO` into a `StudentMentorSync` object.

This is where the batch job:

- copies the student fields
- copies the mentor fields
- stamps the row with `LocalDateTime.now()` in `syncedAt`

### Writer

`StudentMentorWriter` writes processed rows into `db2` using `JdbcBatchItemWriter`.

The insert target is:

Writer SQL example:

- `INSERT INTO student_mentor_sync (student_id, student_name, student_email, mentor_id, mentor_name, expertise, synced_at)`
- `VALUES (:studentId, :studentName, :studentEmail, :mentorId, :mentorName, :expertise, :syncedAt)`

### Table reset behavior

Before each step starts, `BatchConfig` registers a listener that truncates `student_mentor_sync`:

Table reset example: `TRUNCATE TABLE student_mentor_sync`

This means every run produces a fresh snapshot instead of appending duplicates.

### Transaction manager

The batch step uses a `JdbcTransactionManager` tied to `db2`, which keeps the sync writes consistent for the target database.

### Scheduler

`SyncScheduler` is annotated with `@Scheduled(cron = "0 */2 * * * *")`, which means the sync job runs every 2 minutes.

It starts the same batch job programmatically with a timestamp parameter.

---

## Database configuration

The app uses **two separate MySQL databases**.

### `db1` - primary application database

Configured in `application.properties`:

```properties
spring.datasource.db1.url=jdbc:mysql://localhost:3306/db1
spring.datasource.db1.username=root
spring.datasource.db1.password=qwerty1234
spring.datasource.db1.driver-class-name=com.mysql.cj.jdbc.Driver
```

Used for:

- JPA entities: `Student`, `Mentor`
- repositories: `StudentRepository`, `MentorRepository`
- source-side batch reads
- `db1JdbcTemplate`

### `db2` - sync/reporting database

Configured in `application.properties`:

```properties
spring.datasource.db2.url=jdbc:mysql://localhost:3306/db2
spring.datasource.db2.username=root
spring.datasource.db2.password=qwerty1234
spring.datasource.db2.driver-class-name=com.mysql.cj.jdbc.Driver
```

Used for:

- `student_mentor_sync`
- batch writes
- sync count queries
- batch metadata tables, because `spring.batch.jdbc.initialize-schema=always`

### Important configuration flags

```properties
spring.batch.jdbc.initialize-schema=always
spring.batch.job.enabled=false
spring.task.scheduling.enabled=true
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
```

#### What these mean

- **`spring.batch.jdbc.initialize-schema=always`**
  - Spring Batch creates its metadata tables automatically.
- **`spring.batch.job.enabled=false`**
  - jobs do not run automatically when the app starts.
- **`spring.task.scheduling.enabled=true`**
  - the scheduler is active.
- **`spring.jpa.hibernate.ddl-auto=update`**
  - JPA will update the schema for `Student` and `Mentor` if needed.
- **`spring.jpa.show-sql=true`**
  - SQL statements executed by JPA are printed to the console.

---

## Local setup

### Prerequisites

- Java **21**
- Maven or the Maven wrapper (`./mvnw`)
- MySQL server running locally
- Two databases created:
  - `db1`
  - `db2`

### 1) Create the databases

Create the databases:

- `CREATE DATABASE db1;`
- `CREATE DATABASE db2;`

### 2) Ensure the required tables exist

The code expects at least these tables:

#### In `db1`

`db1` tables:

- `mentor(id BIGINT PRIMARY KEY AUTO_INCREMENT, name VARCHAR(255), expertise VARCHAR(255))`
- `student(id BIGINT PRIMARY KEY AUTO_INCREMENT, name VARCHAR(255), email VARCHAR(255), mentor_id BIGINT, FOREIGN KEY (mentor_id) REFERENCES mentor(id))`

#### In `db2`

`db2` table:

- `student_mentor_sync(id BIGINT PRIMARY KEY AUTO_INCREMENT, student_id BIGINT, student_name VARCHAR(255), student_email VARCHAR(255), mentor_id BIGINT, mentor_name VARCHAR(255), expertise VARCHAR(255), synced_at DATETIME)`

### 3) Add seed data

A simple example:

Example seed data for `db1`:

- `USE db1;`
- `INSERT INTO mentor (name, expertise) VALUES ('Alice Mentor', 'Java'), ('Bob Mentor', 'Databases');`
- `INSERT INTO student (name, email, mentor_id) VALUES ('John Student', 'john@example.com', 1), ('Jane Student', 'jane@example.com', 2);`

### 4) Update credentials if needed

The default username/password in this repo are:

- username: `root`
- password: `qwerty1234`

If your local MySQL setup is different, update `src/main/resources/application.properties`.

---

## How to run

From the project root:

```bash
./mvnw spring-boot:run
```

Or build first and run the jar:

```bash
./mvnw clean package
java -jar target/demo-0.0.1-SNAPSHOT.jar
```

The app should start on port `8080` by default.

---

## Useful commands

### Check the current sync row count

```bash
curl http://localhost:8080/sync/status
```

### Trigger a sync manually

```bash
curl -X POST http://localhost:8080/sync/run
```

### View all students

```bash
curl http://localhost:8080/students
```

### View all mentors

```bash
curl http://localhost:8080/mentors
```

---

## Notes and implementation details

### 1) `student_mentor_sync` is not a JPA entity

The sync table is handled with JDBC instead of JPA. The repository file `StudentMentorSyncRepository` is intentionally just a placeholder interface so Spring Data JPA does not try to manage that table in the primary database.

### 2) `SyncService` is a direct JDBC helper

`SyncService` provides three useful methods:

- `readFromDB1()`
- `writeToDB2(...)`
- `getSyncCount()`

It duplicates some of the batch logic but is still useful for direct DB operations and for the status endpoint.

### 3) The sync job is snapshot-based

Because the sync table is truncated before each run, the output represents the current state of `db1` at the time the job runs.

### 4) REST endpoints are read-only except for sync trigger

The current controllers only expose GET endpoints for viewing data and a POST endpoint for running the sync.

### 5) JSON handling on entity relationships

`Student` and `Mentor` use `@JsonBackReference` and `@JsonManagedReference` to avoid infinite recursion when serializing the bidirectional relationship.

---

## Troubleshooting

### The app fails to connect to MySQL

Check that:

- MySQL is running
- the credentials in `application.properties` are correct
- both databases (`db1`, `db2`) exist

### The sync job reports zero rows

Check that:

- `student` and `mentor` tables contain data
- each student has a valid `mentor_id`
- the join query returns rows

### Batch tables are missing

Because Spring Batch metadata is initialized automatically, tables should be created on startup. If not, check the console logs for schema creation errors.

### Manual sync fails with job parameter errors

The job uses a unique `startAt` parameter. If you change the code, make sure each run still gets a unique set of parameters.

---

## Summary

This project is a small demo of a two-database Spring Boot system:

- `db1` stores the source application data
- `db2` stores synchronized flattened data
- JPA serves the main entities
- Spring Batch performs the periodic refresh
- REST endpoints let you inspect data and manually run the job

If you want, I can also add:

- a **sample SQL seed file**
- a **diagram** of the data flow
- an **API examples section with JSON responses**
- a **shorter polished README version** for GitHub



# spring-batch-db-sync
