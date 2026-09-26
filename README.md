# TicketApp API - Enterprise Helpdesk & ITSM Backend

> **Status: MVP Completed**
> *Commercial-grade IT Service Management (ITSM) backend featuring an asynchronous event-driven FIFO assignment engine, an OAuth 2.0 secured Edge layer, S3-compatible binary storage, and a CQRS analytical read model.*

## Overview

TicketApp API is a production-ready Helpdesk backend service engineered for IT Support and Service Desk operations. Built with **Java 21** and **Spring Boot 4**, the system strictly follows **Domain-Driven Design (DDD)**, **Resource-Oriented Architecture (ROA)**, and **Security by Design** principles.

Acting as a stateless **OAuth 2.0 Resource Server** integrated with **Keycloak**, the application leverages advanced **PostgreSQL 16** concurrency controls, asynchronous domain events, and non-blocking materialized view refreshes to guarantee data integrity, strict Service Level Agreement (SLA) tracking, and high throughput under concurrent load.

---

## Key Architectural Highlights

* **Event-Driven FIFO Ticket Queue & Concurrency Control:** Newly created tickets and agent availability transitions publish domain events (`TicketCreatedEvent`, `TicketResolvedEvent`, `AgentAvailableEvent`) processed asynchronously after transaction commit (`@TransactionalEventListener(phase = AFTER_COMMIT)`). The `TicketAssignmentOrchestrator` auto-assigns backlog items (up to 5 active tickets per agent) using native PostgreSQL pessimistic locking (`FOR UPDATE SKIP LOCKED`) to eliminate race conditions and deadlocks across concurrent worker threads.
* **CQRS Analytics Engine (Materialized Views):** Statistical dashboards bypass operational entity hydration entirely. `AgentStatsRepository` queries a pre-aggregated PostgreSQL Materialized View (`agent_stats_mv`) via Spring Data JPA Projections (`AgentStatsProjection`). A background scheduler (`AgentStatsRefreshScheduler`) refreshes the view periodically via `JdbcTemplate` using `REFRESH MATERIALIZED VIEW CONCURRENTLY`, preventing `EXCLUSIVE LOCK` contention on live read traffic.
* **Stateless OAuth 2.0 Security & Centralized IDOR Protection:** Authentication is delegated to **Keycloak**. At the Edge layer, `KeycloakRealmRoleConverter` maps realm roles, while a custom `CurrentRequesterArgumentResolver` (`@CurrentRequester`) resolves the `RequesterContext` (encapsulating `UUID userId` and `AccessLevel`: `STANDARD`, `AGENT`, `ADMIN`). Resource mutations enforce a "Fail-Fast" security model (`requireInternal()` and centralized ownership/assignment validation in `TicketService.getValidatedTicket()`).
* **Anti-Corruption Layer (ACL) & Cached IAM Gateway:** Personal Identifiable Information (PII) such as user email addresses is never duplicated in the local database. `KeycloakIdentityGateway` fetches recipient emails on demand via an OAuth 2.0 `client_credentials` service account and caches responses in **Caffeine** (`identityCache`) with strict size and TTL boundaries.
* **S3-Compatible Object Storage & Path Traversal Guards:** Ticket attachments are streamed directly to **MinIO** via an abstracted `StorageService` interface, keeping binary payloads out of PostgreSQL. `AttachmentService` enforces strict filename sanitization, active-ticket state checks, and ownership verification prior to deletion.
* **Edge Rate Limiting & RFC 7807 Error Contract:** `RateLimitInterceptor` implements the Token Bucket algorithm via **Bucket4j** (50 requests/minute per authenticated `sub` claim or client IP). All domain, validation, and rate-limit errors are handled by decentralized, controller-scoped `@RestControllerAdvice` components returning standardized **RFC 7807 (`ProblemDetail`)** payloads stamped with an injected `java.time.Clock`.
* **Asynchronous HTML Notifications & Observability:** Lifecycle events trigger non-blocking HTML emails rendered via **Thymeleaf** (`EmailTemplateProcessor`) and dispatched through `JavaMailSender` on an isolated `ThreadPoolTaskExecutor`. Custom business counters (`tickets.created.total`, `tickets.resolved.total` tagged by `team_id`) are recorded via **Micrometer** (`TicketMetricsListener`), exposed through Spring Boot Actuator, scraped by **Prometheus**, and visualized in **Grafana**.

