package van.edu.vn.smartcameraai;

import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import org.tensorflow.lite.task.vision.detector.Detection;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

public class StaticScanActivity extends AppCompatActivity implements Detector.DetectorListener {

    private static final String TAG = "StaticScanActivity";

    private ImageView imgView;
    private OverlayView overlayView;
    private TextView tvResultName, tvInferenceTime;
    private ProgressBar progressBar;
    private MaterialButton btnSaveHistory;
    private ImageButton btnBack;

    private Detector detector;
    private Bitmap currentBitmap;
    private String lastLabel = "Không xác định";
    private float lastScore = 0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_static_scan);

        imgView = findViewById(R.id.imgStaticView);
        overlayView = findViewById(R.id.staticOverlay);
        tvResultName = findViewById(R.id.tvStaticResultName);
        tvInferenceTime = findViewById(R.id.tvStaticInferenceTime);
        progressBar = findViewById(R.id.staticLoadProgress);
        btnSaveHistory = findViewById(R.id.btnSaveToHistoryStatic);
        btnBack = findViewById(R.id.btnBackFromStatic);

        btnBack.setOnClickListener(v -> finish());

        // Nhận URI ảnh từ Intent
        String uriString = getIntent().getStringExtra("image_uri");
        if (uriString != null) {
            Uri imageUri = Uri.parse(uriString);
            loadImage(imageUri);
        } else {
            Toast.makeText(this, "Không tìm thấy ảnh", Toast.LENGTH_SHORT).show();
            finish();
        }

        btnSaveHistory.setOnClickListener(v -> saveToHistory());
    }

    private void loadImage(Uri uri) {
        progressBar.setVisibility(View.VISIBLE);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                currentBitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(getContentResolver(), uri), (decoder, info, source) -> {
                    decoder.setMutableRequired(true);
                });
            } else {
                currentBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
            }

            // Chuyển sang Bitmap chuẩn ARGB_8888
            currentBitmap = currentBitmap.copy(Bitmap.Config.ARGB_8888, true);
            imgView.setImageBitmap(currentBitmap);

            // Khởi tạo detector và quét
            detector = new Detector(this, "model.tflite", this);
            detector.detect(currentBitmap, 0);

        } catch (IOException e) {
            Log.e(TAG, "Lỗi nạp ảnh: " + e.getMessage());
            Toast.makeText(this, "Lỗi hiển thị ảnh", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    public void onDetection(List<Detection> results, long inferenceTime, int imageWidth, int imageHeight) {
        runOnUiThread(() -> {
            progressBar.setVisibility(View.GONE);
            overlayView.setResults(results, imageWidth, imageHeight);
            tvInferenceTime.setText("AI phân tích trong: " + inferenceTime + "ms");

            if (results != null && !results.isEmpty()) {
                lastLabel = results.get(0).getCategories().get(0).getLabel();
                lastScore = results.get(0).getCategories().get(0).getScore();
                tvResultName.setText("Kết quả: " + lastLabel + " (" + Math.round(lastScore * 100) + "%)");
                btnSaveHistory.setVisibility(View.VISIBLE);
            } else {
                tvResultName.setText("Không nhận diện được vật thể");
                btnSaveHistory.setVisibility(View.GONE);
            }
        });
    }

    private void saveToHistory() {
        if (currentBitmap == null) return;

        progressBar.setVisibility(View.VISIBLE);
        String fileName = "static_scan_" + System.currentTimeMillis() + ".jpg";
        File file = new File(getFilesDir(), fileName);

        try (FileOutputStream out = new FileOutputStream(file)) {
            currentBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            
            String uid = FirebaseAuth.getInstance().getUid();
            if (uid != null) {
                HashMap<String, Object> history = new HashMap<>();
                history.put("objectName", lastLabel);
                history.put("confidence", Math.round(lastScore * 100) + "%");
                history.put("imagePath", file.getAbsolutePath());
                history.put("timestamp", System.currentTimeMillis());

                FirebaseDatabase.getInstance().getReference("scan_history")
                        .child(uid).push().setValue(history)
                        .addOnCompleteListener(task -> {
                            progressBar.setVisibility(View.GONE);
                            if (task.isSuccessful()) {
                                Toast.makeText(this, "Đã lưu vào lịch sử", Toast.LENGTH_SHORT).show();
                                finish();
                            } else {
                                Toast.makeText(this, "Lỗi lưu Firebase", Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        } catch (IOException e) {
            progressBar.setVisibility(View.GONE);
            Log.e(TAG, "Lỗi lưu file: " + e.getMessage());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (detector != null) detector.close();
    }
}
