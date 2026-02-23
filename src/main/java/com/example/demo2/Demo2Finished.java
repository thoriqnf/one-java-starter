package com.example.demo2;

import com.example.domain.Account;
import com.example.domain.Transaction;
import com.example.repository.AccountRepository;
import com.example.repository.PostgresAccountRepository;
import com.example.service.AccountLocker;
import com.example.service.BatchProcessor;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Demo 2 Finished — Batch Transaction Processor (Unsafe vs Safe Comparison)
 *
 * This runs the same batch of 6 transfers TWICE:
 * 1. WITHOUT locks — shows race conditions with READ/WRITE logging
 * 2. WITH locks — shows correct, consistent balances with LOCK/UNLOCK logging
 */
public class Demo2Finished {

    public static void main(String[] args) {

        String url = "jdbc:postgresql://localhost:5432/postgres";
        String user = "postgres";
        String password = "postgres";

        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            conn.setAutoCommit(false);

            List<Transaction> batch = createBatch();

            // Show the batch first
            System.out.println("===== BATCH TRANSACTION PROCESSOR =====\n");
            System.out.println("Transfers to process:");
            for (int i = 0; i < batch.size(); i++) {
                Transaction t = batch.get(i);
                System.out.printf("  %d. %s → %s : %.2f%n", i + 1,
                        t.getSourceAccount(), t.getTargetAccount(), t.getAmount());
            }

            // ========== ROUND 1: WITHOUT LOCKS (UNSAFE) ==========
            System.out.println("\n" + "=".repeat(50));
            System.out.println("  ROUND 1: WITHOUT LOCKS (UNSAFE)");
            System.out.println("=".repeat(50));

            setupAccounts(conn);
            AccountRepository repository = new PostgresAccountRepository(conn);

            double initialTotal = repository.findByAccountNumber("A001").getBalance()
                    + repository.findByAccountNumber("A002").getBalance();
            System.out.printf("%nInitial: A001=%.2f, A002=%.2f (Total=%.2f)%n%n",
                    repository.findByAccountNumber("A001").getBalance(),
                    repository.findByAccountNumber("A002").getBalance(),
                    initialTotal);

            // Process WITHOUT locks — shows race condition with detailed logging
            runUnsafe(repository, batch);
            conn.commit();

            double unsafeA001 = repository.findByAccountNumber("A001").getBalance();
            double unsafeA002 = repository.findByAccountNumber("A002").getBalance();
            double unsafeTotal = unsafeA001 + unsafeA002;

            System.out.printf("%nFinal: A001=%.2f, A002=%.2f (Total=%.2f)%n", unsafeA001, unsafeA002, unsafeTotal);
            System.out.printf("Expected total: %.2f, Actual total: %.2f → %s%n",
                    initialTotal, unsafeTotal,
                    Math.abs(initialTotal - unsafeTotal) < 0.01 ? "✓ OK" : "✗ MONEY LOST! (race condition)");

            // ========== ROUND 2: WITH LOCKS (SAFE) ==========
            System.out.println("\n" + "=".repeat(50));
            System.out.println("  ROUND 2: WITH LOCKS (SAFE)");
            System.out.println("=".repeat(50));

            setupAccounts(conn);

            initialTotal = repository.findByAccountNumber("A001").getBalance()
                    + repository.findByAccountNumber("A002").getBalance();
            System.out.printf("%nInitial: A001=%.2f, A002=%.2f (Total=%.2f)%n%n",
                    repository.findByAccountNumber("A001").getBalance(),
                    repository.findByAccountNumber("A002").getBalance(),
                    initialTotal);

            // Process WITH locks — using BatchProcessor (safe)
            AccountLocker locker = new AccountLocker();
            BatchProcessor processor = new BatchProcessor(repository, locker, 4);
            processor.processBatch(batch);
            processor.shutdown();
            conn.commit();

            double safeA001 = repository.findByAccountNumber("A001").getBalance();
            double safeA002 = repository.findByAccountNumber("A002").getBalance();
            double safeTotal = safeA001 + safeA002;

            System.out.printf("%nSuccessful: %d, Failed: %d%n", processor.getSuccessCount(), processor.getFailCount());
            System.out.printf("Final: A001=%.2f, A002=%.2f (Total=%.2f)%n", safeA001, safeA002, safeTotal);
            System.out.printf("Expected total: %.2f, Actual total: %.2f → %s%n",
                    initialTotal, safeTotal,
                    Math.abs(initialTotal - safeTotal) < 0.01 ? "✓ NO MONEY LOST!" : "✗ MONEY LOST!");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Process transfers WITHOUT any locks — demonstrates race conditions.
     * Adds Thread.sleep() to make race conditions more likely,
     * and logs each READ/WRITE to show the problem visually.
     */
    private static void runUnsafe(AccountRepository repository, List<Transaction> batch) {
        ExecutorService executor = Executors.newFixedThreadPool(4);
        List<Future<?>> futures = new ArrayList<>();

        for (Transaction t : batch) {
            futures.add(executor.submit(() -> {
                String thread = Thread.currentThread().getName();
                try {
                    // READ — multiple threads can read the SAME stale value
                    Account source = repository.findByAccountNumber(t.getSourceAccount());
                    Account target = repository.findByAccountNumber(t.getTargetAccount());
                    System.out.printf("  [%s] READ  %s=%.2f, %s=%.2f%n",
                            thread, t.getSourceAccount(), source.getBalance(),
                            t.getTargetAccount(), target.getBalance());

                    // Simulate processing delay — makes race conditions more likely
                    Thread.sleep(50);

                    source.debit(t.getAmount());
                    target.credit(t.getAmount());

                    // WRITE — stale data overwrites other thread's changes
                    repository.update(source);
                    repository.update(target);
                    System.out.printf("  [%s] WRITE %s=%.2f, %s=%.2f  (%s → %s : %.2f)%n",
                            thread, t.getSourceAccount(), source.getBalance(),
                            t.getTargetAccount(), target.getBalance(),
                            t.getSourceAccount(), t.getTargetAccount(), t.getAmount());

                } catch (Exception e) {
                    System.out.printf("  [%s] ERROR %s → %s : %s%n",
                            thread, t.getSourceAccount(), t.getTargetAccount(), e.getMessage());
                }
            }));
        }

        for (Future<?> f : futures) {
            try { f.get(); } catch (Exception ignored) {}
        }

        executor.shutdown();
    }

    private static void setupAccounts(Connection conn) throws Exception {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS account ("
                    + "account_number VARCHAR(50) PRIMARY KEY,"
                    + "balance DECIMAL(15, 2) NOT NULL DEFAULT 0.00"
                    + ")");
            stmt.execute("DELETE FROM account");
            stmt.execute("INSERT INTO account VALUES ('A001', 100000)");
            stmt.execute("INSERT INTO account VALUES ('A002', 50000)");
        }
        conn.commit();
    }

    /**
     * Same fixed batch of 6 transfers as Demo2Starter.
     */
    private static List<Transaction> createBatch() {
        List<Transaction> batch = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        batch.add(Transaction.builder().type("TRANSFER").sourceAccount("A001").targetAccount("A002")
                .amount(5000).fee(0).createdAt(now).build());
        batch.add(Transaction.builder().type("TRANSFER").sourceAccount("A002").targetAccount("A001")
                .amount(3000).fee(0).createdAt(now).build());
        batch.add(Transaction.builder().type("TRANSFER").sourceAccount("A001").targetAccount("A002")
                .amount(8000).fee(0).createdAt(now).build());
        batch.add(Transaction.builder().type("TRANSFER").sourceAccount("A002").targetAccount("A001")
                .amount(2000).fee(0).createdAt(now).build());
        batch.add(Transaction.builder().type("TRANSFER").sourceAccount("A001").targetAccount("A002")
                .amount(10000).fee(0).createdAt(now).build());
        batch.add(Transaction.builder().type("TRANSFER").sourceAccount("A002").targetAccount("A001")
                .amount(7000).fee(0).createdAt(now).build());

        return batch;
    }
}
