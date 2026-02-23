package com.example.service;

import com.example.domain.Account;
import com.example.repository.AccountRepository;

/**
 * Service Layer: Use Cases
 * 
 * This class orchestrates the business actions (like transferring money).
 * Notice it contains NO database SQL logic (thanks to the repository)
 * and NO pure math check logic (thanks to the Account domain object).
 * It simply coordinates the flow between them.
 */
public class TransferService {

    private final AccountRepository repository;

    public TransferService(AccountRepository repository) {
        this.repository = repository;
    }

    public void transfer(String from, String to, double amount) {
        Account source = repository.findByAccountNumber(from);
        Account target = repository.findByAccountNumber(to);

        source.debit(amount);
        target.credit(amount);

        repository.update(source);
        repository.update(target);
    }
}
