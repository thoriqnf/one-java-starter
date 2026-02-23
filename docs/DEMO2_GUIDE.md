# Demo 2 — Batch Transaction Processor with Concurrency

## Objective

Build a batch transaction processor that processes multiple bank transfers **concurrently** using thread pools, while keeping account balances **thread-safe** with locks and atomic operations.

---

## What is Concurrency?

Concurrency means running multiple tasks **at the same time**. A bank processes thousands of transactions simultaneously — without concurrency, each transaction waits for the previous one.

But concurrent access to shared data (like account balances) causes **race conditions**:

```
Thread A: reads balance = 10000
Thread B: reads balance = 10000        ← same value!
Thread A: balance - 3000 = 7000 → writes 7000
Thread B: balance - 5000 = 5000 → writes 5000  ← Thread A's debit is LOST!
```

### Key Concurrency Tools

| Tool | What It Does |
|---|---|
| `ExecutorService` | Manages a pool of reusable threads |
| `Future` | A handle to get the result of an async task |
| `ReentrantLock` | Only one thread can enter a protected block at a time |
| `AtomicInteger` | Thread-safe counter (no lock needed) |
| `ConcurrentHashMap` | Thread-safe Map |

---

## What You'll Build

A `BatchProcessor` that:
1. Takes a list of pending transfers
2. Processes them in parallel using `ExecutorService`
3. Protects account balances with `ReentrantLock` (via `AccountLocker`)
4. Tracks success/fail counts with `AtomicInteger`

---

## Full Demo Flow

### Part A: Demo 1 (Stream API + Lambda)

```bash
# 1. Switch to Demo 1 starter branch
git checkout day2-demo-1-starter

# 2. Compile and run — shows "not implemented yet"
mvn clean compile
mvn exec:java -Dexec.mainClass="com.example.demo1.Demo1Starter"

# 3. Participants implement 4 TODOs in TransactionAnalytics.java

# 4. Recompile and run — shows analytics results
mvn clean compile
mvn exec:java -Dexec.mainClass="com.example.demo1.Demo1Starter"

# 5. (Optional) Show finished version
git checkout day2-demo-1-finished
mvn clean compile
mvn exec:java -Dexec.mainClass="com.example.demo1.Demo1Finished"
```

### Part B: Demo 2 (Concurrency)

```bash
# 6. Switch to Demo 2 starter branch
git checkout day2-demo-2-starter

# 7. Compile and run — shows "not implemented yet"
mvn clean compile
mvn exec:java -Dexec.mainClass="com.example.demo2.Demo2Starter"

# 8. Participants implement 3 TODOs in AccountLocker.java and BatchProcessor.java

# 9. Recompile and run — shows parallel processing results
mvn clean compile
mvn exec:java -Dexec.mainClass="com.example.demo2.Demo2Starter"

# 10. Show finished version (unsafe vs safe comparison)
git checkout day2-demo-2-finished
mvn clean compile
mvn exec:java -Dexec.mainClass="com.example.demo2.Demo2Finished"
```

---

## How to Demo

### Prerequisites

- **Java 17** installed
- **Maven** installed
- **PostgreSQL** running on `localhost:5432` with user `postgres` / password `postgres`
- **Demo 1 completed** — the `account` and `transaction_history` tables must exist

### Step 1: Compile the Project

```bash
mvn clean compile
```

Expect `BUILD SUCCESS`.

### Step 2: Run the Starter (Before Implementing)

```bash
mvn exec:java -Dexec.mainClass="com.example.demo2.Demo2Starter"
```

This will:
- Reset account balances (A001: 100000, A002: 50000)
- Generate a batch of 20 random transfers
- Attempt to process them — but concurrency logic is not implemented yet

**Show this output to participants** — the batch processing fails because TODOs are empty.

### Step 3: Implement the TODOs

Participants fill in 3 TODO methods in `service/AccountLocker.java` and `service/BatchProcessor.java` (see steps below).

### Step 4: Recompile and Run Again

```bash
mvn clean compile
mvn exec:java -Dexec.mainClass="com.example.demo2.Demo2Starter"
```

Now the batch processes transfers in parallel with proper thread safety.

### Step 5: Run the Finished Version (Unsafe vs Safe Comparison)

```bash
mvn exec:java -Dexec.mainClass="com.example.demo2.Demo2Finished"
```

This runs the batch **twice**:
1. **Without locks** — shows inconsistent balances, money gets lost
2. **With locks** — shows correct balances, no money lost

**This is the most impactful demo moment** — the visual difference proves why concurrency matters.

---

## Step-by-Step Implementation

### Step 1: Understand the Problem

Open `Demo2Starter.java` to see the batch setup. It creates 20 transfers between A001 and A002, then calls `BatchProcessor` to process them in parallel. Without locks, race conditions corrupt the balances.

### Step 2: Implement the TODOs

#### TODO 1 — AccountLocker: Thread-safe lock management

