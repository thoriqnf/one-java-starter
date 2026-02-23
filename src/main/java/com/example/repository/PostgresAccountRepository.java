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
        String sql = "SELECT account_number, balance FROM account WHERE account_number = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setString(1, accountNumber);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Account(
                            rs.getString("account_number"),
                            rs.getDouble("balance")
                    );
                }
            }

            throw new IllegalArgumentException("Account not found: " + accountNumber);

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void update(Account account) {
        String sql = "UPDATE account SET balance = ? WHERE account_number = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setDouble(1, account.getBalance());
            stmt.setString(2, account.getAccountNumber());

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
