package com.example.repository;

import com.example.domain.Transaction;

import java.util.List;

/**
 * Repository Interface for Transactions
 *
 * SOLID Principle: DIP (Dependency Inversion Principle)
 * The Service layer depends on this abstraction, not on the concrete PostgreSQL implementation.
 */
public interface TransactionRepository {

    /**
     * Save a new transaction record to the database.
     */
    void save(Transaction transaction);

    /**
     * Retrieve all transaction records from the database.
     */
    List<Transaction> findAll();
}
