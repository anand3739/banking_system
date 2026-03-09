# Banking Transaction Management System

Backend project in Java 17 + Spring Boot for managing banking accounts, ACID-safe transactions, and rule-based fraud detection.

## Tech Stack

- Java 17
- Spring Boot 3
- Spring Data JPA + Hibernate
- PostgreSQL
- Maven
- REST APIs
- Lombok

## Features

### Account Management
- Create account
- Get account details
- Check balance

### Transaction Management
- Deposit money
- Withdraw money
- Transfer money between accounts
- Transaction history by account

### ACID Compliance
- `@Transactional` is used on deposit/withdraw/transfer operations.
- Transfer uses pessimistic row locking (`PESSIMISTIC_WRITE`) to prevent race conditions.
- Debit and credit are committed together or rolled back together.

### Fraud Detection (Rule-based)
- Flags transactions above `100000` (configurable)
- Detects more than `5` transactions in `10` minutes from same account (configurable)
- Detects transfers to blacklisted accounts (configurable)
- Fraud alerts are persisted in `fraud_alerts` table

## API Endpoints

- `POST /customers`
- `POST /accounts`
- `GET /accounts/{id}`
- `GET /accounts/{id}/balance`
- `POST /transactions/deposit`
- `POST /transactions/withdraw`
- `POST /transactions/transfer`
- `GET /transactions/history/{accountId}`
- `GET /fraud-alerts`

## Project Structure

```text
src/main/java/com/example/banking
|-- config
|-- controller
|-- dto
|-- entity
|-- enums
|-- exception
|-- repository
`-- service
```

## Configuration

Main config files:
- `src/main/resources/application.properties`
- `src/main/resources/application.yml`

Database values (PostgreSQL):
- `spring.datasource.url=${SPRING_DATASOURCE_URL}`
- `spring.datasource.username=${SPRING_DATASOURCE_USERNAME}`
- `spring.datasource.password=${SPRING_DATASOURCE_PASSWORD}`
- `spring.jpa.hibernate.ddl-auto=update`
- `spring.jpa.show-sql=true`

Render environment variables:
- `SPRING_DATASOURCE_URL=jdbc:postgresql://host:5432/database`
- `SPRING_DATASOURCE_USERNAME=username`
- `SPRING_DATASOURCE_PASSWORD=password`

Fraud config:
- `fraud.amount-threshold=100000`
- `fraud.tx-count-threshold=5`
- `fraud.tx-window-minutes=10`
- `fraud.blacklisted-account-ids=[999001,999002,999003]`

## Database Schema

Sample SQL schema is included in:
- `src/main/resources/schema.sql`

Tables:
- `customers`
- `accounts`
- `transactions`
- `fraud_alerts`

## How to Run (Local)

1. Start PostgreSQL and create DB `banking_db`.
2. Set env vars:
   ```bash
   SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/banking_db
   SPRING_DATASOURCE_USERNAME=postgres
   SPRING_DATASOURCE_PASSWORD=postgres
   ```
3. Build and run:
   ```bash
   mvn clean spring-boot:run
   ```
4. App runs on: `http://localhost:8080`

## How to Run (Docker)

```bash
docker compose up --build
```

This starts:
- PostgreSQL on `localhost:5432`
- Application on `localhost:8080`

## Example API Requests

Ready-to-run sample requests are available in:
- `examples/api-requests.http`

You can also use these example curls:

```bash
curl -X POST http://localhost:8080/customers \
  -H "Content-Type: application/json" \
  -d '{"name":"Anand Kumar","email":"anand@example.com","phone":"+919876543210"}'
```

```bash
curl -X POST http://localhost:8080/transactions/transfer \
  -H "Content-Type: application/json" \
  -d '{"senderAccountId":1,"receiverAccountId":2,"amount":120000}'
```

## Web Frontend

A browser-based control panel is included and served by Spring Boot:
- URL: `http://localhost:8080`
- Files:
  - `src/main/resources/static/index.html`
  - `src/main/resources/static/style.css`
  - `src/main/resources/static/app.js`

What you can do from the page:
- Create customers and accounts
- Get account details and balance
- Deposit, withdraw, and transfer money
- View transaction history
- View fraud alerts

If your API is hosted on another port or host, set the `API Base URL` field in the page header.

## Validation and Exception Handling

- DTO validation via `jakarta.validation` annotations
- Centralized error handling via `@RestControllerAdvice`
- Standardized error payload in `ErrorResponse`

## Logging

- Transaction operations and fraud events are logged with SLF4J.

## Notes

- Maven was not available in the current execution environment, so compilation/tests could not be run here.
- The project is fully scaffolded and ready to build in a standard Java + Maven environment.
