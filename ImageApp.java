package com.Cameraopencv;

import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.videoio.VideoCapture;

import java.io.File;
import java.sql.*;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ImageApp extends Application {

    private ImageView originalView;
    private ImageView edgeView;
    private ImageView croppedView;
    private Label licensePlateLabel = new Label("Detected: ");
    private boolean cameraActive = false;
    private TableView<PlateEntry> tableView = new TableView<>();

    @Override
    public void start(Stage primaryStage) {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        primaryStage.setTitle("License Plate Recognition");

        originalView = new ImageView();
        edgeView = new ImageView();
        croppedView = new ImageView();

        originalView.setFitWidth(500);
        edgeView.setFitWidth(500);
        croppedView.setFitWidth(500);

        originalView.setPreserveRatio(true);
        edgeView.setPreserveRatio(true);
        croppedView.setPreserveRatio(true);

        licensePlateLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: green;");

        Button uploadImageButton = new Button("Upload Image");
        uploadImageButton.setOnAction(e -> handleImageUpload(primaryStage));

        Button uploadVideoButton = new Button("Upload Video");
        uploadVideoButton.setOnAction(e -> handleVideoUpload(primaryStage));

        Button startCameraButton = new Button("Start Camera");
        startCameraButton.setOnAction(e -> startCameraCapture());

        Button stopCameraButton = new Button("Stop Camera");
        stopCameraButton.setOnAction(e -> stopCameraCapture());

        Button showDbButton = new Button("Show Database");
        showDbButton.setOnAction(e -> showDatabaseRecords());

        HBox buttonRow = new HBox(10, uploadImageButton, uploadVideoButton, startCameraButton, stopCameraButton, showDbButton);
        buttonRow.setAlignment(Pos.CENTER_LEFT);

        VBox root = new VBox(10,
                buttonRow,
                new HBox(10, originalView, edgeView, croppedView),
                licensePlateLabel,
                tableView);
        root.setPadding(new Insets(15));

        setupTableView();

        Scene scene = new Scene(root, 1000, 700);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void setupTableView() {
        TableColumn<PlateEntry, String> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().id));

        TableColumn<PlateEntry, String> imagePathCol = new TableColumn<>("Image Path");
        imagePathCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().imagePath));

        TableColumn<PlateEntry, String> textCol = new TableColumn<>("Detected Text");
        textCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().recognizedText));

        TableColumn<PlateEntry, String> timeCol = new TableColumn<>("Timestamp");
        timeCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().timestamp));

        tableView.getColumns().addAll(idCol, imagePathCol, textCol, timeCol);
        tableView.setPrefHeight(200);
    }

    private void handleImageUpload(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Image File");
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            Mat original = Imgcodecs.imread(file.getAbsolutePath());
            Mat gray = new Mat();
            Imgproc.cvtColor(original, gray, Imgproc.COLOR_BGR2GRAY);
            Imgproc.GaussianBlur(gray, gray, new org.opencv.core.Size(3, 3), 0);
            Imgproc.Canny(gray, gray, 100, 200);

            String tempProcessed = "processed_" + UUID.randomUUID() + ".png";
            String tempCropped = "cropped_" + UUID.randomUUID() + ".png";
            String tempOutput = "output_" + UUID.randomUUID() + ".png";
            Imgcodecs.imwrite(tempProcessed, gray);

            LicensePlateDetection.detectLicensePlate(tempProcessed, file.getAbsolutePath(), tempCropped, tempOutput);

            updateImageViews(tempOutput, tempProcessed, tempCropped);

            File croppedFile = new File(tempCropped);
            ITesseract tesseract = new Tesseract();
            tesseract.setDatapath("C:/Program Files/Tesseract-OCR/tessdata");

            try {
                String detectedText = tesseract.doOCR(croppedFile).trim();
                System.out.println("Detected Text: " + detectedText);
                licensePlateLabel.setText("Detected: " + detectedText);
                saveToDatabase(detectedText, file.getAbsolutePath());
            } catch (TesseractException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleVideoUpload(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Video File");
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            VideoCapture capture = new VideoCapture(file.getAbsolutePath());

            if (!capture.isOpened()) {
                System.err.println("Failed to open video file.");
                return;
            }

            new Thread(() -> {
                Mat frame = new Mat();
                int frameCount = 0;

                ITesseract tesseract = new Tesseract();
                tesseract.setDatapath("C:/Program Files/Tesseract-OCR/tessdata"); // Adjust if needed

                String xmlPath = "resources/haarcascade_russian_plate_number.xml";
                CascadeClassifier plateCascade = new CascadeClassifier(xmlPath);


                if (plateCascade.empty()) {
                    System.err.println("Failed to load Haar Cascade for plates.");
                    return;
                }

                Set<String> detectedPlates = new HashSet<>();

                while (capture.read(frame)) {
                    if (frame.empty()) break;

                    frameCount++;
                    if (frameCount % 5 != 0) continue; // Process every 5th frame

                    Mat gray = new Mat();
                    Imgproc.cvtColor(frame, gray, Imgproc.COLOR_BGR2GRAY);

                    MatOfRect plates = new MatOfRect();
                    plateCascade.detectMultiScale(gray, plates);

                    for (Rect rect : plates.toArray()) {
                        Mat plateRegion = new Mat(frame, rect);
                        Imgproc.rectangle(frame, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(0, 255, 0), 2);
                        
                        
                        String croppedPath = "plate_" + UUID.randomUUID() + ".png";
                        Imgcodecs.imwrite(croppedPath, plateRegion);

                        try {
                            String plateText = tesseract.doOCR(new File(croppedPath)).replaceAll("[^A-Z0-9]", "").trim();

                            if (!plateText.isEmpty() && !detectedPlates.contains(plateText)) {
                                detectedPlates.add(plateText);

                                System.out.println("Detected: " + plateText);

                                // ✅ Use OpenCV's Point here (not JavaFX Point)
                                Imgproc.putText(frame, plateText, new org.opencv.core.Point(rect.x, rect.y - 10),
                                        Imgproc.FONT_HERSHEY_SIMPLEX, 0.8, new Scalar(0, 255, 0), 2);

                                String frameOutput = "annotated_frame_" + UUID.randomUUID() + ".png";
                                Imgcodecs.imwrite(frameOutput, frame);

                                javafx.application.Platform.runLater(() -> {
                                    originalView.setImage(new Image(new File(frameOutput).toURI().toString()));
                                    licensePlateLabel.setText("Detected: " + plateText);
                                });

                                saveToDatabase(plateText, file.getAbsolutePath());
                            }
                        } catch (TesseractException e) {
                            e.printStackTrace();
                        }
                    }

                    // Update UI with current annotated frame
                    String tempPath = "live_" + UUID.randomUUID() + ".png";
                    Imgcodecs.imwrite(tempPath, frame);
                    javafx.application.Platform.runLater(() -> {
                        originalView.setImage(new Image(new File(tempPath).toURI().toString()));
                    });

                    try {
                        Thread.sleep(100); // Adjust if video is too fast
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                capture.release();
            }).start();
        }
    }



    private VideoCapture camera;

    private void startCameraCapture() {
        if (cameraActive) return;

        camera = new VideoCapture(0);
        if (!camera.isOpened()) {
            System.err.println("Cannot open camera.");
            return;
        }

        cameraActive = true;

        Thread camThread = new Thread(() -> {
            Mat frame = new Mat();
            int frameCount = 0;

            while (cameraActive) {
                camera.read(frame);
                if (frame.empty()) continue;

                frameCount++;

                if (frameCount % 30 == 0) {
                    Mat gray = new Mat();
                    Imgproc.cvtColor(frame, gray, Imgproc.COLOR_BGR2GRAY);
                    Imgproc.GaussianBlur(gray, gray, new org.opencv.core.Size(3, 3), 0);
                    Imgproc.Canny(gray, gray, 100, 200);

                    String tempProcessed = "cam_processed_" + UUID.randomUUID() + ".png";
                    String tempCropped = "cam_cropped_" + UUID.randomUUID() + ".png";
                    String tempOutput = "cam_output_" + UUID.randomUUID() + ".png";
                    Imgcodecs.imwrite(tempProcessed, gray);

                    LicensePlateDetection.detectLicensePlate(tempProcessed, tempProcessed, tempCropped, tempOutput);

                    File croppedFile = new File(tempCropped);
                    ITesseract tesseract = new Tesseract();
                    tesseract.setDatapath("C:/Program Files/Tesseract-OCR/tessdata");

                    try {
                        String detectedText = tesseract.doOCR(croppedFile).trim();
                        if (!detectedText.isEmpty()) {
                            System.out.println("Detected from camera: " + detectedText);
                            licensePlateLabel.setText("Detected: " + detectedText);
                            saveToDatabase(detectedText, "Camera Feed");

                            updateImageViews(tempOutput, tempProcessed, tempCropped);
                            break;
                        }
                    } catch (TesseractException e) {
                        e.printStackTrace();
                    }
                }
            }

            camera.release();
        });

        camThread.setDaemon(true);
        camThread.start();
    }

    private void stopCameraCapture() {
        cameraActive = false;
        if (camera != null && camera.isOpened()) {
            camera.release();
            System.out.println("Camera stopped.");
        }
    }

    private void updateImageViews(String originalPath, String edgePath, String croppedPath) {
        originalView.setImage(new Image(new File(originalPath).toURI().toString()));
        edgeView.setImage(new Image(new File(edgePath).toURI().toString()));
        croppedView.setImage(new Image(new File(croppedPath).toURI().toString()));
    }

    private void saveToDatabase(String plateNumber, String imagePath) {
        String url = "jdbc:mysql://localhost:3306/licenseplate_db";
        String user = "root";
        String password = "vidyajames#43";

        String insertQuery = "INSERT INTO recognized_plates (image_path, recognized_text) VALUES (?, ?)";

        try (Connection conn = DriverManager.getConnection(url, user, password);
             PreparedStatement stmt = conn.prepareStatement(insertQuery)) {

            stmt.setString(1, imagePath);
            stmt.setString(2, plateNumber);

            int rowsInserted = stmt.executeUpdate();
            if (rowsInserted > 0) {
                System.out.println("Plate number saved to database.");
            }

        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
        }
    }

    private void showDatabaseRecords() {
        ObservableList<PlateEntry> data = FXCollections.observableArrayList();

        String url = "jdbc:mysql://localhost:3306/licenseplate_db";
        String user = "root";
        String password = "vidyajames#43";

        String selectQuery = "SELECT * FROM recognized_plates";

        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(selectQuery)) {

            while (rs.next()) {
                data.add(new PlateEntry(
                        rs.getString("id"),
                        rs.getString("image_path"),
                        rs.getString("recognized_text"),
                        rs.getString("timestamp")
                ));
            }

            tableView.setItems(data);

        } catch (SQLException e) {
            System.err.println("Error loading database: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

    public static class PlateEntry {
        String id;
        String imagePath;
        String recognizedText;
        String timestamp;

        public PlateEntry(String id, String imagePath, String recognizedText, String timestamp) {
            this.id = id;
            this.imagePath = imagePath;
            this.recognizedText = recognizedText;
            this.timestamp = timestamp;
        }
    }

}
