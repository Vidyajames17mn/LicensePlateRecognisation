package com.Cameraopencv;

import java.io.File;

public class TestPlateRecognition {
    public static void main(String[] args) {
        File testImage = new File("test_car.jpg"); // make sure this file exists
        String result = LicensePlateDetector.detectLicensePlate(testImage);
        System.out.println("Final Detected Plate Text: " + result);
    }
}