---

## Project Structure

```text
src/main/java/com/nn/ticketapp_api
├── TicketappApiApplication.java
├── admin/                          # SLA policy configuration domain (AdminSlaController, SlaConfiguration)
├── agent/                          # Agent profiles, availability status, and AgentAvailableEvent
├── communication/                  # Public comments, internal work notes, system events & MinIO attachments
├── notification/                   # Async AFTER_COMMIT email listeners & Thymeleaf HTML template processor
├── team/                           # Support department (Team) lifecycle management
├── ticket/                         # Core Bounded Context
│   ├── api/                        # Immutable Java Record DTOs, MapStruct mappers, TicketExceptionHandler
│   ├── config/                     # Validated @ConfigurationProperties (AnalyticsProperties)
│   ├── controller/                 # Resource-oriented controllers (TicketController, AgentController, AdminController)
│   ├── domain/                     # Rich Ticket Aggregate Root, state machine (TicketStatus), pure SlaPolicy
│   ├── facade/                     # TicketFacade (cross-domain aggregation of tickets, notes & attachments)
│   ├── listener/                   # TicketAssignmentOrchestrator & TicketMetricsListener (Micrometer)
│   ├── repository/                 # TicketRepository (SKIP LOCKED) & AgentStatsRepository (CQRS projection)
│   ├── scheduler/                  # AgentStatsRefreshScheduler (non-transactional CONCURRENTLY refresh)
│   └── service/                    # TicketService & AgentStatsService
└── shared/                         # Cross-cutting infrastructure
    ├── api/                        # GlobalExceptionHandler & framework-agnostic PageResponse<T>
    ├── config/                     # Clock, Async ThreadPool, Caffeine Cache, Scheduling & WebMvc configs
    ├── identity/                   # Keycloak Admin Client ACL Gateway & IdentityProperties
    ├── security/                   # OAuth2 SecurityFilterChain, @CurrentRequester resolver & Bucket4j rate limiter
    └── storage/                    # S3/MinIO client auto-bucket initialization & StorageService adapter
```

---

## Technology Stack

* **Language & Runtime:** Java 21
* **Framework:** Spring Boot 4.1.0 (WebMvc, Data JPA, Security OAuth2 Resource Server, Validation, Mail, Cache, Actuator)
* **Database & Migrations:** PostgreSQL 16, Liquibase (Evolutionary Database Design, Sequences, Materialized Views)
* **Identity & Access Management:** Keycloak 26.0.7 (OAuth 2.0 / OpenID Connect, Admin Client SDK)
* **Object Storage:** MinIO 9.0.3 (S3-Compatible API)
* **Resilience & Caching:** Bucket4j 8.10.1 (Token Bucket Rate Limiting), Caffeine 3.0.5
* **Mapping & Templating:** MapStruct 1.5.5 (Compile-Time Projections), Thymeleaf (HTML Emails), Lombok
* **Observability:** Micrometer, Prometheus, Grafana
* **Testing:** JUnit 5, BDDMockito, AssertJ, Spring Security Test, Testcontainers (`postgresql`, `testcontainers-keycloak`)
* **Build & CI/CD:** Apache Maven (Maven Wrapper), GitHub Actions, Cloud Native Buildpacks

---

## API Overview & Documentation

When the application is running, interactive **OpenAPI 3.0 / Swagger UI** documentation is available at:
* **Swagger UI:** `http://localhost:8080/swagger-ui.html`
* **OpenAPI JSON Specification:** `http://localhost:8080/v3/api-docs`
* **Static Contract Reference:** See [`openapi.yaml`](./openapi.yaml) in the repository root.

### Endpoints & Access Matrix

All `/api/v1/**` endpoints require a valid Bearer JWT issued by Keycloak (`Authorization: Bearer <token>`).

