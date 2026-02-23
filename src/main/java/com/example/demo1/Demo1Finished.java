package com.example.demo1;

import com.example.domain.Transaction;
import com.example.repository.PostgresTransactionRepository;
import com.example.repository.TransactionRepository;
import com.example.service.TransactionAnalytics;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Demo 1 Finished — Transaction Analytics (All TODOs Completed)
 *
 * Run this to see the fully working output of all 4 analytics methods.
 */
public class Demo1Finished {

    public static void main(String[] args) {

        String url = "jdbc:postgresql://localhost:5432/postgres";
        String user = "postgres";
        String password = "postgres";

        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            conn.setAutoCommit(false);

            // --- Setup: Create tables and seed data ---
            setupDatabase(conn);

            // --- Dependencies ---
            TransactionRepository transactionRepo = new PostgresTransactionRepository(conn);
            TransactionAnalytics analytics = new TransactionAnalytics();

            // --- Seed sample transaction data ---
            seedTransactions(transactionRepo, conn);

            // --- Load all transactions ---
            List<Transaction> transactions = transactionRepo.findAll();

            System.out.println("===== TRANSACTION ANALYTICS (FINISHED) =====");
            System.out.println("Loaded " + transactions.size() + " transactions from database.\n");

            // --- TODO 1: Filter by Type ---
            System.out.println("--- Filter by Type: TRANSFER ---");
            List<Transaction> transfers = analytics.filterByType(transactions, "TRANSFER");
            for (Transaction t : transfers) {
                System.out.printf("  %s → %s : %.2f%n", t.getSourceAccount(), t.getTargetAccount(), t.getAmount());
            }

            // --- TODO 2: Total Amount ---
            System.out.println("\n--- Total Amount (all) ---");
            double total = analytics.totalAmount(transactions);
            System.out.printf("  Total: %.2f%n", total);

            // --- TODO 3: Total by Type ---
            System.out.println("\n--- Total by Type ---");
            Map<String, Double> byType = analytics.totalByType(transactions);
            byType.forEach((type, amount) -> System.out.printf("  %-12s: %.2f%n", type, amount));

            // --- TODO 4: Top 3 by Amount ---
            System.out.println("\n--- Top 3 by Amount ---");
            List<Transaction> top3 = analytics.topByAmount(transactions, 3);
            for (int i = 0; i < top3.size(); i++) {
                Transaction t = top3.get(i);
                String target = t.getTargetAccount() != null ? " → " + t.getTargetAccount() : "";
                System.out.printf("  %d. %-12s %.2f  (%s%s)%n", i + 1, t.getType(), t.getAmount(), t.getSourceAccount(), target);
            }

            System.out.println("\n===== ALL ANALYTICS COMPLETE =====");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void setupDatabase(Connection conn) throws Exception {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS account ("
                    + "account_number VARCHAR(50) PRIMARY KEY,"
                    + "balance DECIMAL(15, 2) NOT NULL DEFAULT 0.00"
                    + ")");

            stmt.execute("CREATE TABLE IF NOT EXISTS transaction_history ("
                    + "id SERIAL PRIMARY KEY,"
                    + "type VARCHAR(20) NOT NULL,"
                    + "source_account VARCHAR(20) NOT NULL,"
                    + "target_account VARCHAR(20),"
                    + "amount NUMERIC(15,2) NOT NULL,"
                    + "fee NUMERIC(15,2) DEFAULT 0,"
                    + "created_at TIMESTAMP DEFAULT NOW()"
                    + ")");
        }
        conn.commit();
    }

    private static void seedTransactions(TransactionRepository repo, Connection conn) throws Exception {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM transaction_history");
        }
        conn.commit();

        LocalDateTime now = LocalDateTime.now();

        repo.save(Transaction.builder()
                .type("TRANSFER").sourceAccount("A001").targetAccount("A002")
                .amount(25000).fee(0).createdAt(now.minusHours(5)).build());

        repo.save(Transaction.builder()
                .type("WITHDRAWAL").sourceAccount("A001").targetAccount(null)
                .amount(10000).fee(2500).createdAt(now.minusHours(4)).build());

        repo.save(Transaction.builder()
                .type("TRANSFER").sourceAccount("A002").targetAccount("A001")
                .amount(15000).fee(0).createdAt(now.minusHours(3)).build());

        repo.save(Transaction.builder()
                .type("WITHDRAWAL").sourceAccount("A002").targetAccount(null)
                .amount(20000).fee(2500).createdAt(now.minusHours(2)).build());

        repo.save(Transaction.builder()
                .type("TRANSFER").sourceAccount("A001").targetAccount("A002")
                .amount(8000).fee(0).createdAt(now.minusHours(1)).build());

        repo.save(Transaction.builder()
                .type("WITHDRAWAL").sourceAccount("A001").targetAccount(null)
                .amount(3500).fee(2500).createdAt(now.minusMinutes(30)).build());

        repo.save(Transaction.builder()
                .type("WITHDRAWAL").sourceAccount("A002").targetAccount(null)
                .amount(10000).fee(2500).createdAt(now.minusMinutes(15)).build());

        conn.commit();

        System.out.println("Seeded 7 sample transactions into database.\n");
    }
}
