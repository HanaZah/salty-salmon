
# FinAdvise-Core: Modern Wealth Management Backend [WIP]

## 🚀 The Mission
Transitioning from a background in Delphi development to Modern Enterprise Java, I built this project to demonstrate basic proficiency in building secure, scalable, and testable systems. It makes use of my previous financial career, where similar applications were a staple.

What begun as a simple "how would I do it?" hobby project is now a showcase of **Clean Architecture**, **Stateless Security**, and **Defensive Programming**.


## 🛠 Tech Stack

**Language:** Java 21 (utilizing Pattern Matching and Records)

**Framework:** Spring Boot 4.0.3

**Build System**: Gradle (Kotlin DSL)

**Security**: Spring Security 6+ (OAuth2 Resource Server / JWT)

**Data**: Oracle DB (Latest) with Hibernate/JPA

**Utilities**: Lombok, HashIds (for URL-safe ID obfuscation), .env for secret management

**Testing**: JUnit 5, Mockito, Testcontainers (Oracle-specific integration tests)

**Infrastructure**: Docker Compose

## ![Swagger](https://img.shields.io/badge/-Swagger-%23Clojure?style=flat-square&logo=swagger&logoColor=white) Interactive API Documentation
This project includes a fully integrated Swagger UI for real-time API exploration.

#### 1. Access the UI: 
 - Navigate to http://localhost:8080/swagger-ui.html while the app is running.

#### 2. Authorize: 
 - Use the POST */api/v1/auth/login* endpoint with your .env credentials to receive a JWT.
 - Click the "Authorize" button at the top of the Swagger page.
 - Paste your token to unlock protected User and Address endpoints.

#### 3. Explore Schema: 
 - All error responses strictly follow the RFC 7807 (Problem Detail) standard.
 - Validation failures include a custom errors extension for granular field-level feedback, as documented in the "Schemas" section.

## 🏗 Architectural Highlights

### 1. Identity & Security (users package)
- **Stateless Authentication**: Fully implemented OAuth2 Resource Server issuing signed JWTs, complete with custom encoder, decoder and claims extractor.
- **Role-Based Access Control (RBAC)**: Granular security at the method level using @PreAuthorize.
- **Secure-by-Design**: User identity is extracted directly from the JWT subject in the Controller layer, preventing IDOR (Insecure Direct Object Reference) attacks on profile/password updates.
- **Bootstrapping**: Includes a CommandLineRunner seeder that automatically creates the initial System Admin using environment variables, ensuring zero-manual-setup for new deployments.
- **ID Obfuscation & Business Keys**: Uses HashIds to generate unique, non-sequential employeeId values from internal database sequences. These are persisted as unique business keys, ensuring that internal Long IDs are never exposed and preventing IDOR or enumeration attacks.

### 2. Normalization & Validation (addresses package)
- **Manual mapping**: Uses custom mapper to map highly normalized nested entities that constituite full address into easily traversable flat DTO.
- **Normalized Persistence**: Implements a findOrCreate strategy to prevent duplicate address entries in a highly normalized database schema.
- **Extensible Validation**: Features an ExternalAddressValidator interface. While currently using internal logic only, it is architected to support third-party integrations (like RÚIAN) without modifying core service logic.

### 3. Testing Philosophy
- **Reliable Integration**: I don't believe in "H2 for tests, Oracle for Prod." My E2E tests use Testcontainers to spin up a real Oracle instance, ensuring that sequence behaviors and SQL dialects are identical to production.
- **Defensive Coverage**: Tests include "unhappy paths" (e.g., password reuse, invalid credentials, unauthorized access) for critical points to ensure the GlobalExceptionHandler and ProblemDetail responses are consistent.

## 🚦 Getting Started

#### 1. Clone the Repository

#### 2. Environment Setup: Copy .env.example to .env.

#### 3. Launch Database: 

```bash
docker-compose up -d
```

#### 4. Run Application:

```bash
  ./gradlew bootRun
```

*The system will automatically seed a default administrator based on your .env values.*


    
## 📈 Roadmap

[x] **Core Identity & Security**: Stateless JWT, RBAC, and Secure Profile Management.

[x] **Address Normalization**: Multi-entity address sharing with findOrCreate logic.

[ ] **Client Onboarding**: KYC-compliant onboarding and CRM administration.

[ ] **Contract & Portfolio Management**: Managing client-specific financial products mapped to global Providers and Product Types.

[ ] **Analytical Engine**: 
- Cash Flow Assessment: Automated analysis of client income vs. expenditure.

- Investment Modeling: Advanced projections and wealth growth simulations.

## 💡 Why this project?
I am moving away from legacy systems to embrace the type of code I enjoy: **Type-safe**, **highly tested**, and properly **decoupled**.