| Method | Endpoint | Access Level | Success Status | Description |
| :--- | :--- | :---: | :---: | :--- |
| **Tickets (Core & Workflow)** | | | | |
| `POST` | `/api/v1/tickets` | Authenticated | `201 Created` | Creates a new support ticket (`INCxxxxxxx`), calculates SLA deadline, and triggers async queue evaluation. |
| `GET` | `/api/v1/tickets` | Authenticated | `200 OK` | Retrieves all tickets created by the authenticated user. |
| `GET` | `/api/v1/tickets/{id}` | Owner / Assigned Agent / `ADMIN` | `200 OK` | Aggregates ticket details, comments, attachments, and (for internal roles) work notes & system events. |
| `PATCH` | `/api/v1/tickets/{id}` | `AGENT` (Assigned) / `ADMIN` | `200 OK` | Updates ticket title, priority, or target team (unassigns current agent on team transfer). |
| `GET` | `/api/v1/tickets/queue` | `AGENT` / `ADMIN` | `200 OK` | Retrieves paginated unassigned (`NEW`) tickets for a specific `teamId` ordered oldest-first (FIFO). |
| `POST` | `/api/v1/tickets/{id}/assign` | `AGENT` / `ADMIN` | `200 OK` | Manually assigns a `NEW` ticket to the requesting agent and transitions status to `IN_PROGRESS`. |
| `POST` | `/api/v1/tickets/{id}/resolve` | `AGENT` (Assigned) / `ADMIN` | `200 OK` | Transitions ticket to `RESOLVED`, records a mandatory resolution note, and triggers capacity re-evaluation. |
| `POST` | `/api/v1/tickets/{id}/close` | Owner / Assigned Agent / `ADMIN` | `200 OK` | Transitions ticket to terminal `CLOSED` state (blocks all subsequent modifications). |
| `POST` | `/api/v1/tickets/{id}/reopen` | Owner / Assigned Agent / `ADMIN` | `200 OK` | Reopens a `RESOLVED` ticket back to `IN_PROGRESS` while preserving the original SLA deadline. |
| **Communications & Attachments** | | | | |
| `POST` | `/api/v1/tickets/{id}/comments` | Authenticated | `201 Created` | Adds a `PUBLIC_COMMENT` to an active (non-closed) ticket. |
| `POST` | `/api/v1/tickets/{id}/work-notes` | `AGENT` / `ADMIN` | `201 Created` | Adds an internal `WORK_NOTE` visible only to support staff. |
| `POST` | `/api/v1/tickets/{ticketId}/attachments` | Authenticated | `201 Created` | Streams a multipart file (max 5MB) to MinIO object storage and persists metadata. |
| `DELETE` | `/api/v1/tickets/{ticketId}/attachments/{attachmentId}` | Attachment Owner | `204 No Content` | Deletes attachment binary from MinIO and removes database metadata. |
| **Agent Operations & Analytics** | | | | |
| `GET` | `/api/v1/agents/me/tickets` | `AGENT` / `ADMIN` | `200 OK` | Returns a paginated backlog of active (`IN_PROGRESS`, `RESOLVED`) tickets assigned to the agent. |
| `PUT` | `/api/v1/agents/me/status` | `AGENT` / `ADMIN` | `204 No Content` | Updates agent availability (`OFFLINE`, `AVAILABLE`, `BUSY`, `AWAY`). Setting `AVAILABLE` triggers queue polling. |
| `GET` | `/api/v1/agents/me/stats` | `AGENT` / `ADMIN` | `200 OK` | Queries `agent_stats_mv` for the agent's open, resolved, and SLA-breached ticket counts. |
| **Administration (Teams, Agents & SLA)** | | | | |
| `POST` | `/api/v1/agents` | `ADMIN` | `201 Created` | Provisions a new `AgentProfile` linked to a Keycloak user UUID and support `Team`. |
| `POST` | `/api/v1/teams` | `ADMIN` | `201 Created` | Creates a new IT Support department (`Team`). |
| `GET` | `/api/v1/teams` | Authenticated | `200 OK` | Lists all available support teams. |
| `GET` | `/api/v1/teams/{id}` | Authenticated | `200 OK` | Retrieves details of a specific support team. |
| `PATCH` | `/api/v1/teams/{id}` | `ADMIN` | `200 OK` | Updates a support team's name or description. |
| `PUT` | `/api/v1/admin/sla-configs/{priority}` | `ADMIN` | `200 OK` | Configures target SLA resolution hours for a `TicketPriority` (`LOW`, `MEDIUM`, `HIGH`, `CRITICAL`). |
| `GET` | `/api/v1/admin/stats/agents` | `ADMIN` | `200 OK` | Retrieves global performance and SLA breach metrics across all agents from `agent_stats_mv`. |
| `GET` | `/api/v1/admin/tickets` | `ADMIN` | `200 OK` | Returns a paginated global view of all tickets in the system. |

