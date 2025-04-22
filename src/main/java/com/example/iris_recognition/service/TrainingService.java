package com.example.iris_recognition.service;

import com.example.iris_recognition.model.TrainingData;
import com.example.iris_recognition.repository.TrainingDataRepository;
import com.example.iris_recognition.util.YoloModelUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TrainingService {

    private final TrainingDataRepository trainingDataRepository;
    private final StorageService storageService;
    private final YoloModelUtil yoloModelUtil;

    @Value("${app.model.path}")
    private String modelPath;

    @Autowired
    public TrainingService(
            TrainingDataRepository trainingDataRepository,
            StorageService storageService,
            YoloModelUtil yoloModelUtil) {
        this.trainingDataRepository = trainingDataRepository;
        this.storageService = storageService;
        this.yoloModelUtil = yoloModelUtil;
    }

    public TrainingData addTrainingData(MultipartFile imageFile, MultipartFile annotationFile) throws IOException {
        // Store files
        String imagePath = storageService.storeTrainingImage(imageFile);
        String annotationPath = storageService.storeAnnotation(annotationFile);

        // Save training data record
        TrainingData trainingData = new TrainingData(imagePath, annotationPath);
        return trainingDataRepository.save(trainingData);
    }

    public List<TrainingData> getAllTrainingData() {
        return trainingDataRepository.findAll();
    }

    public List<TrainingData> getUnvalidatedTrainingData() {
        return trainingDataRepository.findByIsValidated(false);
    }

    public TrainingData validateTrainingData(Long id, boolean isValid) {
        TrainingData trainingData = trainingDataRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Training data not found with id: " + id));

        trainingData.setValidated(isValid);
        trainingData.setStatus(isValid ? "PROCESSED" : "INVALID");

        return trainingDataRepository.save(trainingData);
    }

    public void trainModel(int epochs) throws IOException {
        // Get all validated training data
        List<TrainingData> validatedData = trainingDataRepository.findByStatus("PROCESSED");

        // Update status to TRAINING
        validatedData.forEach(data -> {
            data.setStatus("TRAINING");
            trainingDataRepository.save(data);
        });

        // Get paths for training
        List<String> imagePaths = validatedData.stream()
                .map(TrainingData::getImagePath)
                .collect(Collectors.toList());

        List<String> annotationPaths = validatedData.stream()
                .map(TrainingData::getAnnotationPath)
                .collect(Collectors.toList());

        // Train model
        yoloModelUtil.trainModel(imagePaths, annotationPaths, epochs);

        // Save the trained model with timestamp
        String trainedModelPath = modelPath + "trained_" + LocalDateTime.now().toString().replace(":", "-") + ".h5";
        yoloModelUtil.saveModel(trainedModelPath);

        // Update status to TRAINED
        validatedData.forEach(data -> {
            data.setStatus("TRAINED");
            trainingDataRepository.save(data);
        });
    }
}
