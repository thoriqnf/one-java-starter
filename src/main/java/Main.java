import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class Main {
    public static void main(String[] args) {
        String url = "jdbc:postgresql://localhost:5432/postgres";
        String user = "postgres";
        String pass = "OneBC40226";

        try (Connection conn = DriverManager.getConnection(url, user, pass)) {
            // 1. Initialize Schema
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("CREATE TABLE IF NOT EXISTS accounts (" +
                        "account_number VARCHAR(50) PRIMARY KEY, " +
                        "balance DECIMAL(15, 2) NOT NULL DEFAULT 0.00)");
                
                // Seed data if empty
                stmt.execute("INSERT INTO accounts (account_number, balance) VALUES ('A001', 1000.00) ON CONFLICT DO NOTHING");
                stmt.execute("INSERT INTO accounts (account_number, balance) VALUES ('A002', 500.00) ON CONFLICT DO NOTHING");
            }

            // 2. Setup Components
            AccountRepository repository = new JdbcAccountRepository(conn);
            TransferService transferService = new TransferService(repository);

            // 3. Perform Transfer
            System.out.println("Transferring 200 from A001 to A002...");
            transferService.transfer("A001", "A002", 200.0);

            // 4. Verify Results
            Account a001 = repository.findByAccountNumber("A001");
            Account a002 = repository.findByAccountNumber("A002");
            
            System.out.println("New Balance A001: " + a001.getBalance());
            System.out.println("New Balance A002: " + a002.getBalance());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
