package com.example.repository;

import com.example.domain.Account;

/**
 * Repository Interface
 * 
 * SOLID Principle: DIP (Dependency Inversion Principle)
 * The Service layer depends on this abstraction, NOT on the concrete PostgreSQL implementation.
 * This allows us to easily swap out the database (e.g., to MySQL or MongoDB) without modifying the business logic.
 */
public interface AccountRepository {
    Account findByAccountNumber(String accountNumber);
    void update(Account account);
}
