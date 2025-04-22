package com.example.iris_recognition.service;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class StorageService {

    @Value("${app.upload.path}")
    private String uploadDir;

    @Value("${app.training.data.path}")
    private String trainingDataDir;

    public String storeFile(MultipartFile file) throws IOException {
        // Create directories if they don't exist
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String fileExtension = getFileExtension(originalFilename);
        String filename = UUID.randomUUID().toString() + fileExtension;

        // Copy file to upload directory
        Path targetLocation = uploadPath.resolve(filename);
        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

        return targetLocation.toString();
    }

    public String storeTrainingImage(MultipartFile file) throws IOException {
        // Create training data directory if it doesn't exist
        Path trainingPath = Paths.get(trainingDataDir, "images");
        if (!Files.exists(trainingPath)) {
            Files.createDirectories(trainingPath);
        }

        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String fileExtension = getFileExtension(originalFilename);
        String filename = UUID.randomUUID().toString() + fileExtension;

        // Copy file to training directory
        Path targetLocation = trainingPath.resolve(filename);
        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

        return targetLocation.toString();
    }

    public String storeAnnotation(MultipartFile file) throws IOException {
        // Create annotation directory if it doesn't exist
        Path annotationPath = Paths.get(trainingDataDir, "annotations");
        if (!Files.exists(annotationPath)) {
            Files.createDirectories(annotationPath);
        }

        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String fileExtension = getFileExtension(originalFilename);
        String filename = UUID.randomUUID().toString() + fileExtension;

        // Copy file to annotation directory
        Path targetLocation = annotationPath.resolve(filename);
        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

        return targetLocation.toString();
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf(".") == -1) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }
}
