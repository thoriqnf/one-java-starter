# Data Processing & Multithreading in Java

A comprehensive guide covering Collection & Stream API, Functional Programming, and Concurrency — applied to a banking domain.

---

## 1. Collection & Stream API

### What is Stream API?

Stream API (introduced in Java 8) lets you process collections of data **declaratively** — describing *what* you want instead of *how* to loop through it.

```java
// Traditional loop — imperative style
List<Transaction> transfers = new ArrayList<>();
for (Transaction t : transactions) {
    if (t.getType().equals("TRANSFER")) {
        transfers.add(t);
    }
}

// Stream API — declarative style
List<Transaction> transfers = transactions.stream()
        .filter(t -> t.getType().equals("TRANSFER"))
        .collect(Collectors.toList());
```

### Core Stream Operations

| Operation | Type | Description | Example |
|---|---|---|---|
| `.stream()` | Source | Creates a stream from a collection | `list.stream()` |
| `.filter()` | Intermediate | Keep elements matching a condition | `.filter(t -> t.getAmount() > 1000)` |
| `.map()` | Intermediate | Transform each element | `.map(Transaction::getAmount)` |
| `.sorted()` | Intermediate | Sort elements | `.sorted(Comparator.comparing(Transaction::getAmount))` |
| `.distinct()` | Intermediate | Remove duplicates | `.distinct()` |
| `.limit()` | Intermediate | Take first N elements | `.limit(5)` |
| `.collect()` | Terminal | Gather results into a collection | `.collect(Collectors.toList())` |
| `.reduce()` | Terminal | Combine elements into a single result | `.reduce(0.0, Double::sum)` |
| `.forEach()` | Terminal | Perform action on each element | `.forEach(System.out::println)` |
| `.count()` | Terminal | Count elements | `.count()` |

### Collectors — Grouping & Aggregation

```java
// Group transactions by type
Map<String, List<Transaction>> byType = transactions.stream()
        .collect(Collectors.groupingBy(Transaction::getType));

// Sum amounts per type
Map<String, Double> totalByType = transactions.stream()
        .collect(Collectors.groupingBy(
                Transaction::getType,
                Collectors.summingDouble(Transaction::getAmount)
        ));

// Get statistics (count, sum, min, max, average)
DoubleSummaryStatistics stats = transactions.stream()
        .collect(Collectors.summarizingDouble(Transaction::getAmount));

System.out.println("Average: " + stats.getAverage());
System.out.println("Max: " + stats.getMax());
System.out.println("Total: " + stats.getSum());
```

### Optional — Safe Value Handling

```java
// Find the largest transaction — may not exist if list is empty
Optional<Transaction> largest = transactions.stream()
        .max(Comparator.comparing(Transaction::getAmount));

// Safe access — no NullPointerException
largest.ifPresent(t -> System.out.println("Largest: " + t.getAmount()));

// Or provide a default
double maxAmount = largest.map(Transaction::getAmount).orElse(0.0);
```

---

## 2. Functional Programming & Lambda Expressions

### What is a Lambda?

A lambda is a **short, inline function** without a name. It replaces verbose anonymous classes.

```java
// Anonymous class (old way)
Comparator<Transaction> byAmount = new Comparator<Transaction>() {
    @Override
    public int compare(Transaction a, Transaction b) {
        return Double.compare(a.getAmount(), b.getAmount());
    }
};

// Lambda (modern way)
Comparator<Transaction> byAmount = (a, b) -> Double.compare(a.getAmount(), b.getAmount());

// Method reference (even shorter)
Comparator<Transaction> byAmount = Comparator.comparing(Transaction::getAmount);
```

### Key Functional Interfaces

Java provides built-in functional interfaces in `java.util.function`:

| Interface | Signature | Use Case | Example |
|---|---|---|---|
| `Predicate<T>` | `T → boolean` | Filtering | `t -> t.getAmount() > 1000` |
| `Function<T, R>` | `T → R` | Transforming | `Transaction::getAmount` |
| `Consumer<T>` | `T → void` | Side effects | `System.out::println` |
| `Supplier<T>` | `() → T` | Lazy creation | `() -> new Transaction(...)` |
| `Comparator<T>` | `(T, T) → int` | Sorting | `Comparator.comparing(Transaction::getAmount)` |

### Composing Predicates

```java
Predicate<Transaction> isTransfer = t -> t.getType().equals("TRANSFER");
Predicate<Transaction> isLargeAmount = t -> t.getAmount() > 10000;

// Combine with AND
Predicate<Transaction> largeTransfers = isTransfer.and(isLargeAmount);

// Combine with OR
Predicate<Transaction> transferOrLarge = isTransfer.or(isLargeAmount);

// Negate
Predicate<Transaction> notTransfer = isTransfer.negate();

// Use in stream
List<Transaction> results = transactions.stream()
        .filter(largeTransfers)
        .collect(Collectors.toList());
```

