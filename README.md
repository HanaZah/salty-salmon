
# FinAdvise-Core: Modern Wealth Management Backend 

## Status: Archived / V1 Prototype
This repository represents the initial exploratory phase of the CRM, featuring complex infrastructure integrations (AWS S3, asynchronous processing, CRON jobs).

*I am currently rebuilding this project in a new repository, starting with MVP, focusing strictly on core architectural fundamentals, REST semantics, and optimized JPA performance.*

## 🚀 The Mission
Transitioning from a background in Delphi development to Modern Enterprise Java, I built this project to demonstrate basic proficiency in building secure, scalable, and testable systems. It makes use of my previous financial career, where similar applications were a staple. What began as a simple "how would I do it?" hobby project is now a showcase of Clean Architecture, Stateless Security, and Defensive Programming.

## 🛠 Tech Stack
* **Language**: Java 21 utilizing Pattern Matching and Records.
* **Framework**: Spring Boot 4.0.3.
* **Build System**: Gradle using Kotlin DSL.
* **Security**: Spring Security 6+ with OAuth2 Resource Server and JWT.
* **Data**: Oracle DB with Hibernate/JPA.
* **Document Storage**: S3-compatible MinIO using Spring Cloud AWS.
* **Utilities**: Lombok, HashIds for ID obfuscation, Apache Tika for MIME type detection, and `.env` for secrets.
* **Testing**: JUnit 5, Mockito, and Testcontainers for Oracle integration tests.
* **Infrastructure**: Docker Compose for Oracle and MinIO services.

## ![Swagger](https://img.shields.io/badge/-Swagger-%23Clojure?style=flat-square&logo=swagger&logoColor=white) Interactive API Documentation
This project includes a fully integrated Swagger UI for real-time API exploration.

#### 1. Access the UI: 
 * Navigate to http://localhost:8080/swagger-ui.html while the app is running.

#### 2. Authorize: 
 * Use the POST */api/v1/auth/login* endpoint with default admin credentials to receive a JWT (employeeId is provided by the app in console output on first startup, password is in your .env file).
 * Click the "Authorize" button at the top of the Swagger page.
 * Paste your token to unlock all protected endpoints.

#### 3. Explore Schema: 
 * All error responses strictly follow the RFC 7807 (Problem Detail) standard.
 * Validation failures include a custom errors extension for granular field-level feedback, as documented in the "Schemas" section. 

## 🏗 Architectural Highlights
* **Identity & Security**: Fully implemented OAuth2 Resource Server issuing signed JWTs. Uses Role-Based Access Control (RBAC) via `@PreAuthorize`. Protects against IDOR by extracting user identity directly from the JWT subject. Includes a `CommandLineRunner` seeder for zero-manual-setup admin creation.
* **ID Obfuscation**: Uses HashIds to generate unique, non-sequential `employeeId` values from internal database sequences, keeping internal Long IDs hidden.
* **Address Normalization**: Uses a custom mapper for highly normalized nested entities and implements a `findOrCreate` strategy to prevent duplicate entries. Designed with an extensible `ExternalAddressValidator` interface.
* **Core CRM & Domain**: Complete client lifecycle management, budget tracking (incomes and expenses), and asset management implementation.
* **Document Management**: Asynchronous file uploading orchestrated with a `CompletableFuture` executor pool, integrating with S3/MinIO storage.
* **Automated Scheduled Jobs**: Cron-based scheduling for birthday/anniversary rollovers and automated cleanup of soft-deleted documents.

## 🚦 Getting Started
1. Clone the Repository.
2. Copy `.env.example` to `.env` and fill in the required database, MinIO, and HashIds secrets.
3. Run `docker compose up` to launch the Oracle database and MinIO storage.
4. Start the Spring Boot application  `./gradlew bootRun` (the system will automatically seed a default administrator based on your `.env` values).

## 📈 Roadmap
* [x] **Core Identity & Security**: Stateless JWT, RBAC, and Secure Profile Management.
* [x] **Address Normalization**: Multi-entity address sharing with `findOrCreate` logic.
* [x] **Client Onboarding**: KYC-compliant onboarding and CRM administration.
* [x] **Contract & Portfolio Management**: Managing client-specific financial products mapped to global Providers and Product Types.
* [x] **Analytical Engine**: Cash Flow Assessment & Budget Tracking.
* [ ] **Advanced Investment Modeling**: Advanced projections and wealth growth simulations.

---
💡 **Why this project?**
I am moving away from legacy systems to embrace the type of code I enjoy: Type-safe, highly tested, and properly decoupled.
