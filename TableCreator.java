package com.Cameraopencv;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TableCreator {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/licenseplate_db_v2";
        String user = "root";
        String password = "vidyajames#43";

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
        String uniqueTableName = "recognized_plates_" + dtf.format(LocalDateTime.now());

        String createTableSQL = "CREATE TABLE " + uniqueTableName + " ("
                + "id INT AUTO_INCREMENT PRIMARY KEY, "
                + "plate_number VARCHAR(20), "
                + "recognized_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                + ")";

        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(createTableSQL);
            System.out.println("Table created: " + uniqueTableName);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
