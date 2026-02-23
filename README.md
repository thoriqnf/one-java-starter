# Clean Architecture Bank Transfer & Withdrawal

A simple Java console application demonstrating Clean Architecture and SOLID principles with PostgreSQL. Features include money transfer between accounts and withdrawals with a dynamic fee calculator (Strategy Pattern).

## Prerequisites

- **Java 17**
- **Maven**
- **PostgreSQL**: Running locally on port `5432` with user `postgres` and password `postgres`.
- **Database Setup**: 
  1. Create a database named `bankdb`: `CREATE DATABASE bankdb;`
  2. Run the following SQL setup to prepare the `account` table:
  ```sql
  CREATE TABLE account (
      account_number VARCHAR(20) PRIMARY KEY,
      balance NUMERIC(15,2) NOT NULL
  );

  INSERT INTO account VALUES ('A001', 1000);
  INSERT INTO account VALUES ('A002', 500);
  ```

## How to Run

Navigate to the project directory and execute the following maven commands to compile and run the application:

```bash
mvn clean compile
mvn exec:java
```

### Expected Output

```text
Processing transfer of 100 from A001 to A002...
Processing same-bank withdrawal of 50 from A001...
Processing other-bank withdrawal of 50 from A002...
Other-bank withdrawal failed as expected (insufficient balance for 2500 fee): Insufficient balance
All operations success!
```

## Architecture highlights
- **Clean Architecture Focus**: Separated `domain`, `repository`, and `service` layers.
- **SOLID**:
  - SRP (Single Responsibility Principle): `Account` domain class handles domain rules, services orchestrate use cases.
  - DIP (Dependency Inversion Principle): Services depend on `AccountRepository` interface, not the concrete `PostgresAccountRepository`.
- **Data Safety**: Uses `PreparedStatement` to prevent SQL Injection, and `try-with-resources` for JDBC objects. Proper connection transaction commits are enforced in the main entrypoint.
