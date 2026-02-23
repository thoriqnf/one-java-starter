package com.example.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Domain Layer: Transaction Entity
 *
 * Represents a single banking transaction record (transfer or withdrawal).
 * Uses Lombok annotations to generate boilerplate code:
 * - @Data: generates getters, setters, toString, equals, hashCode
 * - @AllArgsConstructor: generates a constructor with all fields
 * - @Builder: generates a fluent builder pattern for creating instances
 */
@Data
@AllArgsConstructor
@Builder
public class Transaction {

    private int id;
    private String type;            // "TRANSFER" or "WITHDRAWAL"
    private String sourceAccount;
    private String targetAccount;   // null for withdrawals
    private double amount;
    private double fee;
    private LocalDateTime createdAt;
}
