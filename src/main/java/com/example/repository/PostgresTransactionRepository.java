package com.example.repository;

import com.example.domain.Transaction;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Infrastructure Layer: PostgreSQL Implementation for Transactions
 *
 * Handles all database queries for Transaction records.
 * Uses PreparedStatement to prevent SQL Injection and try-with-resources
 * for automatic resource cleanup.
 */
public class PostgresTransactionRepository implements TransactionRepository {

    private final Connection connection;

    public PostgresTransactionRepository(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void save(Transaction transaction) {
        String sql = "INSERT INTO transaction_history (type, source_account, target_account, amount, fee, created_at) "
                + "VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, transaction.getType());
            stmt.setString(2, transaction.getSourceAccount());
            stmt.setString(3, transaction.getTargetAccount());
            stmt.setDouble(4, transaction.getAmount());
            stmt.setDouble(5, transaction.getFee());
            stmt.setTimestamp(6, Timestamp.valueOf(
                    transaction.getCreatedAt() != null ? transaction.getCreatedAt() : LocalDateTime.now()
            ));
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Transaction> findAll() {
        String sql = "SELECT id, type, source_account, target_account, amount, fee, created_at FROM transaction_history ORDER BY created_at";
        List<Transaction> transactions = new ArrayList<>();

        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Transaction t = Transaction.builder()
                        .id(rs.getInt("id"))
                        .type(rs.getString("type"))
                        .sourceAccount(rs.getString("source_account"))
                        .targetAccount(rs.getString("target_account"))
                        .amount(rs.getDouble("amount"))
                        .fee(rs.getDouble("fee"))
                        .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
                        .build();
                transactions.add(t);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return transactions;
    }
}
