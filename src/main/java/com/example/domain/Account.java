package com.example.domain;

/**
 * Domain Layer: Account Entity
 * 
 * SOLID Principle: SRP (Single Responsibility Principle)
 * This class is solely responsible for modeling the core business rules of an
 * Account (balance validation, debit/credit math). It has no dependency on
 * databases, frameworks, or external services.
 */
public class Account {

    private final String accountNumber;
    private double balance;

    public Account(String accountNumber, double balance) {
        this.accountNumber = accountNumber;
        this.balance = balance;
    }

    public void debit(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Invalid amount");
        }
        if (balance < amount) {
            throw new IllegalStateException("Insufficient balance");
        }
        balance -= amount;
    }

    public void credit(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Invalid amount");
        }
        balance += amount;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public double getBalance() {
        return balance;
    }
}
