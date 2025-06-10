package com.Cameraopencv;

import java.io.File;
import net.sourceforge.tess4j.*;

public class TesseractExample {
    public static void main(String[] args) {
        // Create an instance of Tesseract
        Tesseract instance = new Tesseract();

        // Set the path to the tessdata folder (where language files are located)
        instance.setDatapath("C:\\Program Files\\Tesseract-OCR\\tessdata");

        // Path to the image
        File imageFile = new File("C:\\Users\\chand\\OneDrive\\Pictures\\google download\\plate.jpeg");

        try {
            // Perform OCR on the image and print the result
            String result = instance.doOCR(imageFile);
            System.out.println(result);
        } catch (TesseractException e) {
            e.printStackTrace();
        }
    }
}
