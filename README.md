# Clean Architecture Bank: Withdrawal Feature (Tutorial)

This repository contains the starter code for a simple Java console application demonstrating Clean Architecture and SOLID principles with PostgreSQL. 

Your goal is to implement the **Withdrawal** feature based on the existing `TransferService` pattern. This guide will walk you through exactly how to do it.

## Prerequisites

- **Java 17**
- **Maven**
- **PostgreSQL**: Running locally on port `5432` with user `postgres` and password `postgres`.
- The database `postgres` should be accessible. The application will automatically create the `account` table and seed it with initial data if it doesn't exist.

## Step-by-Step Implementation

Currently, the project only supports transferring money between accounts. Let's add the ability to withdraw money with a fixed flat fee of `2500` applied to every withdrawal.

### 1. Create the `WithdrawalService`

In Clean Architecture, business use cases belong in the **Service** layer. Since withdrawing money is a different use case than transferring, it should have its own dedicated service class to follow the Single Responsibility Principle (SRP).

Create a new file: `src/main/java/com/example/service/WithdrawalService.java`

```java
package com.example.service;

import com.example.domain.Account;
import com.example.repository.AccountRepository;

/**
 * Service Layer: Use Cases
 * 
 * Orchestrates the withdrawal process. Similar to TransferService,
 * it fetches the domain object, applies business rules (flat fee),
 * and saves the state via the repository. 
 */
public class WithdrawalService {

    private static final double FLAT_FEE = 2500.0;
    private final AccountRepository repository;

    // Dependency Injection via constructor
    public WithdrawalService(AccountRepository repository) {
        this.repository = repository;
    }

    public void withdraw(String accountNumber, double amount) {
        // 1. Fetch the account from the database
        Account account = repository.findByAccountNumber(accountNumber);
        
        // 2. Apply business logic: Deduct the requested amount plus our fixed flat fee
        account.debit(amount + FLAT_FEE);
        
        // 3. Save the updated balance back to the database
        repository.update(account);
    }
}
```

### 2. Update `Main.java` to Use the New Service

Now that our use case exists, we need to wire it up in our entry point (`Main.java`). This acts as our "Composition Root" where dependencies are injected, and database connections/transactions are managed.

Open `src/main/java/com/example/Main.java`.

First, add the import at the top:
```java
import com.example.service.WithdrawalService;
```

Next, right after instantiating `TransferService`, create an instance of `WithdrawalService` passing the repository:
```java
            // Dependency Injection: Pass the pieces into each other
            AccountRepository repository = new PostgresAccountRepository(conn); 
            TransferService transferService = new TransferService(repository);
            
            // Instantiating the new service
            WithdrawalService withdrawalService = new WithdrawalService(repository);
```

Finally, add the logic to test the withdrawal under your existing transfer test, remembering to execute it within a `try-catch` block so failures rollback safely:

```java
            System.out.println("Processing first withdrawal of 5000 from A001...");
            try {
                withdrawalService.withdraw("A001", 5000);
            } catch (Exception e) {
                System.out.println("Withdrawal failed: " + e.getMessage());
                // ACID: Rollback any intermediate state to keep data safe
                conn.rollback();
            }

            System.out.println("Processing second withdrawal of 10000 from A002...");
            try {
                withdrawalService.withdraw("A002", 10000);
                System.out.println("Withdrawal successful.");
            } catch (Exception e) {
                System.out.println("Withdrawal failed: " + e.getMessage());
                // ACID: Cancel the transaction safely
                conn.rollback();
            }
```

## How to Run

Once everything is implemented, compile and run the application using Maven:

```bash
mvn clean compile
mvn exec:java
```

### Expected Output

If implemented correctly, running the console application should produce output similar to:

```text
Processing transfer of 25000 from A001 to A002...
Processing first withdrawal of 5000 from A001...
Processing second withdrawal of 10000 from A002...
Withdrawal successful.
-------------------------------------------
All operations success!
-------------------------------------------
Final Balance A001: 67500.0
Final Balance A002: 62500.0
```

## Architecture Summary
- **Domain Layer (`Account.java`)**: Handles the core validation (sufficient balances, invalid amounts).
- **Service Layer (`TransferService`, `WithdrawalService`)**: Orchestrates the behavior (get data -> trigger domain rules -> save data).
- **Repository Interface (`AccountRepository`)**: Defines the data-access contract so the service doesn't care about SQL.
- **Repository Implementation (`PostgresAccountRepository`)**: Handles actual SQL queries and `PreparedStatement` to prevent SQL injection.
- **Entry Point (`Main.java`)**: Cements everything together and handles transaction commits and rollbacks (ACID guarantees).
