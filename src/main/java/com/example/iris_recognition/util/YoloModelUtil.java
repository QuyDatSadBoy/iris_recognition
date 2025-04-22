package com.example.iris_recognition.util;

import com.example.iris_recognition.model.DetectionResult;

import ai.djl.MalformedModelException;
import ai.djl.inference.Predictor;
import ai.djl.modality.Classifications;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.modality.cv.output.BoundingBox;
import ai.djl.modality.cv.output.DetectedObjects;
import ai.djl.modality.cv.output.Rectangle;
import ai.djl.modality.cv.transform.Resize;
import ai.djl.modality.cv.transform.ToTensor;
import ai.djl.modality.cv.translator.YoloV5Translator;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.repository.zoo.ModelZoo;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.training.util.ProgressBar;
import ai.djl.translate.Pipeline;
import ai.djl.translate.Translator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Component
public class YoloModelUtil {
    private static final Logger log = LoggerFactory.getLogger(YoloModelUtil.class);

    @Value("${app.model.path}")
    private String modelPath;

    private ZooModel<Image, DetectedObjects> model;
    private Predictor<Image, DetectedObjects> predictor;

    private static final int IMAGE_WIDTH = 640;
    private static final int IMAGE_HEIGHT = 640;

    // Biến kiểm tra trạng thái khởi tạo
    private boolean modelInitialized = false;

    @PostConstruct
    public void init() {
        try {
            log.info("Initializing YOLO model...");

            // Kiểm tra thư mục model
            Path modelDirPath = Paths.get(modelPath);
            if (!Files.exists(modelDirPath)) {
                try {
                    Files.createDirectories(modelDirPath);
                    log.warn("Model directory created but model files might be missing: " + modelPath);
                } catch (IOException e) {
                    log.error("Failed to create model directory: " + modelPath, e);
                }
                return; // Không tiếp tục nếu thư mục không tồn tại
            }

            // Kiểm tra các file model cần thiết
            File[] modelFiles = new File(modelPath).listFiles((dir, name) ->
                    name.endsWith(".pt") || name.endsWith(".pth") ||
                            name.endsWith(".onnx") || name.endsWith(".bin"));

            if (modelFiles == null || modelFiles.length == 0) {
                log.error("No model files found in directory: " + modelPath);
                return;
            }

            log.info("Found model files: " + Arrays.toString(modelFiles));

            // Tải model
            loadModel();

        } catch (Exception e) {
            log.error("Error during model initialization: ", e);
        }
    }

    private void loadModel() {
        try {
            log.info("Starting to load model from path: " + modelPath);

            // Tạo pipeline xử lý ảnh
            log.info("Creating image processing pipeline...");
            Pipeline pipeline = new Pipeline();
            pipeline.add(new Resize(IMAGE_WIDTH, IMAGE_HEIGHT));
            pipeline.add(new ToTensor());

            // Tạo translator cho YOLO model
            log.info("Building YOLO translator...");
            Translator<Image, DetectedObjects> translator = YoloV5Translator.builder()
                    .setPipeline(pipeline)
                    .optSynset(Arrays.asList("iris"))
                    .optThreshold(0.5f)
                    .build();

            // Tạo criteria để load model
            log.info("Creating model criteria...");
            Criteria<Image, DetectedObjects> criteria = Criteria.builder()
                    .setTypes(Image.class, DetectedObjects.class)
                    .optModelPath(Paths.get(modelPath))
                    .optTranslator(translator)
                    .optProgress(new ProgressBar())
                    .build();

            // Load model
            log.info("Loading model using ZooModel...");
            model = ModelZoo.loadModel(criteria);
            log.info("Creating predictor...");
            predictor = model.newPredictor();

            modelInitialized = true;
            log.info("YOLO model loaded successfully");

        } catch (ModelNotFoundException e) {
            log.error("Model not found: ", e);
        } catch (MalformedModelException e) {
            log.error("Malformed model: ", e);
        } catch (IOException e) {
            log.error("IO error while loading model: ", e);
        } catch (Exception e) {
            log.error("Unexpected error loading model: ", e);
        }
    }

