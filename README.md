# TicketApp API - Helpdesk Backend

> **Status: MVP Completed**
> *The Minimum Viable Product for the Helpdesk API is fully implemented. It features a robust asynchronous assignment queue, an OAuth 2.0 secured Edge layer, and a high-performance CQRS analytical read model.*

## Overview

TicketApp API is a commercial-grade backend service designed for IT Support operations. Built with a strict adherence to
Domain-Driven Design (DDD) and Security by Design principles, the application acts as an OAuth 2.0 Resource Server. It utilizes advanced PostgreSQL features and Event-Driven architecture to ensure consistency, high availability, and optimal performance under heavy load.

## Core Features Implemented

* **Event-Driven Ticket Queue (FIFO):** Asynchronous auto-assignment engine utilizing PostgreSQL Pessimistic Locking (`FOR UPDATE SKIP LOCKED`) to prevent Race Conditions during concurrent agent polling.
* **CQRS Analytics Engine:** Performance-optimized read model using PostgreSQL `MATERIALIZED VIEW` with `CONCURRENTLY` background refreshes to aggregate SLA breaches without blocking operational transactions.
* **IAM & Stateless Security:** Full Keycloak integration. Edge-layer authorization with robust IDOR protection and Context Object propagation (`RequesterContext`).
* **S3-Compatible Object Storage:** Idempotent attachment handling powered by MinIO, decoupling heavy binary streaming from the relational database.
* **Resilience & Rate Limiting:** Token Bucket algorithm implemented via `Bucket4j` to mitigate DoS attacks and prevent thread pool starvation.
* **Shift-Left Error Handling:** Global RFC 7807 (`ProblemDetail`) compliance with robust Domain Exception mapping and compile-time DTO projections (MapStruct).
* **Enterprise Observability:** Real-time telemetry and custom event-driven business metrics exposed via Micrometer and Spring Boot Actuator, scraped by Prometheus and visualized in Grafana.

## Technology Stack

* **Language:** Java 21+
* **Framework:** Spring Boot 4
* **Database:** PostgreSQL 16 (Managed via Liquibase)
* **Security & IAM:** Keycloak (OAuth 2.0 / JWT Authentication)
* **Object Storage:** MiniIO (S3 Compatible)
* **Email Testing:** Mailpit
* **Containerization:** Docker & Docker Compose
* **CI/CD:** GitHub Actions 
* **Testing:** JUnit 5, AssertJ, Mockito, Testcontainers
* **Observability:** Spring Boot Actuator, Micrometer, Prometheus, Grafana

## How to Run Locally

### Prerequisites

* Java 21 or higher installed
* Docker Desktop (or Docker Engine) running

### Starting the Application

Thanks to the `spring-boot-docker-compose` dependency, you don't need to start containers manually.
1. Clone the repository:
   ```bash
   git clone https://github.com/Wilie12/ticketapp-api.git
   ```

2. Duplicate the environment variables template and configure (optional for local dev):
    ```bash
   cp .env.example .env
   ```

3. Run the application using the Maven wrapper:
   ```bash
   ./mvnw spring-boot:run
   ```

### Running Tests

```bash
   ./mvnw clean verify
```