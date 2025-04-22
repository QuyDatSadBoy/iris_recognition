package com.example.iris_recognition.repository;

import com.example.iris_recognition.model.TrainingData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TrainingDataRepository extends JpaRepository<TrainingData, Long> {
    List<TrainingData> findByStatus(String status);
    List<TrainingData> findByIsValidated(boolean isValidated);
}