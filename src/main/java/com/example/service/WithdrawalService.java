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
