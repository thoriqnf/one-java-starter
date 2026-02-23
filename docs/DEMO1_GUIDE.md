# Demo 1 — Transaction Analytics with Stream API & Lambda

## Objective

Add transaction history tracking to the banking app, then use **Stream API** and **Lambda expressions** to analyze transaction data from PostgreSQL.

---

## What is Stream API?

Stream API (Java 8+) lets you process collections **declaratively** — you describe *what* you want instead of writing manual loops.

```java
// ❌ Traditional loop — imperative style
List<Transaction> transfers = new ArrayList<>();
for (Transaction t : transactions) {
    if (t.getType().equals("TRANSFER")) {
        transfers.add(t);
    }
}

// ✅ Stream API — declarative style
List<Transaction> transfers = transactions.stream()
        .filter(t -> t.getType().equals("TRANSFER"))
        .collect(Collectors.toList());
```

### Key Stream Operations

| Operation | Type | What it does |
|---|---|---|
| `.stream()` | Source | Creates a stream from a List |
| `.filter(t -> ...)` | Intermediate | Keep only elements matching a condition |
| `.map(t -> ...)` | Intermediate | Transform each element |
| `.sorted(...)` | Intermediate | Sort elements |
| `.limit(n)` | Intermediate | Take first N elements |
| `.collect(...)` | Terminal | Gather results back into a List/Map |
| `.mapToDouble(...)` | Intermediate | Convert to a double stream (for math) |
| `.sum()` | Terminal | Sum all values |

---

## What are Lambda Expressions?

A lambda is a **short, inline function** — it replaces long anonymous classes with a concise arrow syntax.

```java
// ❌ Anonymous class (old way)
Comparator<Transaction> byAmount = new Comparator<Transaction>() {
    @Override
    public int compare(Transaction a, Transaction b) {
        return Double.compare(a.getAmount(), b.getAmount());
    }
};

// ✅ Lambda (new way)
Comparator<Transaction> byAmount = (a, b) -> Double.compare(a.getAmount(), b.getAmount());

// ✅ Method reference (even shorter — when lambda just calls one method)
Comparator<Transaction> byAmount = Comparator.comparing(Transaction::getAmount);
```

### Common Functional Interfaces

| Interface | Signature | Use Case |
|---|---|---|
| `Predicate<T>` | `T → boolean` | Filtering: `t -> t.getAmount() > 1000` |
| `Function<T, R>` | `T → R` | Transforming: `Transaction::getAmount` |
| `Comparator<T>` | `(T, T) → int` | Sorting: `Comparator.comparing(...)` |
| `Consumer<T>` | `T → void` | Side effects: `System.out::println` |

---

## What You'll Build

A `TransactionAnalytics` service that answers questions like:

- What's the total amount transferred today?
- Which type has the most money flowing?
- What are the top 3 largest transactions?

---

## How to Demo

### Prerequisites

- **Java 17** installed
- **Maven** installed
- **PostgreSQL** running on `localhost:5432` with user `postgres` / password `postgres`

### Step 1: Compile the Project

```bash
mvn clean compile
```

This downloads dependencies (including Lombok) and compiles all Java files. Expect a `BUILD SUCCESS` message.

### Step 2: Run the Starter (Before Implementing)

```bash
mvn exec:java -Dexec.mainClass="com.example.demo1.Demo1Starter"
```

This will:
- Create the `account` and `transaction_history` tables automatically
- Seed 7 sample transactions into the database
- Call all 4 analytics methods — but they return empty results (`not implemented yet`)

**Show this output to participants** — this is the "before" state they'll fix.

### Step 3: Implement the TODOs

Participants fill in the 4 TODO methods in `service/TransactionAnalytics.java` (see steps below).

### Step 4: Recompile and Run Again

```bash
mvn clean compile
mvn exec:java -Dexec.mainClass="com.example.demo1.Demo1Starter"
```

Now the output shows real analytics results. **Each time they implement a TODO**, they can recompile and see the new section fill in.

### Step 5: Run the Finished Version (Optional)

To show the fully completed output at any time:

```bash
mvn exec:java -Dexec.mainClass="com.example.demo1.Demo1Finished"
```

---

## Database Table

A new `transaction_history` table is created automatically when you run the app:

```sql
CREATE TABLE IF NOT EXISTS transaction_history (
    id SERIAL PRIMARY KEY,
    type VARCHAR(20) NOT NULL,           -- 'TRANSFER' or 'WITHDRAWAL'
    source_account VARCHAR(20) NOT NULL,
    target_account VARCHAR(20),           -- NULL for withdrawals
    amount NUMERIC(15,2) NOT NULL,
    fee NUMERIC(15,2) DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW()
);
```

---

## Step-by-Step Implementation

### Step 1: Understand the Transaction Model

Open `src/main/java/com/example/domain/Transaction.java`. This uses Lombok for boilerplate:

```java
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
```

### Step 2: Implement the TODOs

