import java.sql.Connection;
import java.sql.DriverManager;

public class testcon {
    public static void main(String[] args) {
        String url = "jdbc:postgresql://localhost:5432/postgres";
        String user = "postgres";
        String pass = "OneBC40226"; 

        try (Connection conn = DriverManager.getConnection(url, user, pass)) {
            if (conn != null) {
                System.out.println("✅ Koneksi ke PostgreSQL 17 Berhasil!");
            }
        } catch (Exception e) {
            System.err.println("❌ Gagal: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
