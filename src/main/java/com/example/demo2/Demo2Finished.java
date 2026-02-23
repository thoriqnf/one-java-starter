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
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Demo 2 Finished — Batch Transaction Processor (Unsafe vs Safe Comparison)
 *
 * This runs the same batch of transfers TWICE:
 * 1. WITHOUT locks — shows race conditions and lost money
 * 2. WITH locks — shows correct, consistent balances
 */
public class Demo2Finished {

    public static void main(String[] args) {

        String url = "jdbc:postgresql://localhost:5432/postgres";
        String user = "postgres";
        String password = "postgres";

        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            conn.setAutoCommit(false);

            List<Transaction> batch = generateBatch(20);

            // ========== ROUND 1: WITHOUT LOCKS (UNSAFE) ==========
            System.out.println("=".repeat(50));
            System.out.println("  ROUND 1: WITHOUT LOCKS (UNSAFE)");
            System.out.println("=".repeat(50));

            setupAccounts(conn);
            AccountRepository repository = new PostgresAccountRepository(conn);

            double initialTotal = repository.findByAccountNumber("A001").getBalance()
                    + repository.findByAccountNumber("A002").getBalance();
            System.out.printf("%nInitial Balances: A001=%.2f, A002=%.2f (Total=%.2f)%n",
                    repository.findByAccountNumber("A001").getBalance(),
                    repository.findByAccountNumber("A002").getBalance(),
                    initialTotal);

            System.out.printf("Processing %d transfers with 4 threads (NO LOCKS)...%n%n", batch.size());

            // Process WITHOUT locks — just raw threads
            runUnsafe(repository, batch);
            conn.commit();

            double unsafeA001 = repository.findByAccountNumber("A001").getBalance();
            double unsafeA002 = repository.findByAccountNumber("A002").getBalance();
            double unsafeTotal = unsafeA001 + unsafeA002;

            System.out.printf("Final Balances: A001=%.2f, A002=%.2f (Total=%.2f)%n", unsafeA001, unsafeA002, unsafeTotal);
            System.out.printf("Balance check: %.2f vs %.2f %s%n%n",
                    initialTotal, unsafeTotal,
                    Math.abs(initialTotal - unsafeTotal) < 0.01 ? "✓" : "✗ MONEY LOST!");

            // ========== ROUND 2: WITH LOCKS (SAFE) ==========
            System.out.println("=".repeat(50));
            System.out.println("  ROUND 2: WITH LOCKS (SAFE)");
            System.out.println("=".repeat(50));

            setupAccounts(conn);

            initialTotal = repository.findByAccountNumber("A001").getBalance()
                    + repository.findByAccountNumber("A002").getBalance();
            System.out.printf("%nInitial Balances: A001=%.2f, A002=%.2f (Total=%.2f)%n",
                    repository.findByAccountNumber("A001").getBalance(),
                    repository.findByAccountNumber("A002").getBalance(),
                    initialTotal);

            System.out.printf("Processing %d transfers with 4 threads (WITH LOCKS)...%n%n", batch.size());

            // Process WITH locks — using BatchProcessor
            AccountLocker locker = new AccountLocker();
            BatchProcessor processor = new BatchProcessor(repository, locker, 4);
            processor.processBatch(batch);
            processor.shutdown();
            conn.commit();

            double safeA001 = repository.findByAccountNumber("A001").getBalance();
            double safeA002 = repository.findByAccountNumber("A002").getBalance();
            double safeTotal = safeA001 + safeA002;

            System.out.printf("Successful: %d, Failed: %d%n", processor.getSuccessCount(), processor.getFailCount());
            System.out.printf("Final Balances: A001=%.2f, A002=%.2f (Total=%.2f)%n", safeA001, safeA002, safeTotal);
            System.out.printf("Balance check: %.2f vs %.2f %s%n",
                    initialTotal, safeTotal,
                    Math.abs(initialTotal - safeTotal) < 0.01 ? "✓ (no money lost!)" : "✗ MONEY LOST!");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Process transfers WITHOUT any locks — demonstrates race conditions.
     */
    private static void runUnsafe(AccountRepository repository, List<Transaction> batch) {
        ExecutorService executor = Executors.newFixedThreadPool(4);
        List<Future<?>> futures = new ArrayList<>();

        for (Transaction t : batch) {
            futures.add(executor.submit(() -> {
                try {
                    Account source = repository.findByAccountNumber(t.getSourceAccount());
                    Account target = repository.findByAccountNumber(t.getTargetAccount());

                    // No lock — multiple threads read/write the same account simultaneously!
                    source.debit(t.getAmount());
                    target.credit(t.getAmount());

                    repository.update(source);
                    repository.update(target);
                } catch (Exception e) {
                    // Silently ignore for demo
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

    private static List<Transaction> generateBatch(int count) {
        List<Transaction> batch = new ArrayList<>();
        Random random = new Random(42);

        for (int i = 0; i < count; i++) {
            boolean direction = random.nextBoolean();
            double amount = (random.nextInt(10) + 1) * 1000;

            batch.add(Transaction.builder()
                    .type("TRANSFER")
                    .sourceAccount(direction ? "A001" : "A002")
                    .targetAccount(direction ? "A002" : "A001")
                    .amount(amount)
                    .fee(0)
                    .createdAt(LocalDateTime.now())
                    .build());
        }

        return batch;
    }
}
