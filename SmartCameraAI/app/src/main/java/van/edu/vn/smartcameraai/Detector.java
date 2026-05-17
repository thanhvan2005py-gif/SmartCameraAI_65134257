package van.edu.vn.smartcameraai;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.SystemClock;
import android.util.Log;

import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.Rot90Op;
import org.tensorflow.lite.task.core.BaseOptions;
import org.tensorflow.lite.task.vision.detector.Detection;
import org.tensorflow.lite.task.vision.detector.ObjectDetector;

import java.io.IOException;
import java.util.List;

public class Detector {

    private final Context context;
    private final String modelPath;
    private final DetectorListener listener;
    private ObjectDetector objectDetector;

    public interface DetectorListener {
        void onDetection(List<Detection> results, long inferenceTime, int imageWidth, int imageHeight);
    }

    public Detector(Context context, String modelPath, DetectorListener listener) {
        this.context = context;
        this.modelPath = modelPath;
        this.listener = listener;
        setupObjectDetector();
    }

    private void setupObjectDetector() {
        ObjectDetector.ObjectDetectorOptions.Builder optionsBuilder =
                ObjectDetector.ObjectDetectorOptions.builder()
                        .setScoreThreshold(0.5f)
                        .setMaxResults(3);

        BaseOptions.Builder baseOptionsBuilder = BaseOptions.builder().setNumThreads(2);
        optionsBuilder.setBaseOptions(baseOptionsBuilder.build());

        try {
            objectDetector = ObjectDetector.createFromFileAndOptions(context, modelPath, optionsBuilder.build());
        } catch (IOException e) {
            Log.e("Detector", "TFLite failed to load model with error: " + e.getMessage());
        }
    }

    public void detect(Bitmap bitmap, int imageRotation) {
        if (objectDetector == null) {
            setupObjectDetector();
        }

        long inferenceTime = SystemClock.uptimeMillis();

        // Create pre-processor for the image.
        // See config: https://github.com/tensorflow/examples/blob/master/lite/examples/object_detection/android/lib_support/src/main/java/org/tensorflow/lite/examples/detection/tflite/TFLiteObjectDetectionAPIModel.java
        ImageProcessor imageProcessor = new ImageProcessor.Builder()
                .add(new Rot90Op(-imageRotation / 90))
                .build();

        // Preprocess the image and convert it into a TensorImage for detection.
        TensorImage tensorImage = imageProcessor.process(TensorImage.fromBitmap(bitmap));

        List<Detection> results = objectDetector.detect(tensorImage);
        inferenceTime = SystemClock.uptimeMillis() - inferenceTime;

        listener.onDetection(results, inferenceTime, tensorImage.getWidth(), tensorImage.getHeight());
    }

    public void close() {
        if (objectDetector != null) {
            objectDetector.close();
        }
    }
}
