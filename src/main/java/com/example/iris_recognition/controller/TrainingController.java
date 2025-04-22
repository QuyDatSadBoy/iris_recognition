package com.example.iris_recognition.controller;


import com.example.iris_recognition.model.TrainingData;
import com.example.iris_recognition.service.TrainingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/training")
public class TrainingController {

    private final TrainingService trainingService;

    @Autowired
    public TrainingController(TrainingService trainingService) {
        this.trainingService = trainingService;
    }

    @PostMapping("/upload")
    public ResponseEntity<TrainingData> uploadTrainingData(
            @RequestParam("image") MultipartFile imageFile,
            @RequestParam("annotation") MultipartFile annotationFile) {
        try {
            TrainingData trainingData = trainingService.addTrainingData(imageFile, annotationFile);
            return ResponseEntity.ok(trainingData);
        } catch (IOException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping
    public ResponseEntity<List<TrainingData>> getAllTrainingData() {
        List<TrainingData> trainingData = trainingService.getAllTrainingData();
        return ResponseEntity.ok(trainingData);
    }

    @GetMapping("/unvalidated")
    public ResponseEntity<List<TrainingData>> getUnvalidatedTrainingData() {
        List<TrainingData> trainingData = trainingService.getUnvalidatedTrainingData();
        return ResponseEntity.ok(trainingData);
    }

    @PutMapping("/{id}/validate")
    public ResponseEntity<TrainingData> validateTrainingData(
            @PathVariable Long id,
            @RequestParam Boolean isValid) {
        TrainingData trainingData = trainingService.validateTrainingData(id, isValid);
        return ResponseEntity.ok(trainingData);
    }

    @PostMapping("/train")
    public ResponseEntity<String> trainModel(@RequestParam(defaultValue = "10") int epochs) {
        try {
            trainingService.trainModel(epochs);
            return ResponseEntity.ok("Training started successfully");
        } catch (IOException e) {
            return ResponseEntity.badRequest().body("Error starting training: " + e.getMessage());
        }
    }
}
