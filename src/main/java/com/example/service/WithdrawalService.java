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
        Account account = repository.findByAccountNumber(accountNumber);
        
        account.debit(amount + FLAT_FEE);
        
        repository.update(account);
    }
}
