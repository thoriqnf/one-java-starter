package com.example.repository;

import com.example.domain.Account;

public interface AccountRepository {
    Account findByAccountNumber(String accountNumber);
    void update(Account account);
}
