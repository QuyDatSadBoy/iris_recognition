package com.example.iris_recognition.model;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "training_data")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrainingData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String imagePath;
    private String annotationPath;
    private boolean isValidated;
    private LocalDateTime uploadedAt;
    private String status; // NEW, PROCESSED, TRAINING, TRAINED

    public TrainingData(String imagePath, String annotationPath) {
        this.imagePath = imagePath;
        this.annotationPath = annotationPath;
        this.isValidated = false;
        this.uploadedAt = LocalDateTime.now();
        this.status = "NEW";
    }
}