### Method References

| Type | Syntax | Equivalent Lambda |
|---|---|---|
| Static method | `Math::abs` | `x -> Math.abs(x)` |
| Instance method | `Transaction::getAmount` | `t -> t.getAmount()` |
| Constructor | `ArrayList::new` | `() -> new ArrayList<>()` |

---

## 3. Concurrency & Thread-Safe Programming

### Why Concurrency?

A bank processes thousands of transactions simultaneously. Without concurrency, each transaction waits for the previous one — too slow. But with concurrency, multiple threads access shared data (account balances), which can cause **race conditions**.

### Race Condition Example

```java
// Two threads withdraw from the same account simultaneously
// Thread A reads balance: 10000
// Thread B reads balance: 10000
// Thread A subtracts 3000 → writes 7000
// Thread B subtracts 5000 → writes 5000  ← Thread A's withdrawal is LOST!
```

### ExecutorService — Thread Pool Management

Instead of manually creating threads, use `ExecutorService` to manage a pool of reusable threads:

```java
// Create a pool of 4 threads
ExecutorService executor = Executors.newFixedThreadPool(4);

// Submit tasks
List<Future<Boolean>> futures = new ArrayList<>();
for (Transaction t : batch) {
    Future<Boolean> future = executor.submit(() -> {
        processTransfer(t);
        return true;
    });
    futures.add(future);
}

// Wait for all tasks to complete
executor.shutdown();
executor.awaitTermination(30, TimeUnit.SECONDS);

// Check results
for (Future<Boolean> f : futures) {
    boolean success = f.get(); // blocks until result is ready
}
```

### ReentrantLock — Thread-Safe Operations

```java
private final ReentrantLock lock = new ReentrantLock();

public void safeDebit(double amount) {
    lock.lock();       // Only one thread can enter at a time
    try {
        if (balance < amount) {
            throw new IllegalStateException("Insufficient balance");
        }
        balance -= amount;
    } finally {
        lock.unlock();  // ALWAYS unlock in finally block
    }
}
```

### Atomic Operations — Lock-Free Counters

For simple counters (success/fail counts), use atomic classes instead of locks:

```java
private final AtomicInteger successCount = new AtomicInteger(0);
private final AtomicInteger failCount = new AtomicInteger(0);

// Thread-safe increment — no lock needed
successCount.incrementAndGet();
failCount.incrementAndGet();

System.out.println("Success: " + successCount.get());
System.out.println("Failed: " + failCount.get());
```

### synchronized vs ReentrantLock

| Feature | `synchronized` | `ReentrantLock` |
|---|---|---|
| Simplicity | ✅ Simple keyword | ❌ Manual lock/unlock |
| Try-lock | ❌ No | ✅ `tryLock()` with timeout |
| Fair ordering | ❌ No guarantee | ✅ `new ReentrantLock(true)` |
| Multiple conditions | ❌ One wait set | ✅ Multiple `Condition` objects |
| Use in banking | Simple cases | Complex transaction locking |

---

## 4. Practice: Parallel Data Processing

### Putting It All Together

In a real banking system, you combine Stream API with Concurrency:

1. **Stream API** to analyze and prepare data (filter, group, aggregate)
2. **ExecutorService** to process batches in parallel
3. **ReentrantLock** to protect shared resources (account balances)
4. **AtomicInteger** to track statistics safely

### Architecture Flow

```
┌─────────────────────────────────────────────────────────┐
│ 1. Load Transactions from PostgreSQL                    │
│    └── TransactionRepository.findAll()                  │
├─────────────────────────────────────────────────────────┤
│ 2. Analyze with Stream API (Demo 1)                     │
│    └── filter → group → aggregate → report              │
├─────────────────────────────────────────────────────────┤
│ 3. Process in Parallel (Demo 2)                         │
│    ├── ExecutorService (thread pool)                     │
│    ├── ReentrantLock (protect account balance)           │
│    └── AtomicInteger (track success/fail)                │
├─────────────────────────────────────────────────────────┤
│ 4. Results                                              │
│    └── Safe, consistent balances + performance stats    │
└─────────────────────────────────────────────────────────┘
```

### Key Takeaways

| Concept | When to Use |
|---|---|
| Stream API | Processing/analyzing collections of data |
| Lambda | Short inline functions for predicates, mappers, comparators |
| ExecutorService | Running multiple tasks in parallel |
| ReentrantLock | Protecting shared mutable state (e.g. account balance) |
| AtomicInteger | Thread-safe counters without locking overhead |
| Future | Getting results from async tasks |
