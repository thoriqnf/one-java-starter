package com.example;

import com.example.repository.*;
import com.example.service.TransferService;

import java.sql.Connection;
import java.sql.DriverManager;

public class Main {

    public static void main(String[] args) {

        String url = "jdbc:postgresql://localhost:5432/postgres";
        String user = "postgres";
        String password = "postgres";

        try (Connection conn = DriverManager.getConnection(url, user, password)) {

            conn.setAutoCommit(false);

            AccountRepository repository = new PostgresAccountRepository(conn);
            TransferService service = new TransferService(repository);

            service.transfer("A001", "A002", 100);

            conn.commit();

            System.out.println("Transfer success!");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
