# TicketApp API - Enterprise ITSM Backend

## Overview
TicketApp API is a production-grade IT Service Management (ITSM) backend built with Java 21 and Spring Boot. It strictly follows Domain-Driven Design (DDD) and Cloud-Native resiliency patterns, leveraging PostgreSQL concurrency controls, RabbitMQ event-driven messaging, and CQRS materialized views to guarantee SLA tracking under high concurrent load.

## Technology Stack
* **Runtime & Framework:** Java 21, Spring Boot 4.1.0
* **Persistence & Migrations:** PostgreSQL 16, Liquibase, Spring Data JPA
* **Identity & Security:** Keycloak 26 (OAuth 2.0 / OpenID Connect)
* **Messaging & Events:** RabbitMQ 4.0 (AMQP)
* **Caching & Rate Limiting:** Redis 7.4, Bucket4j 8.10 (Lettuce CAS)
* **Object Storage:** MinIO 9 (S3-Compatible API)
* **Observability:** Micrometer, Prometheus, Grafana
* **Testing:** JUnit 5, BDDMockito, AssertJ, Testcontainers

## Key Architectural Highlights

* **Distributed EDA (RabbitMQ):** Mitigates Phantom Events by bridging domain events to AMQP strictly after database commits via `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)`.
* **CQRS & Materialized Views:** Bypasses entity hydration for analytics. Uses background `CONCURRENTLY` refreshes via `JdbcTemplate` to prevent `EXCLUSIVE LOCK` contention on live read traffic.
* **Pessimistic Locking:** Utilizes PostgreSQL native `FOR UPDATE SKIP LOCKED` for deadlock-free, FIFO background workload distribution across horizontally scaled consumers.
* **Stateless Security & IDOR Defense:** Delegates IAM to Keycloak. Enforces a "Fail-Fast" security model via a centralized resource ownership validation gateway (`RequesterContext`), mitigating Insecure Direct Object Reference (IDOR) vulnerabilities.
* **Distributed Rate Limiting:** Enforces Edge API throttling using Bucket4j and Redis Compare-And-Swap (CAS) operations to protect against burst traffic.
* **S3-Compatible Storage:** Streams binary attachments directly to MinIO, isolating the RDBMS from heavy payloads and strictly enforcing path-traversal sanitization.

## Testing Strategy & CI/CD

* **Shift-Left BDD:** Strict `given/when/then` testing structure using BDDMockito and deterministic `Clock.fixed()` time injection to prevent test flakiness.
* **Singleton Testcontainers:** Spins up PostgreSQL, Redis, RabbitMQ, and Keycloak exactly once per JVM run via `@ServiceConnection`, drastically reducing CI pipeline execution time.
* **Cloud-Native CI/CD:** Utilizes Maven Wrapper in batch mode (`-B -ntp`) for build determinism and Spring Boot Cloud Native Buildpacks for secure OCI image generation.

## Local Environment Setup

The project uses `spring-boot-docker-compose` to automatically provision all infrastructure dependencies upon startup.

### Prerequisites
* JDK 21+
* Docker Engine / Docker Desktop

### Quick Start
1. Clone the repository:
   ```bash
   git clone https://github.com/Wilie12/ticketapp-api.git
   cd ticketapp-api
   ```
2. Launch the application:
   ```bash
   ./mvnw spring-boot:run
   ```

### API Documentation
Interactive OpenAPI 3.0 specification is available at: `http://localhost:8080/swagger-ui.html`

### Verification & Build
```bash
# Run the full test suite
./mvnw clean verify -B -ntp

# Build a Cloud-Native OCI container image
./mvnw spring-boot:build-image -B -ntp
```