---

## How to Run Locally

### Prerequisites

* **JDK 21** or higher installed
* **Docker Desktop** (or Docker Engine with Docker Compose) running

### 1. Starting the Application

Thanks to `spring-boot-docker-compose` integration and safe fallback defaults in `application.yaml`, the application automatically provisions and starts all required infrastructure containers defined in `compose.yaml` upon startup:

1. Clone the repository:
   ```bash
   git clone https://github.com/Wilie12/ticketapp-api.git
   cd ticketapp-api
   ```

2. *(Optional)* Copy the environment template if you wish to override default local credentials:
   ```bash
   cp .env.example .env
   ```

3. Launch the service using the repository-bound Maven Wrapper:
   ```bash
   ./mvnw spring-boot:run
   ```

### 2. Local Infrastructure Services (`compose.yaml`)

Once started, the following services and web consoles are accessible on your host machine:

| Service | Container Name | Host Port(s) | Web Console / Endpoint |
| :--- | :--- | :--- | :--- |
| **TicketApp API** | *(Host JVM)* | `8080` | `http://localhost:8080/swagger-ui.html` |
| **PostgreSQL 16** | `ticketapp-postgres` | `5432` | `jdbc:postgresql://localhost:5432/ticketapp` |
| **Keycloak 26 IAM** | `ticketapp-keycloak` | `8081` | `http://localhost:8081` |
| **MinIO Object Storage** | `ticketapp-minio` | `9000` (API), `9001` (UI) | `http://localhost:9001` |
| **Mailpit (SMTP Catcher)** | `ticketapp-mailpit` | `1025` (SMTP), `8025` (UI) | `http://localhost:8025` |
| **Prometheus** | `ticketapp-prometheus` | `9090` | `http://localhost:9090` |
| **Grafana** | `ticketapp-grafana` | `3000` | `http://localhost:3000` |

---

## Testing Strategy & CI/CD

The project enforces a strict **Shift-Left** testing standard structured in BDD style (`// given`, `// when`, `// then` via `BDDMockito`) across three isolated layers:

1. **Domain & Service Unit Tests:** Fast, isolated verification of `Ticket` state-machine invariants, pure `DefaultSlaPolicy` calculations, and service orchestration with deterministic `Clock.fixed(...)` time injection.
2. **Web Slice Tests (`@WebMvcTest`):** All controller tests extend `BaseControllerTest`, verifying HTTP status codes, JSON serialization, Jakarta validation errors (`ProblemDetail`), and OAuth 2.0 role access rules using `SecurityTestUtils.validJwt(userId, role)`.
3. **Persistence & Event Integration Tests (`@SpringBootTest`):** Extend `BaseIntegrationTest`, which implements the **Singleton Container Pattern** using **Testcontainers** to spin up real **PostgreSQL 16** (with full Liquibase migrations and `agent_stats_mv` materialized view) and **Keycloak 26** (pre-configured with `ticketapp-realm.json`) once per JVM test run.

### Running the Full Verification Suite

Execute the deterministic build and test suite using the Maven Wrapper in batch mode:

```bash
./mvnw clean verify -B -ntp
```

### Building a Cloud-Native OCI Container Image

The project uses Spring Boot **Cloud Native Buildpacks** instead of raw Dockerfiles to produce optimized, security-patched OCI images:

```bash
./mvnw spring-boot:build-image -B -ntp
```