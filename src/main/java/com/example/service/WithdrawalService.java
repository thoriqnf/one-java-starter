package com.example.service;

import com.example.domain.Account;
import com.example.repository.AccountRepository;

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
