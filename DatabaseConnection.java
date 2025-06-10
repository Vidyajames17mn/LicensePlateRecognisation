package com.Cameraopencv;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    private static final String URL = "jdbc:mysql://localhost:3306/licenseplate_db";
    private static final String USER = "root"; // 👈 Replace with your MySQL username
    private static final String PASSWORD = "vidyajames#43"; // 👈 Replace with your MySQL password

    public static Connection getConnection() {
        try {
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
}
