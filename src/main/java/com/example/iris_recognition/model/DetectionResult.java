package com.example.iris_recognition.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DetectionResult {
    private String imageName;
    private List<BoundingBox> detections;
    private long processingTimeMs;
    private String annotatedImagePath;
    private String errorMessage; // Thêm trường thông báo lỗi

    public DetectionResult(String imageName, List<BoundingBox> detections, long processingTimeMs) {
        this.imageName = imageName;
        this.detections = detections;
        this.processingTimeMs = processingTimeMs;
        this.annotatedImagePath = null;
        this.errorMessage = null;
    }

    public DetectionResult(String imageName, List<BoundingBox> detections,
                           long processingTimeMs, String errorMessage) {
        this.imageName = imageName;
        this.detections = detections;
        this.processingTimeMs = processingTimeMs;
        this.annotatedImagePath = null;
        this.errorMessage = errorMessage;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BoundingBox {
        private int x;
        private int y;
        private int width;
        private int height;
        private double confidence;
        private String label;
    }
}