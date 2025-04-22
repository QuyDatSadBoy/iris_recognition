package com.example.iris_recognition.service;

import com.example.iris_recognition.model.DetectionResult;
import com.example.iris_recognition.util.YoloModelUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class DetectionService {
    private static final Logger log = LoggerFactory.getLogger(DetectionService.class);

    private final StorageService storageService;
    private final YoloModelUtil yoloModelUtil;

    @Value("${app.upload.path}")
    private String uploadDir;

    @Autowired
    public DetectionService(StorageService storageService, YoloModelUtil yoloModelUtil) {
        this.storageService = storageService;
        this.yoloModelUtil = yoloModelUtil;
    }

    public DetectionResult detectIrisFromImage(MultipartFile file) throws IOException {
        try {
            // Store uploaded file
            String imagePath = storageService.storeFile(file);

            // Process image with YOLO model
            DetectionResult result = yoloModelUtil.detectIris(imagePath);

            // Draw detection boxes on the image and save it
            if (!result.getDetections().isEmpty()) {
                String annotatedImagePath = yoloModelUtil.drawDetectionBoxes(imagePath, result);
                result.setAnnotatedImagePath(annotatedImagePath);
            }

            return result;
        } catch (Exception e) {
            log.error("Error in iris detection process: ", e);
            throw new IOException("Failed to process image: " + e.getMessage());
        }
    }
}