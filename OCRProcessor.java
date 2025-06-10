package com.Cameraopencv;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class OCRProcessor {

    public static void processAndRecognizePlate(Mat cropped) {
        // Apply thresholding to improve OCR
        Mat gray = new Mat();
        Imgproc.cvtColor(cropped, gray, Imgproc.COLOR_BGR2GRAY);
        Imgproc.threshold(gray, gray, 0, 255, Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU);

        // Optionally upscale for better OCR
        Mat resized = new Mat();
        Imgproc.resize(gray, resized, new Size(cropped.width() * 2, cropped.height() * 2));

        // Save image to file for Tesseract
        String tempCropped = "cropped_" + System.currentTimeMillis() + ".png";
        Imgcodecs.imwrite(tempCropped, resized);
        File croppedFile = new File(tempCropped);

        String detectedText = "";

        try {
            ITesseract tesseract = new Tesseract();
            tesseract.setDatapath("C:/Program Files/Tesseract-OCR"); // path to Tesseract-OCR, not tessdata
            tesseract.setLanguage("eng");
            tesseract.setTessVariable("user_defined_dpi", "300");
            tesseract.setPageSegMode(6); // Assume single block of text

            System.out.println("Performing OCR on: " + croppedFile.getAbsolutePath());
            detectedText = tesseract.doOCR(croppedFile);
            detectedText = detectedText.trim(); // Remove whitespace

            if (detectedText.isEmpty()) {
                System.out.println("No text detected, inserting blank record.");
            } else {
                System.out.println("Detected Text: " + detectedText);
            }

        } catch (TesseractException e) {
            System.err.println("OCR failed: " + e.getMessage());
        }

        saveDetectedTextToDatabase(detectedText);

        // Optional: Delete temp image after use
        if (croppedFile.exists()) {
            croppedFile.delete();
        }
    }

    private static void saveDetectedTextToDatabase(String detectedText) {
        String url = "jdbc:mysql://localhost:3306/licenseplate_db";
        String user = "root"; // TODO: Replace with your MySQL username
        String password = "vidyajames#43"; // TODO: Replace with your MySQL password

        String query = "INSERT INTO recognized_plates (plate_text, timestamp) VALUES (?, NOW())";

        try (Connection conn = DriverManager.getConnection(url, user, password);
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, detectedText); // Always insert (even if empty)
            stmt.executeUpdate();
            System.out.println("Inserted into DB: '" + detectedText + "'");

        } catch (SQLException e) {
            System.err.println("Database insert failed: " + e.getMessage());
        }
    }
}
