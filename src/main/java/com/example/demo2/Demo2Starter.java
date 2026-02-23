package com.example.demo2;

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

/**
 * Demo 2 Starter — Batch Transaction Processor
 *
 * This demo teaches Concurrency and Thread-Safe programming by processing
 * a batch of 20 bank transfers in parallel.
 *
 * Run this class, then implement the TODOs in AccountLocker.java and BatchProcessor.java.
 */
public class Demo2Starter {

    public static void main(String[] args) {

        String url = "jdbc:postgresql://localhost:5432/postgres";
        String user = "postgres";
        String password = "postgres";

        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            conn.setAutoCommit(false);

            // --- Setup: Reset account balances ---
            setupAccounts(conn);

            // --- Dependencies ---
            AccountRepository repository = new PostgresAccountRepository(conn);
            AccountLocker locker = new AccountLocker();
            int threadCount = 4;
            BatchProcessor processor = new BatchProcessor(repository, locker, threadCount);

            // --- Generate a batch of random transfers ---
            List<Transaction> batch = generateBatch(20);

            System.out.println("===== BATCH TRANSACTION PROCESSOR =====\n");

            // Show initial balances
            System.out.println("Initial Balances:");
            System.out.printf("  A001: %.2f%n", repository.findByAccountNumber("A001").getBalance());
            System.out.printf("  A002: %.2f%n", repository.findByAccountNumber("A002").getBalance());
            double initialTotal = repository.findByAccountNumber("A001").getBalance()
                    + repository.findByAccountNumber("A002").getBalance();

            System.out.printf("%nProcessing %d transfers with %d threads...%n%n", batch.size(), threadCount);

            // --- Process the batch ---
            long startTime = System.currentTimeMillis();
            processor.processBatch(batch);
            processor.shutdown();
            conn.commit();
            long elapsed = System.currentTimeMillis() - startTime;

            // --- Results ---
            System.out.println("\n===== RESULTS =====");
            System.out.printf("Successful : %d%n", processor.getSuccessCount());
            System.out.printf("Failed     : %d%n", processor.getFailCount());
            System.out.printf("Time taken : %dms%n", elapsed);

            System.out.println("\nFinal Balances:");
            double finalA001 = repository.findByAccountNumber("A001").getBalance();
            double finalA002 = repository.findByAccountNumber("A002").getBalance();
            System.out.printf("  A001: %.2f%n", finalA001);
            System.out.printf("  A002: %.2f%n", finalA002);

            double finalTotal = finalA001 + finalA002;
            System.out.printf("%nBalance check: %.2f = %.2f %s%n",
                    initialTotal, finalTotal,
                    Math.abs(initialTotal - finalTotal) < 0.01 ? "✓ (no money lost!)" : "✗ MONEY LOST!");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Reset account balances for a clean demo.
     */
    private static void setupAccounts(Connection conn) throws Exception {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS account ("
                    + "account_number VARCHAR(50) PRIMARY KEY,"
                    + "balance DECIMAL(15, 2) NOT NULL DEFAULT 0.00"
                    + ")");

            // Reset balances
            stmt.execute("DELETE FROM account");
            stmt.execute("INSERT INTO account VALUES ('A001', 100000)");
            stmt.execute("INSERT INTO account VALUES ('A002', 50000)");
        }
        conn.commit();
    }

    /**
     * Generate a batch of random transfers between A001 and A002.
     */
    private static List<Transaction> generateBatch(int count) {
        List<Transaction> batch = new ArrayList<>();
        Random random = new Random(42); // fixed seed for reproducible results

        for (int i = 0; i < count; i++) {
            boolean direction = random.nextBoolean(); // true = A001→A002, false = A002→A001
            double amount = (random.nextInt(10) + 1) * 1000; // 1000 to 10000

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
