# Demo 2 — Batch Transaction Processor with Concurrency

## Objective

Build a batch transaction processor that processes multiple bank transfers **concurrently** using thread pools, while keeping account balances **thread-safe** with locks and atomic operations.

---

## What You'll Build

A `BatchProcessor` that:
1. Takes a list of pending transfers
2. Processes them in parallel using `ExecutorService`
3. Protects account balances with `ReentrantLock` (via `AccountLocker`)
4. Tracks success/fail counts with `AtomicInteger`
5. Demonstrates the difference between **unsafe** and **safe** concurrent execution

---

## How to Demo

### Prerequisites

- **Java 17** installed
- **Maven** installed
- **PostgreSQL** running on `localhost:5432` with user `postgres` / password `postgres`
- **Demo 1 completed** — the `account` and `transaction_history` tables must exist
- The existing `Account`, `AccountRepository`, `Transaction` classes are available

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

**Show this output to participants** — the batch runs single-threaded or fails because TODOs are empty.

### Step 3: Implement the TODOs

Participants fill in the 6 TODO methods in `service/AccountLocker.java` and `service/BatchProcessor.java` (see steps below).

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

### Step 1: Understand the Problem — Race Conditions

Without synchronization, two threads withdrawing from the same account can cause data loss:

```
Thread A: reads balance = 10000
Thread B: reads balance = 10000
Thread A: balance - 3000 = 7000 → writes 7000
Thread B: balance - 5000 = 5000 → writes 5000  ← Thread A's debit is LOST!
```

The `Demo2Starter` will show this problem first, then you fix it.

### Step 2: Implement AccountLocker

Open `service/AccountLocker.java`. This provides per-account locking:

#### TODO 1 — Get or create a lock for an account
```java
private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

public ReentrantLock getLock(String accountNumber) {
    // computeIfAbsent is atomic — safe for concurrent access
    return locks.computeIfAbsent(accountNumber, k -> new ReentrantLock());
}
```
**Concept:** `ConcurrentHashMap` + `computeIfAbsent()` for thread-safe lazy initialization

#### TODO 2 — Lock multiple accounts in a consistent order
```java
public void lockAccounts(String account1, String account2) {
    // Always lock in alphabetical order to prevent deadlocks
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
**Concept:** Lock ordering to prevent **deadlocks**. If Thread A locks A001→A002 and Thread B locks A002→A001, they deadlock. Alphabetical order prevents this.

### Step 3: Implement BatchProcessor

Open `service/BatchProcessor.java`. This orchestrates the parallel execution:

#### TODO 3 — Create the ExecutorService
```java
private final ExecutorService executor;
private final AtomicInteger successCount = new AtomicInteger(0);
private final AtomicInteger failCount = new AtomicInteger(0);

public BatchProcessor(AccountRepository repository, AccountLocker locker, int threadCount) {
    this.repository = repository;
    this.locker = locker;
    this.executor = Executors.newFixedThreadPool(threadCount);
}
```
**Concept:** `Executors.newFixedThreadPool()` creates a reusable pool of threads

#### TODO 4 — Submit transfers as Callable tasks
```java
public void processBatch(List<Transaction> batch) {
    List<Future<Boolean>> futures = new ArrayList<>();

    for (Transaction t : batch) {
        Future<Boolean> future = executor.submit(() -> processSingleTransfer(t));
        futures.add(future);
    }

    // Wait for all to complete
    for (Future<Boolean> f : futures) {
        try {
            f.get(); // blocks until done
        } catch (Exception e) {
            System.out.println("Task error: " + e.getMessage());
        }
    }
}
```
**Concept:** `submit()` returns a `Future` — a handle to the async result. `get()` blocks until done.

#### TODO 5 — Process a single transfer with locks
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
        System.out.println("Transfer failed (" + t.getSourceAccount() + " → "
                + t.getTargetAccount() + "): " + e.getMessage());
        return false;
    } finally {
        locker.unlockAccounts(t.getSourceAccount(), t.getTargetAccount());
    }
}
```
**Concept:** Lock → try → business logic → finally unlock. `AtomicInteger.incrementAndGet()` is thread-safe.

#### TODO 6 — Shutdown and report
```java
public void shutdown() throws InterruptedException {
    executor.shutdown();
    executor.awaitTermination(30, TimeUnit.SECONDS);
}

public int getSuccessCount() { return successCount.get(); }
public int getFailCount() { return failCount.get(); }
```
**Concept:** `shutdown()` stops accepting new tasks. `awaitTermination()` waits for running tasks to finish.

---

## Running the Finished App

After all 6 TODOs are implemented, compile and run:

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
| `ExecutorService` | Managing thread pool for parallel transfers |
| `Callable` / `Future` | Submitting tasks and getting results |
| `ReentrantLock` | Protecting account reads/writes per account |
| `ConcurrentHashMap` | Thread-safe storage for per-account locks |
| `AtomicInteger` | Counting successes/failures without locks |
| Lock ordering | Preventing deadlocks (alphabetical account order) |
| `shutdown()` + `awaitTermination()` | Graceful thread pool cleanup |

---

## Bonus Reference

### ReentrantLock Pattern

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

### AtomicInteger — Lock-Free Counters

```java
private final AtomicInteger successCount = new AtomicInteger(0);
private final AtomicInteger failCount = new AtomicInteger(0);

// Thread-safe increment — no lock needed
successCount.incrementAndGet();
failCount.incrementAndGet();
```

### synchronized vs ReentrantLock

| Feature | `synchronized` | `ReentrantLock` |
|---|---|---|
| Simplicity | ✅ Simple keyword | ❌ Manual lock/unlock |
| Try-lock | ❌ No | ✅ `tryLock()` with timeout |
| Fair ordering | ❌ No guarantee | ✅ `new ReentrantLock(true)` |
| Multiple conditions | ❌ One wait set | ✅ Multiple `Condition` objects |
| Use in banking | Simple cases | Complex transaction locking |

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

---

## Demo Flow Summary

```
1. mvn clean compile                    → Compile everything
2. mvn exec:java ...Demo2Starter        → Show "not implemented" state
3. Implement AccountLocker TODOs        → Explain locks + deadlock prevention
4. Implement BatchProcessor TODOs       → Explain ExecutorService + Future
5. mvn clean compile && run Starter     → Show working parallel processing
6. mvn exec:java ...Demo2Finished       → Show unsafe vs safe comparison (WOW moment)
```
