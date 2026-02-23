package com.example.service;

import com.example.domain.Transaction;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service Layer: Transaction Analytics
 *
 * This class uses Stream API and Lambda expressions to analyze
 * transaction data. Each method corresponds to a Stream concept.
 *
 * Your task: implement each TODO method using Stream API.
 */
public class TransactionAnalytics {

    /**
     * TODO 1: Filter transactions by type
     *
     * Use: .stream(), .filter(), .collect()
     * Hint: Compare t.getType() with the given type parameter
     *
     * @param transactions the list of all transactions
     * @param type the type to filter by ("TRANSFER" or "WITHDRAWAL")
     * @return a new list containing only transactions of the given type
     */
    public List<Transaction> filterByType(List<Transaction> transactions, String type) {
        return transactions.stream()
                .filter(t -> t.getType().equals(type))
                .collect(Collectors.toList());
    }

    /**
     * TODO 2: Calculate total amount across all transactions
     *
     * Use: .stream(), .mapToDouble(), .sum()
     * Hint: Use method reference Transaction::getAmount
     *
     * @param transactions the list of transactions
     * @return the sum of all transaction amounts
     */
    public double totalAmount(List<Transaction> transactions) {
        return transactions.stream()
                .mapToDouble(Transaction::getAmount)
                .sum();
    }

    /**
     * TODO 3: Group transactions by type and sum the amounts per group
     *
     * Use: .stream(), .collect(), Collectors.groupingBy(), Collectors.summingDouble()
     * Hint: Group by Transaction::getType, downstream sum Transaction::getAmount
     *
     * @param transactions the list of transactions
     * @return a Map where key = type, value = total amount for that type
     */
    public Map<String, Double> totalByType(List<Transaction> transactions) {
        return transactions.stream()
                .collect(Collectors.groupingBy(
                        Transaction::getType,
                        Collectors.summingDouble(Transaction::getAmount)
                ));
    }

    /**
     * TODO 4: Get the top N transactions sorted by amount (descending)
     *
     * Use: .stream(), .sorted(), Comparator.comparing(), .reversed(), .limit(), .collect()
     * Hint: Sort by Transaction::getAmount in reversed order, then limit to n
     *
     * @param transactions the list of transactions
     * @param n how many top transactions to return
     * @return a list of the top N transactions by amount
     */
    public List<Transaction> topByAmount(List<Transaction> transactions, int n) {
        return transactions.stream()
                .sorted(Comparator.comparing(Transaction::getAmount).reversed())
                .limit(n)
                .collect(Collectors.toList());
    }
}
