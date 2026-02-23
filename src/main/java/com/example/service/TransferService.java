package com.example.service;

import com.example.domain.Account;
import com.example.repository.AccountRepository;

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
