package com.Cameraopencv;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class LicensePlateDetection {

    public static void detectLicensePlate(String inputPath, String originalPath, String croppedPath, String outputPath) {
        // Load the processed image (e.g., after Canny)
        Mat src = Imgcodecs.imread(inputPath);
        Mat gray = new Mat();

        // ✅ Ensure grayscale for findContours
        if (src.channels() > 1) {
            Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY);
        } else {
            gray = src.clone();
        }

        // ✅ Optional: Apply binary thresholding to aid contour detection
        Imgproc.threshold(gray, gray, 0, 255, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);

        // 🔍 Find contours
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(gray, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

        // Load the original image for drawing bounding boxes
        Mat original = Imgcodecs.imread(originalPath);

        Rect plateRect = null;
        for (MatOfPoint contour : contours) {
            Rect rect = Imgproc.boundingRect(contour);
            float aspectRatio = (float) rect.width / (float) rect.height;

            if (aspectRatio > 2 && aspectRatio < 6 && rect.width > 60 && rect.height > 20) {
                plateRect = rect;
                Imgproc.rectangle(original, new Point(rect.x, rect.y),
                        new Point(rect.x + rect.width, rect.y + rect.height),
                        new Scalar(0, 255, 0), 2);
                break; // Take first likely match
            }
        }

        // 🟢 Save output image with green box
        Imgcodecs.imwrite(outputPath, original);

        if (plateRect != null) {
            Mat croppedPlate = new Mat(original, plateRect);
            Imgcodecs.imwrite(croppedPath, croppedPlate);
        } else {
            System.err.println("No suitable license plate contour found.");
        }
    }
}
