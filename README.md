# PostgreSQL Java Connection Setup

A simple Java project to test PostgreSQL connection.

## Prerequisites
- Java 17
- PostgreSQL 17 running locally on `5432` with user `postgres` and password `OneBC40226`

---

##  Easy Way (Using Maven)
Run this command in the project root:
```cmd
mvn clean compile exec:java
```

---

## Manual Way (Windows Command Prompt)
If you don't use Maven, follow these steps:

1. **Download the driver:** Ensure `postgresql-42.7.2.jar` is in this folder.
2. **Compile the code:**
   ```cmd
   javac src\main\java\testcon.java -d .
   ```
3. **Run the code:** (Note the `;` used in Windows)
   ```cmd
   java -cp ".;postgresql-42.7.2.jar" testcon
   ```

### Expected Output
```text
✅ Koneksi ke PostgreSQL 17 Berhasil!
```
