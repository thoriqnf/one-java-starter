package com.example.service;

import com.example.domain.Account;
import com.example.domain.Transaction;
import com.example.repository.AccountRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service Layer: Batch Transaction Processor
 *
 * Processes a batch of transfers concurrently using a thread pool.
 * Uses AccountLocker for thread-safe balance updates and
 * AtomicInteger for thread-safe success/fail counters.
 *
 * Your task: implement the TODO methods for parallel processing.
 */
public class BatchProcessor {

    private final AccountRepository repository;
    private final AccountLocker locker;
    private final ExecutorService executor;
    private final AtomicInteger successCount = new AtomicInteger(0);
    private final AtomicInteger failCount = new AtomicInteger(0);

    public BatchProcessor(AccountRepository repository, AccountLocker locker, int threadCount) {
        this.repository = repository;
        this.locker = locker;
        this.executor = Executors.newFixedThreadPool(threadCount);
    }

    /**
     * TODO 2: Process a batch of transfers in parallel using ExecutorService
     *
     * Use: executor.submit(), Future, f.get()
     * Hint:
     *   1. Loop through the batch, submit each as a Callable using executor.submit(() -> processSingleTransfer(t))
     *   2. Collect all Future<Boolean> results into a list
     *   3. Loop through futures and call f.get() to wait for each to complete
     *
     * @param batch the list of transactions to process in parallel
     */
    public void processBatch(List<Transaction> batch) {
        List<Future<Boolean>> futures = new ArrayList<>();

        for (Transaction t : batch) {
            Future<Boolean> future = executor.submit(() -> processSingleTransfer(t));
            futures.add(future);
        }

        for (Future<Boolean> f : futures) {
            try {
                f.get();
            } catch (Exception e) {
                System.out.println("Task error: " + e.getMessage());
            }
        }
    }

    /**
     * TODO 3: Process a single transfer with thread-safe locking
     *
     * Use: locker.lockAccounts(), try/finally, locker.unlockAccounts()
     * Hint:
     *   1. Lock both accounts using locker.lockAccounts()
     *   2. In a try block: load accounts, debit/credit, update, increment successCount
     *   3. In catch block: increment failCount, print error
     *   4. In finally block: ALWAYS unlock both accounts
     *   5. Print the thread name and transfer details:
     *      System.out.printf("[%s] %s → %s : %.2f ✓%n", Thread.currentThread().getName(), ...)
     *
     * @param t the transaction to process
     * @return true if successful, false if failed
     */
    private boolean processSingleTransfer(Transaction t) {
        String thread = Thread.currentThread().getName();
        locker.lockAccounts(t.getSourceAccount(), t.getTargetAccount());
        try {
            Account source = repository.findByAccountNumber(t.getSourceAccount());
            Account target = repository.findByAccountNumber(t.getTargetAccount());

            source.debit(t.getAmount());
            target.credit(t.getAmount());

            repository.update(source);
            repository.update(target);

            successCount.incrementAndGet();
            System.out.printf("  [%s] %s → %s : %.2f ✓%n",
                    thread, t.getSourceAccount(), t.getTargetAccount(), t.getAmount());
            return true;
        } catch (Exception e) {
            failCount.incrementAndGet();
            System.out.printf("  [%s] %s → %s : %.2f ✗ (%s)%n",
                    thread, t.getSourceAccount(), t.getTargetAccount(), t.getAmount(), e.getMessage());
            return false;
        } finally {
            locker.unlockAccounts(t.getSourceAccount(), t.getTargetAccount());
        }
    }

    /**
     * Shutdown the executor and wait for all tasks to finish.
     */
    public void shutdown() throws InterruptedException {
        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);
    }

    public int getSuccessCount() {
        return successCount.get();
    }

    public int getFailCount() {
        return failCount.get();
    }
}
