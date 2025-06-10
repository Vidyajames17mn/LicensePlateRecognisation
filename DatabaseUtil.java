package com.Cameraopencv;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.*;

public class DatabaseUtil {

    public static ObservableList<PlateEntry> getAllRecords() {
        ObservableList<PlateEntry> list = FXCollections.observableArrayList();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM recognized_plates");
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                PlateEntry entry = new PlateEntry(
                        rs.getString("id"),
                        rs.getString("image_path"),
                        rs.getString("recognized_text"),
                        rs.getString("timestamp")
                );
                list.add(entry);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public static void clearAllRecords() {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM recognized_plates")) {
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
