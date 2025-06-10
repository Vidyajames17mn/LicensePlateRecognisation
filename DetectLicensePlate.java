package com.Cameraopencv;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class DetectLicensePlate {

    static {
        System.load("C:\\opencv\\build\\java\\x64\\opencv_java4110.dll");
    }

    public static String detectLicensePlate(File file) {
        String imagePath = file.getAbsolutePath();
        Mat image = Imgcodecs.imread(imagePath);
        if (image.empty()) {
            return "Error: Could not load image";
        }

        Mat gray = new Mat();
        Imgproc.cvtColor(image, gray, Imgproc.COLOR_BGR2GRAY);
        Imgproc.GaussianBlur(gray, gray, new Size(5, 5), 0);
        Mat edges = new Mat();
        Imgproc.Canny(gray, edges, 100, 200);

        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(edges, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

        Rect plateRect = null;
        for (MatOfPoint contour : contours) {
            Rect rect = Imgproc.boundingRect(contour);
            float aspectRatio = (float) rect.width / (float) rect.height;
            if (aspectRatio > 2 && aspectRatio < 6 && rect.height > 30 && rect.width > 60) {
                plateRect = rect;
                break;
            }
        }

        if (plateRect == null) {
            return "License plate not found";
        }

        Mat plate = new Mat(image, plateRect);
        String croppedPath = "cropped_plate.png";
        Imgcodecs.imwrite(croppedPath, plate);

        return runTesseract(croppedPath);
    }

    private static String runTesseract(String imagePath) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                "C:\\Program Files\\Tesseract-OCR\\tesseract.exe",
                imagePath,
                "stdout",
                "-l", "eng",
                "--psm", "7"
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }
            process.waitFor();

            return output.toString().trim().replaceAll("[^A-Za-z0-9]", "");
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return "";
        }
    }
}