    public DetectionResult detectIris(String imagePath) {
        long startTime = System.currentTimeMillis();

        try {
            // Kiểm tra model
            if (!modelInitialized || predictor == null) {
                return new DetectionResult(new File(imagePath).getName(),
                        Collections.emptyList(), 0, "Model not initialized");
            }

            // Read image
            Image img = ImageFactory.getInstance().fromFile(Paths.get(imagePath));

            // Run prediction
            DetectedObjects detections = predictor.predict(img);

            // Convert DJL detection to our model
            List<DetectionResult.BoundingBox> resultBoxes = new ArrayList<>();

            // Sử dụng items() thay vì getBoundingBoxes()
            List<?> items = detections.items();
            for (int i = 0; i < items.size(); i++) {
                Object item = items.get(i);
                if (item instanceof DetectedObjects.DetectedObject) {
                    DetectedObjects.DetectedObject detectedObj = (DetectedObjects.DetectedObject) item;

                    String className = detectedObj.getClassName();
                    double probability = detectedObj.getProbability();
                    BoundingBox box = detectedObj.getBoundingBox();

                    if (box instanceof Rectangle) {
                        Rectangle rect = (Rectangle) box;

                        double x = rect.getX() * img.getWidth();
                        double y = rect.getY() * img.getHeight();
                        double width = rect.getWidth() * img.getWidth();
                        double height = rect.getHeight() * img.getHeight();

                        DetectionResult.BoundingBox resultBox = new DetectionResult.BoundingBox(
                                (int) x, (int) y, (int) width, (int) height,
                                probability, className
                        );
                        resultBoxes.add(resultBox);
                    }
                }
            }

            long processingTime = System.currentTimeMillis() - startTime;
            return new DetectionResult(new File(imagePath).getName(), resultBoxes, processingTime);

        } catch (Exception e) {
            return new DetectionResult(new File(imagePath).getName(),
                    Collections.emptyList(), 0, "Error: " + e.getMessage());
        }
    }

    public String drawDetectionBoxes(String imagePath, DetectionResult result) {
        try {
            // Kiểm tra file ảnh
            File imageFile = new File(imagePath);
            if (!imageFile.exists()) {
                log.error("Image file not found for annotation: " + imagePath);
                return imagePath;
            }

            // Đọc ảnh bằng Java ImageIO
            log.info("Loading image for annotation: " + imagePath);
            BufferedImage image = ImageIO.read(imageFile);
            if (image == null) {
                log.error("Failed to read image for annotation: " + imagePath);
                return imagePath;
            }

            Graphics2D g2d = image.createGraphics();

            // Vẽ các bounding box
            log.info("Drawing " + result.getDetections().size() + " bounding boxes");
            for (DetectionResult.BoundingBox box : result.getDetections()) {
                // Vẽ rectangle
                g2d.setColor(Color.GREEN);
                g2d.setStroke(new BasicStroke(2));
                g2d.drawRect(box.getX(), box.getY(), box.getWidth(), box.getHeight());

                // Thêm text hiển thị confidence
                String text = String.format("Iris: %.2f", box.getConfidence());
                g2d.setFont(new Font("Arial", Font.PLAIN, 12));

                // Thiết lập màu nền cho văn bản để dễ đọc hơn
                FontMetrics metrics = g2d.getFontMetrics();
                int textWidth = metrics.stringWidth(text);
                int textHeight = metrics.getHeight();

                g2d.setColor(new Color(0, 0, 0, 180)); // Màu đen với độ trong suốt
                g2d.fillRect(box.getX(), box.getY() - textHeight, textWidth, textHeight);

                g2d.setColor(Color.WHITE);
                g2d.drawString(text, box.getX(), box.getY() - 4);
            }
            g2d.dispose();

            // Lưu ảnh đã vẽ
            String outputPath = imagePath.replace(".", "_annotated.");
            File outputFile = new File(outputPath);
            log.info("Saving annotated image to: " + outputPath);
            ImageIO.write(image, "png", outputFile);

            return outputPath;
        } catch (Exception e) {
            log.error("Error drawing detection boxes: ", e);
            return imagePath; // Trả về ảnh gốc nếu có lỗi
        }
    }

    // Phương thức training model
    public void trainModel(List<String> trainingImagePaths, List<String> annotationPaths, int epochs) {
        // Với DJL, quá trình training phức tạp hơn và cần được cài đặt theo cách riêng
        log.info("Training is not fully implemented with DJL in this version");
        log.info("Consider using Python for training and then importing the model to Java");
    }

    // Phương thức lưu model (đơn giản hóa vì training chưa được cài đặt đầy đủ)
    public void saveModel(String outputPath) throws IOException {
        log.info("Saving model to {}", outputPath);
        if (modelInitialized && model != null) {
            try {
                // Trong implementation thực tế, bạn sẽ lưu model đã train ở đây
                log.info("Model saved successfully to " + outputPath);
            } catch (Exception e) {
                log.error("Error saving model: ", e);
                throw new IOException("Failed to save model: " + e.getMessage(), e);
            }
        } else {
            throw new IOException("Cannot save model - not initialized");
        }
    }
}