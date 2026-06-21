# TicketApp API - Helpdesk Backend

> **Work in Progress (WIP)**
> *This Helpdesk API is currently under active development. The core infrastructure (Docker, Keycloak, PostgreSQL,
CI/CD) is established, and I am currently implementing the business logic for ticket management.*

## Overview

TicketApp API is a robust and secure backend service designed for Helpdesk operations. Built with a strong emphasis on
Security by Design, the application acts as an OAuth 2.0 Resource Server, utilizing modern architectural patterns to
ensure scalability, maintainability, and seamless deployment.

## Technology Stack

* **Language:** Java 21+
* **Framework:** Spring Boot 4
* **Database:** PostgreSQL 16
* **Security & IAM:** Keycloak (OAuth 2.0 / JWT Authentication)
* **Database Migrations:** Liquibase
* **Email Testing:** Mailpit
* **Containerization:** Docker & Docker Compose
* **CI/CD:** GitHub Actions (Automated Maven Builds & Tests)
* **Testing:** JUnit 5, Testcontainers

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

2. Run the application using the Maven wrapper:
   ```bash
   ./mvnw spring-boot:run
   ```

### Running Tests

```bash
   ./mvnw clean verify
```