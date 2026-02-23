package com.example.service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Service Layer: Account Locker
 *
 * Provides per-account locking to prevent race conditions
 * when multiple threads access the same account simultaneously.
 *
 * Your task: implement the TODO methods to enable thread-safe account access.
 */
public class AccountLocker {

    /**
     * TODO 1: Implement thread-safe lock management
     *
     * You need to:
     * 1. Create a ConcurrentHashMap<String, ReentrantLock> to store one lock per account
     * 2. getLock(): return the lock for an account (create one if it doesn't exist)
     *    Hint: use ConcurrentHashMap.computeIfAbsent()
     * 3. lockAccounts(): lock both accounts in alphabetical order (to prevent deadlocks)
     *    Hint: compare account1 and account2, lock the smaller one first
     * 4. unlockAccounts(): unlock both accounts
     */

    private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    public ReentrantLock getLock(String accountNumber) {
        return locks.computeIfAbsent(accountNumber, k -> new ReentrantLock());
    }

    public void lockAccounts(String account1, String account2) {
        // Always lock in alphabetical order to prevent deadlocks
        String first = account1.compareTo(account2) < 0 ? account1 : account2;
        String second = account1.compareTo(account2) < 0 ? account2 : account1;
        getLock(first).lock();
        getLock(second).lock();
    }

    public void unlockAccounts(String account1, String account2) {
        getLock(account1).unlock();
        getLock(account2).unlock();
    }
}