Open **`src/main/java/com/example/service/TransactionAnalytics.java`** — this is the file you will edit. Each TODO method needs a Stream API implementation:

#### TODO 1 — Filter by type

**File:** `service/TransactionAnalytics.java` → method `filterByType()`

```java
public List<Transaction> filterByType(List<Transaction> transactions, String type) {
    return transactions.stream()
            .filter(t -> t.getType().equals(type))
            .collect(Collectors.toList());
}
```

**Stream concept:** `.stream()` → `.filter(predicate)` → `.collect()` — filters the list, keeping only elements where the lambda returns `true`.

#### TODO 2 — Calculate total amount

**File:** `service/TransactionAnalytics.java` → method `totalAmount()`

```java
public double totalAmount(List<Transaction> transactions) {
    return transactions.stream()
            .mapToDouble(Transaction::getAmount)
            .sum();
}
```

**Stream concept:** `.mapToDouble(methodRef)` converts each Transaction to its amount (a double), then `.sum()` adds them all up. `Transaction::getAmount` is a **method reference** — shorthand for `t -> t.getAmount()`.

#### TODO 3 — Group by type with totals

**File:** `service/TransactionAnalytics.java` → method `totalByType()`

```java
public Map<String, Double> totalByType(List<Transaction> transactions) {
    return transactions.stream()
            .collect(Collectors.groupingBy(
                    Transaction::getType,
                    Collectors.summingDouble(Transaction::getAmount)
            ));
}
```

**Stream concept:** `Collectors.groupingBy()` splits the stream into groups by type. The second argument `Collectors.summingDouble()` tells it to sum the amounts within each group. Result: `{"TRANSFER": 48000, "WITHDRAWAL": 43500}`.

#### TODO 4 — Find top N by amount

**File:** `service/TransactionAnalytics.java` → method `topByAmount()`

```java
public List<Transaction> topByAmount(List<Transaction> transactions, int n) {
    return transactions.stream()
            .sorted(Comparator.comparing(Transaction::getAmount).reversed())
            .limit(n)
            .collect(Collectors.toList());
}
```

**Stream concept:** `.sorted()` sorts elements using a `Comparator`. `Comparator.comparing(Transaction::getAmount).reversed()` sorts by amount **descending**. `.limit(n)` takes only the first N results.

---

## Running the Finished App

After all 4 TODOs are implemented, compile and run:

```bash
mvn clean compile
mvn exec:java -Dexec.mainClass="com.example.demo1.Demo1Starter"
```

### Expected Output

```
===== TRANSACTION ANALYTICS =====

--- Filter by Type: TRANSFER ---
  A001 → A002 : 25000.00
  A002 → A001 : 15000.00
  A001 → A002 : 8000.00

--- Total Amount (all) ---
  Total: 91500.00

--- Total by Type ---
  TRANSFER    : 48000.00
  WITHDRAWAL  : 43500.00

--- Top 3 by Amount ---
  1. TRANSFER    25000.00  (A001 → A002)
  2. WITHDRAWAL  20000.00  (A002)
  3. TRANSFER    15000.00  (A002 → A001)
```

---

## Key Concepts Recap

| Concept | Where Used |
|---|---|
| `stream().filter()` | TODO 1 — Filter by transaction type |
| `mapToDouble().sum()` | TODO 2 — Calculate total amount |
| `Collectors.groupingBy()` | TODO 3 — Group by type with totals |
| `Comparator.comparing()` | TODO 4 — Sort by amount descending |
| Method references | `Transaction::getAmount`, `Transaction::getType` |
| Lambda expressions | `t -> t.getType().equals(type)` in filter |

---

## Bonus Reference

### Collectors — More Examples

```java
// Group transactions into lists by type
Map<String, List<Transaction>> byType = transactions.stream()
        .collect(Collectors.groupingBy(Transaction::getType));

// Get statistics (count, sum, min, max, average)
DoubleSummaryStatistics stats = transactions.stream()
        .collect(Collectors.summarizingDouble(Transaction::getAmount));

System.out.println("Average: " + stats.getAverage());
System.out.println("Max: " + stats.getMax());
```

### Method References

| Type | Syntax | Equivalent Lambda |
|---|---|---|
| Static method | `Math::abs` | `x -> Math.abs(x)` |
| Instance method | `Transaction::getAmount` | `t -> t.getAmount()` |
| Constructor | `ArrayList::new` | `() -> new ArrayList<>()` |

### Composing Predicates

```java
Predicate<Transaction> isTransfer = t -> t.getType().equals("TRANSFER");
Predicate<Transaction> isLarge = t -> t.getAmount() > 10000;

// Combine with AND / OR / negate
Predicate<Transaction> largeTransfers = isTransfer.and(isLarge);
Predicate<Transaction> transferOrLarge = isTransfer.or(isLarge);
Predicate<Transaction> notTransfer = isTransfer.negate();

List<Transaction> results = transactions.stream()
        .filter(largeTransfers)
        .collect(Collectors.toList());
```
