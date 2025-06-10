package com.Cameraopencv;

public class PlateEntry {
    public String id;
    public String imagePath;
    public String recognizedText;
    public String timestamp;

    public PlateEntry(String id, String imagePath, String recognizedText, String timestamp) {
        this.id = id;
        this.imagePath = imagePath;
        this.recognizedText = recognizedText;
        this.timestamp = timestamp;
    }
}
