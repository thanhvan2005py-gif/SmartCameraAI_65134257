package van.edu.vn.smartcameraai;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.task.vision.detector.Detection;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ScanActivity extends AppCompatActivity implements Detector.DetectorListener {

    private static final String TAG = "ScanActivity";
    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private static final String[] REQUIRED_PERMISSIONS = new String[]{Manifest.permission.CAMERA};

    private PreviewView viewFinder;
    private OverlayView overlayView;
    private TextView tvInferenceTime;
    private ProgressBar progressBar;
    private ImageButton btnBack;

    private Detector detector;
    private ExecutorService cameraExecutor;
    private long lastSaveTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        viewFinder = findViewById(R.id.viewFinder);
        overlayView = findViewById(R.id.overlayView);
        tvInferenceTime = findViewById(R.id.tvInferenceTime);
        progressBar = findViewById(R.id.scanProgressBar);
        btnBack = findViewById(R.id.btnBackScan);

        btnBack.setOnClickListener(v -> finish());

        cameraExecutor = Executors.newSingleThreadExecutor();

        if (allPermissionsGranted()) {
            setupDetector();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
    }

    private void setupDetector() {
        progressBar.setVisibility(View.VISIBLE);
        detector = new Detector(this, "model.tflite", this);
        progressBar.setVisibility(View.GONE);
        startCamera();
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(viewFinder.getSurfaceProvider());

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                        .build();

                imageAnalysis.setAnalyzer(cameraExecutor, image -> {
                    detectObjects(image);
                });

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);

            } catch (Exception e) {
                Log.e(TAG, "Camera setup failed", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void detectObjects(ImageProxy imageProxy) {
        if (detector == null) {
            imageProxy.close();
            return;
        }

        Bitmap bitmap = Bitmap.createBitmap(imageProxy.getWidth(), imageProxy.getHeight(), Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(imageProxy.getPlanes()[0].getBuffer());

        detector.detect(bitmap, imageProxy.getImageInfo().getRotationDegrees());
        imageProxy.close();
    }

    @Override
    public void onDetection(List<Detection> results, long inferenceTime, int imageWidth, int imageHeight) {
        runOnUiThread(() -> {
            overlayView.setResults(results, imageWidth, imageHeight);
            tvInferenceTime.setText("Inference time: " + inferenceTime + "ms");
            
            if (!results.isEmpty()) {
                checkAndSaveHistory(results.get(0));
            }
        });
    }

    private void checkAndSaveHistory(Detection detection) {
        // Chỉ lưu lịch sử mỗi 5 giây 1 lần để tránh spam database
        if (System.currentTimeMillis() - lastSaveTime < 5000) return;

        String label = detection.getCategories().get(0).getLabel();
        float score = detection.getCategories().get(0).getScore();
        String uid = FirebaseAuth.getInstance().getUid();

        if (uid != null && score > 0.7) { // Độ tin cậy trên 70%
            lastSaveTime = System.currentTimeMillis();
            HashMap<String, Object> history = new HashMap<>();
            history.put("objectName", label);
            history.put("confidence", Math.round(score * 100) + "%");
            history.put("timestamp", System.currentTimeMillis());

            FirebaseDatabase.getInstance().getReference("scan_history")
                    .child(uid)
                    .push()
                    .setValue(history);
        }
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                setupDetector();
            } else {
                Toast.makeText(this, "Bạn cần cấp quyền camera", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }
}
