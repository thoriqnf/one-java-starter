package com.example;

import com.example.repository.*;
import com.example.service.TransferService;
import com.example.service.WithdrawalService;

import java.sql.Connection;
import java.sql.DriverManager;

/**
 * Application Entry Point / Configurator
 * 
 * This class acts as the "Composition Root".
 * It is responsible for setting up the database connection,
 * wiring up dependencies (Dependency Injection), and managing transactions.
 */
public class Main {

    public static void main(String[] args) {

        // Database connection details
        String url = "jdbc:postgresql://localhost:5432/postgres";
        String user = "postgres";
        String password = "postgres";

        // Connect to PostgreSQL database inside a try-with-resources block so it auto-closes
        try (Connection conn = DriverManager.getConnection(url, user, password)) {

            // Disable auto-commit so we can group multiple queries together in a Transaction
            // If one fails, the whole block fails safely without saving half data
            conn.setAutoCommit(false);

            try (java.sql.Statement statement = conn.createStatement()) {
                // Initialize the table if it doesn't exist on this computer yet
                statement.execute("CREATE TABLE IF NOT EXISTS account (" +
                        "account_number VARCHAR(20) PRIMARY KEY," +
                        "balance NUMERIC(15,2) NOT NULL" +
                        ")");

                // Check if the table is empty
                java.sql.ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM account");
                if (rs.next() && rs.getInt(1) == 0) {
                    // Seed the table with some starting data for playing around
                    statement.execute("INSERT INTO account VALUES ('A001', 100000)");
                    statement.execute("INSERT INTO account VALUES ('A002', 50000)");
                }
            }
            // Commit the setup queries to the database
            conn.commit();

            // Dependency Injection: Pass the pieces into each other like Legos
            AccountRepository repository = new PostgresAccountRepository(conn); 
            TransferService transferService = new TransferService(repository);
            WithdrawalService withdrawalService = new WithdrawalService(repository);

            System.out.println("Processing transfer of 25000 from A001 to A002...");
            try {
                transferService.transfer("A001", "A002", 25000);
            } catch (Exception e) {
                System.out.println("Transfer failed: " + e.getMessage());
                // ACID: Rollback the transaction to undo any partial changes (e.g. money left A001 but didn't reach A002)
                conn.rollback();
            }

            System.out.println("Processing first withdrawal of 5000 from A001...");
            try {
                withdrawalService.withdraw("A001", 5000);
            } catch (Exception e) {
                System.out.println("Withdrawal failed: " + e.getMessage());
                // ACID: Rollback any intermediate state to keep data safe
                conn.rollback();
            }

            System.out.println("Processing second withdrawal of 10000 from A002...");
            try {
                withdrawalService.withdraw("A002", 10000);
                System.out.println("Withdrawal successful.");
            } catch (Exception e) {
                System.out.println("Withdrawal failed: " + e.getMessage());
                // ACID: Cancel the transaction safely
                conn.rollback();
            }

            conn.commit();

            System.out.println("-------------------------------------------");
            System.out.println("All operations success!");
            System.out.println("-------------------------------------------");
            System.out.println("Final Balance A001: " + repository.findByAccountNumber("A001").getBalance());
            System.out.println("Final Balance A002: " + repository.findByAccountNumber("A002").getBalance());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
