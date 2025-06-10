package com.Cameraopencv;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.TesseractException;


public class PlateRecognizer {
	
	public static void processImage(String imagePath, ImageView originalView, ImageView edgeView, ImageView croppedView, Label resultLabel) {
	    // Load image
	    Mat original = Imgcodecs.imread(imagePath);
	    if (original.empty()) {
	        System.err.println("Cannot load image: " + imagePath);
	        return;
	    }

	    // Preprocessing: grayscale, blur, edge detection
	    Mat gray = new Mat();
	    Imgproc.cvtColor(original, gray, Imgproc.COLOR_BGR2GRAY);
	    Imgproc.GaussianBlur(gray, gray, new Size(3, 3), 0);
	    Imgproc.Canny(gray, gray, 100, 200);

	    // Save temp files
	    String edgePath = "edge_" + UUID.randomUUID() + ".png";
	    String croppedPath = "cropped_" + UUID.randomUUID() + ".png";
	    String boxedPath = "boxed_" + UUID.randomUUID() + ".png";

	    Imgcodecs.imwrite(edgePath, gray);

	    // Plate detection
	    detectAndDrawPlate(edgePath, imagePath, croppedPath, boxedPath);

	    // Display results
	    originalView.setImage(new Image(new File(boxedPath).toURI().toString()));
	    edgeView.setImage(new Image(new File(edgePath).toURI().toString()));
	    croppedView.setImage(new Image(new File(croppedPath).toURI().toString()));

	    // OCR
	    File croppedFile = new File(croppedPath);
	    ITesseract tesseract = new Tesseract();
	    tesseract.setDatapath("C:/Program Files/Tesseract-OCR/tessdata");

	    try {
	        String detectedText = tesseract.doOCR(croppedFile).trim();
	        System.out.println("Detected: " + detectedText);
	        resultLabel.setText("Detected: " + detectedText);
	    } catch (TesseractException e) {
	        e.printStackTrace();
	        resultLabel.setText("OCR Error");
	    }
	}
	
	public static void processVideo(
	        String videoPath,
	        ImageView originalView,
	        ImageView edgeView,
	        ImageView croppedView,
	        Label licensePlateLabel,
	        ProgressBar progressBar) {

	    VideoCapture capture = new VideoCapture(videoPath);

	    if (!capture.isOpened()) {
	        System.err.println("Failed to open video file.");
	        return;
	    }

	    Mat frame = new Mat();
	    int frameCount = 0;
	    int totalFrames = (int) capture.get(Videoio.CAP_PROP_FRAME_COUNT);

	    while (capture.read(frame)) {
	        frameCount++;
	        if (frame.empty()) continue;

	        // ProgressBar update
	        double progress = (double) frameCount / totalFrames;
	        Platform.runLater(() -> progressBar.setProgress(progress));

	        if (frameCount % 15 == 0) {
	            Mat gray = new Mat();
	            Imgproc.cvtColor(frame, gray, Imgproc.COLOR_BGR2GRAY);
	            Imgproc.GaussianBlur(gray, gray, new Size(3, 3), 0);
	            Imgproc.Canny(gray, gray, 100, 200);

	            String tempProcessed = "processed_" + UUID.randomUUID() + ".png";
	            String tempCropped = "cropped_" + UUID.randomUUID() + ".png";
	            String tempOutput = "output_" + UUID.randomUUID() + ".png";
	            Imgcodecs.imwrite(tempProcessed, gray);

	            LicensePlateDetection.detectLicensePlate(tempProcessed, tempProcessed, tempCropped, tempOutput);

	            File croppedFile = new File(tempCropped);
	            ITesseract tesseract = new Tesseract();
	            tesseract.setDatapath("C:/Program Files/Tesseract-OCR/tessdata");

	            try {
	                String detectedText = tesseract.doOCR(croppedFile).trim();
	                if (!detectedText.isEmpty()) {
	                    System.out.println("Detected from video: " + detectedText);
	                    Platform.runLater(() -> {
	                        licensePlateLabel.setText("Detected: " + detectedText);
	                        originalView.setImage(new Image(new File(tempOutput).toURI().toString()));
	                        edgeView.setImage(new Image(new File(tempProcessed).toURI().toString()));
	                        croppedView.setImage(new Image(new File(tempCropped).toURI().toString()));
	                    });
	                    break;
	                }
	            } catch (TesseractException e) {
	                e.printStackTrace();
	            }
	        }
	    }

	    capture.release();
	    Platform.runLater(() -> progressBar.setProgress(1.0));
	}



    public static void detectAndDrawPlate(String edgeImagePath, String originalImagePath, String croppedOutputPath, String boxedOutputPath) {
        Mat original = Imgcodecs.imread(originalImagePath);
        Mat edges = Imgcodecs.imread(edgeImagePath, Imgcodecs.IMREAD_GRAYSCALE);

        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(edges, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

        Rect bestRect = null;
        double bestRatioDiff = Double.MAX_VALUE;

        for (MatOfPoint contour : contours) {
            Rect rect = Imgproc.boundingRect(contour);
            double aspectRatio = rect.width / (double) rect.height;
            double area = rect.area();

            if (area > 1000 && aspectRatio >= 2 && aspectRatio <= 6) {
                double ratioDiff = Math.abs(aspectRatio - 4.0); // Ideal ratio ~4.0
                if (ratioDiff < bestRatioDiff) {
                    bestRatioDiff = ratioDiff;
                    bestRect = rect;
                }
            }
        }

        if (bestRect != null) {
            // Draw green bounding box
            Imgproc.rectangle(original, bestRect.tl(), bestRect.br(), new Scalar(0, 255, 0), 3);

            // Save boxed output image
            Imgcodecs.imwrite(boxedOutputPath, original);

            // Save cropped plate image
            Mat plate = new Mat(original, bestRect);
            Imgcodecs.imwrite(croppedOutputPath, plate);
        } else {
            System.out.println("No plate-like region detected.");
        }
    }
}
