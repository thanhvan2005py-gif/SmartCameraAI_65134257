package van.edu.vn.smartcameraai;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
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
import androidx.camera.core.Camera;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import org.tensorflow.lite.task.vision.detector.Detection;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
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
    private FloatingActionButton fabFlash, fabCapture;

    private Detector detector;
    private ExecutorService cameraExecutor;
    private CameraControl cameraControl;
    private boolean isFlashEnabled = false;

    // Biến cờ dùng để kiểm soát việc chụp hình (Chỉ chụp khi bằng true)
    private volatile boolean isCaptureRequested = false;
    private String lastDetectedObject = "Không xác định";
    private float lastScore = 0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        viewFinder = findViewById(R.id.viewFinder);
        overlayView = findViewById(R.id.overlayView);
        tvInferenceTime = findViewById(R.id.tvInferenceTime);
        progressBar = findViewById(R.id.scanProgressBar);
        btnBack = findViewById(R.id.btnBackScan);
        fabFlash = findViewById(R.id.fabFlash);
        fabCapture = findViewById(R.id.fabCapture);

        btnBack.setOnClickListener(v -> finish());

        fabFlash.setOnClickListener(v -> {
            if (cameraControl != null) {
                isFlashEnabled = !isFlashEnabled;
                cameraControl.enableTorch(isFlashEnabled);
            }
        });

        // HÀNH ĐỘNG CHỤP: Chỉ bật cờ yêu cầu chụp lên, không xử lý logic nặng ở đây để tránh lag giao diện
        fabCapture.setOnClickListener(v -> {
            isCaptureRequested = true;
        });

        cameraExecutor = Executors.newSingleThreadExecutor();

        if (allPermissionsGranted()) {
            setupDetector();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
    }

    private void setupDetector() {
        progressBar.setVisibility(View.VISIBLE);
        cameraExecutor.execute(() -> {
            try {
                detector = new Detector(this, "model.tflite", this);
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    startCamera();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Lỗi nạp mô hình AI", Toast.LENGTH_SHORT).show();
                });
            }
        });
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
                Camera camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
                cameraControl = camera.getCameraControl();
            } catch (Exception e) {
                Log.e(TAG, "Lỗi Camera: " + e.getMessage());
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void detectObjects(ImageProxy imageProxy) {
        if (detector == null) {
            imageProxy.close();
            return;
        }
        try {
            Bitmap bitmap = Bitmap.createBitmap(imageProxy.getWidth(), imageProxy.getHeight(), Bitmap.Config.ARGB_8888);
            bitmap.copyPixelsFromBuffer(imageProxy.getPlanes()[0].getBuffer());

            // KIỂM TRA: Nếu người dùng vừa nhấn nút chụp, tiến hành đóng gói lưu file ngay trên luồng background này
            if (isCaptureRequested) {
                isCaptureRequested = false; // Hạ cờ xuống ngay lập tức để không bị lưu lặp lại ở khung hình sau
                captureAndSave(bitmap);
            }

            detector.detect(bitmap, imageProxy.getImageInfo().getRotationDegrees());
        } catch (Exception e) {
            Log.e(TAG, "Lỗi xử lý khung hình: " + e.getMessage());
        } finally {
            imageProxy.close();
        }
    }

    @Override
    public void onDetection(List<Detection> results, long inferenceTime, int imageWidth, int imageHeight) {
        runOnUiThread(() -> {
            overlayView.setResults(results, imageWidth, imageHeight);
            tvInferenceTime.setText("AI xử lý: " + inferenceTime + "ms");
            if (!results.isEmpty()) {
                lastDetectedObject = results.get(0).getCategories().get(0).getLabel();
                lastScore = results.get(0).getCategories().get(0).getScore();
            }
        });
    }

    // Hàm nhận Bitmap trực tiếp từ luồng quét tại đúng thời điểm bấm nút
    private void captureAndSave(Bitmap bitmapToSave) {
        if (bitmapToSave == null) return;

        String fileName = "scan_" + System.currentTimeMillis() + ".jpg";
        File file = new File(getFilesDir(), fileName);

        try (FileOutputStream out = new FileOutputStream(file)) {
            bitmapToSave.compress(Bitmap.CompressFormat.JPEG, 90, out);

            // Tiến hành đẩy thông tin lên Firebase Realtime
            saveToFirebase(file.getAbsolutePath());

            // Hiển thị thông báo lên màn hình chính
            runOnUiThread(() -> Toast.makeText(ScanActivity.this, "Đã lưu " + lastDetectedObject + " vào lịch sử quét", Toast.LENGTH_SHORT).show());
        } catch (Exception e) {
            Log.e(TAG, "Lỗi lưu ảnh: " + e.getMessage());
            runOnUiThread(() -> Toast.makeText(ScanActivity.this, "Lỗi khi chụp hình", Toast.LENGTH_SHORT).show());
        }
    }

    private void saveToFirebase(String imagePath) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid != null) {
            HashMap<String, Object> history = new HashMap<>();
            history.put("objectName", lastDetectedObject);
            history.put("confidence", Math.round(lastScore * 100) + "%");
            history.put("imagePath", imagePath);
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
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }
}