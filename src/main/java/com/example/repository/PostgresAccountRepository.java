package com.example.repository;

import com.example.domain.Account;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Infrastructure Layer: PostgreSQL Implementation
 * 
 * This class handles all actual database queries for Accounts.
 * 
 * Security feature: Uses PreparedStatement for all queries to prevent SQL Injection attacks.
 * It also uses try-with-resources to ensure database connections and statements are automatically closed to prevent memory leaks.
 */
public class PostgresAccountRepository implements AccountRepository {

    private final Connection connection;

    public PostgresAccountRepository(Connection connection) {
        this.connection = connection;
    }

    @Override
    public Account findByAccountNumber(String accountNumber) {
        // The '?' is a placeholder to prevent SQL Injection hackers from manipulating our query
        String sql = "SELECT account_number, balance FROM account WHERE account_number = ?";
        
        // Try-with-resources automatically closes the statement when we are done using it
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {

            // Replace the 1st '?' with the actual accountNumber variable
            stmt.setString(1, accountNumber);

            // Execute the query and get the Result set back from PostgreSQL
            try (ResultSet rs = stmt.executeQuery()) {
                // rs.next() checks if there is at least one result
                if (rs.next()) {
                    // Create and return a new Account Domain Object from the database row
                    return new Account(
                            rs.getString("account_number"),
                            rs.getDouble("balance")
                    );
                }
            }

            // If we reach here, it means rs.next() was false (no account was found)
            throw new IllegalArgumentException("Account not found: " + accountNumber);

        } catch (SQLException e) {
            // Convert checked SQL Exceptions into unchecked Runtime Exceptions for simpler error handling
            throw new RuntimeException(e);
        }
    }

    @Override
    public void update(Account account) {
        String sql = "UPDATE account SET balance = ? WHERE account_number = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {

            // '1' and '2' refer to the first and second '?' placeholders respectively
            stmt.setDouble(1, account.getBalance()); 
            stmt.setString(2, account.getAccountNumber());

            // Use executeUpdate() instead of executeQuery() because we are modifying data, not reading it
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
