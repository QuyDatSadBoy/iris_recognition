package com.example.iris_recognition.controller;

import com.example.iris_recognition.model.DetectionResult;
import com.example.iris_recognition.service.DetectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/detect")
public class DetectionController {
    private static final Logger log = LoggerFactory.getLogger(DetectionController.class);

    private final DetectionService detectionService;

    @Autowired
    public DetectionController(DetectionService detectionService) {
        this.detectionService = detectionService;
    }

    @PostMapping
    public ResponseEntity<?> detectIris(@RequestParam("image") MultipartFile file) {
        try {
            log.info("Received detection request for file: " + file.getOriginalFilename());

            if (file.isEmpty()) {
                log.warn("Empty file submitted");
                Map<String, String> error = new HashMap<>();
                error.put("error", "Please select a file to upload");
                return ResponseEntity.badRequest().body(error);
            }

            DetectionResult result = detectionService.detectIrisFromImage(file);

            // Kiểm tra xem có lỗi không
            if (result.getErrorMessage() != null && !result.getErrorMessage().isEmpty()) {
                log.warn("Detection completed with error: " + result.getErrorMessage());
                Map<String, String> response = new HashMap<>();
                response.put("error", result.getErrorMessage());
                response.put("imageName", result.getImageName());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }

            log.info("Detection completed successfully for: " + file.getOriginalFilename());
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            log.error("Error processing upload: ", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Error processing image: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping("/image/{filename}")
    public ResponseEntity<Resource> getDetectedImage(@PathVariable String filename) {
        try {
            // Tạo đường dẫn đến file ảnh đã được annotation
            Path imagePath = Paths.get("uploads/annotated_" + filename);
            File file = imagePath.toFile();

            if (!file.exists()) {
                log.warn("Annotated image not found: " + filename);
                return ResponseEntity.notFound().build();
            }

            ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(imagePath));

            return ResponseEntity.ok()
                    .contentLength(file.length())
                    .contentType(MediaType.IMAGE_JPEG)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getName() + "\"")
                    .body(resource);
        } catch (IOException e) {
            log.error("Error returning image: ", e);
            return ResponseEntity.badRequest().build();
        }
    }
}