**File:** `src/main/java/com/example/service/AccountLocker.java` → methods `getLock()`, `lockAccounts()`, `unlockAccounts()`

```java
private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

public ReentrantLock getLock(String accountNumber) {
    return locks.computeIfAbsent(accountNumber, k -> new ReentrantLock());
}

public void lockAccounts(String account1, String account2) {
    String first = account1.compareTo(account2) < 0 ? account1 : account2;
    String second = account1.compareTo(account2) < 0 ? account2 : account1;
    getLock(first).lock();
    getLock(second).lock();
}

public void unlockAccounts(String account1, String account2) {
    getLock(account1).unlock();
    getLock(account2).unlock();
}
```

**Concurrency concept:** `ConcurrentHashMap.computeIfAbsent()` creates locks lazily and thread-safely. Lock ordering (alphabetical) prevents **deadlocks** — if Thread A locks A001→A002 and Thread B locks A002→A001, they deadlock. Sorting prevents this.

#### TODO 2 — BatchProcessor: Parallel execution with ExecutorService

**File:** `src/main/java/com/example/service/BatchProcessor.java` → method `processBatch()`

```java
public void processBatch(List<Transaction> batch) {
    List<Future<Boolean>> futures = new ArrayList<>();

    for (Transaction t : batch) {
        Future<Boolean> future = executor.submit(() -> processSingleTransfer(t));
        futures.add(future);
    }

    for (Future<Boolean> f : futures) {
        try {
            f.get();
        } catch (Exception e) {
            System.out.println("Task error: " + e.getMessage());
        }
    }
}
```

**Concurrency concept:** `executor.submit()` sends each transfer to the thread pool. It returns a `Future` — a handle to the async result. `f.get()` blocks until that task finishes. The thread pool runs multiple transfers simultaneously.

#### TODO 3 — BatchProcessor: Thread-safe single transfer

**File:** `src/main/java/com/example/service/BatchProcessor.java` → method `processSingleTransfer()`

```java
private boolean processSingleTransfer(Transaction t) {
    locker.lockAccounts(t.getSourceAccount(), t.getTargetAccount());
    try {
        Account source = repository.findByAccountNumber(t.getSourceAccount());
        Account target = repository.findByAccountNumber(t.getTargetAccount());

        source.debit(t.getAmount());
        target.credit(t.getAmount());

        repository.update(source);
        repository.update(target);

        successCount.incrementAndGet();
        return true;
    } catch (Exception e) {
        failCount.incrementAndGet();
        return false;
    } finally {
        locker.unlockAccounts(t.getSourceAccount(), t.getTargetAccount());
    }
}
```

**Concurrency concept:** Lock → try → business logic → finally unlock. The `finally` block **always** runs, even if an exception occurs, ensuring locks are released. `AtomicInteger.incrementAndGet()` is thread-safe without needing a lock.

---

## Running the Finished App

After all 3 TODOs are implemented, compile and run:

```bash
mvn clean compile
mvn exec:java -Dexec.mainClass="com.example.demo2.Demo2Finished"
```

### Expected Output

```
==================================================
  ROUND 1: WITHOUT LOCKS (UNSAFE)
==================================================

Initial Balances: A001=100000.00, A002=50000.00 (Total=150000.00)
Processing 20 transfers with 4 threads (NO LOCKS)...

Final Balances: A001=108000.00, A002=62000.00 (Total=170000.00)
Balance check: 150000.00 vs 170000.00 ✗ MONEY LOST!

==================================================
  ROUND 2: WITH LOCKS (SAFE)
==================================================

Initial Balances: A001=100000.00, A002=50000.00 (Total=150000.00)
Processing 20 transfers with 4 threads (WITH LOCKS)...

Successful: 20, Failed: 0
Final Balances: A001=100000.00, A002=50000.00 (Total=150000.00)
Balance check: 150000.00 vs 150000.00 ✓ (no money lost!)
```

---

## Key Concepts Recap

| Concept | Where Used |
|---|---|
| `ExecutorService` | TODO 2 — Managing thread pool for parallel transfers |
| `Future` | TODO 2 — Getting results from async tasks |
| `ReentrantLock` | TODO 1 — Protecting account per-account |
| `ConcurrentHashMap` | TODO 1 — Thread-safe lock storage |
| `AtomicInteger` | TODO 3 — Counting successes/failures |
| Lock ordering | TODO 1 — Preventing deadlocks |

---

## Demo Flow Summary

```
1. mvn clean compile                    → Compile everything
2. mvn exec:java ...Demo2Starter        → Show "not implemented" state
3. Implement AccountLocker (TODO 1)     → Explain locks + deadlock prevention
4. Implement BatchProcessor (TODO 2+3)  → Explain ExecutorService + Future
5. mvn clean compile && run Starter     → Show working parallel processing
6. mvn exec:java ...Demo2Finished       → Show unsafe vs safe comparison (WOW moment)
```
