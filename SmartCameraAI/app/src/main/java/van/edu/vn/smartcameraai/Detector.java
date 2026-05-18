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
        // Cấu hình các thông số cơ bản cho bộ nhận diện
        ObjectDetector.ObjectDetectorOptions.Builder optionsBuilder =
                ObjectDetector.ObjectDetectorOptions.builder()
                        .setScoreThreshold(0.4f)
                        .setMaxResults(3);

        // Ép chạy bằng CPU với 4 luồng xử lý để đảm bảo độ ổn định cao nhất, không dùng GPU
        BaseOptions.Builder baseOptionsBuilder = BaseOptions.builder().setNumThreads(4);
        optionsBuilder.setBaseOptions(baseOptionsBuilder.build());

        try {
            // Tiến hành khởi tạo mô hình AI
            objectDetector = ObjectDetector.createFromFileAndOptions(context, modelPath, optionsBuilder.build());
            Log.d("Detector", "Khởi tạo ObjectDetector bằng CPU thành công!");
        } catch (Exception e) {
            Log.e("Detector", "Không thể nạp mô hình AI. Lỗi: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void detect(Bitmap bitmap, int imageRotation) {
        // Nếu chưa khởi tạo thành công, thử gọi lại hàm setup
        if (objectDetector == null) {
            setupObjectDetector();
        }

        // Kiểm tra an toàn: Nếu hệ thống JNI vẫn lỗi (do lỗi 16 KB), bỏ qua lượt quét này để tránh sập app
        if (objectDetector == null) {
            Log.e("Detector", "Bộ quét AI chưa sẵn sàng do lỗi nạp thư viện hệ thống.");
            return;
        }

        long inferenceTime = SystemClock.uptimeMillis();

        // Tiền xử lý xoay ảnh từ Camera tương thích với Model AI
        ImageProcessor imageProcessor = new ImageProcessor.Builder()
                .add(new Rot90Op(-imageRotation / 90))
                .build();

        // Chuyển đổi dữ liệu Bitmap sang TensorImage
        TensorImage tensorImage = imageProcessor.process(TensorImage.fromBitmap(bitmap));

        try {
            // Thực hiện nhận diện vật thể
            List<Detection> results = objectDetector.detect(tensorImage);
            inferenceTime = SystemClock.uptimeMillis() - inferenceTime;

            // Trả kết quả về cho Giao diện xử lý thông qua Listener
            if (listener != null) {
                listener.onDetection(results, inferenceTime, tensorImage.getWidth(), tensorImage.getHeight());
            }
        } catch (Exception e) {
            Log.e("Detector", "Lỗi xảy ra trong quá trình nhận diện hình ảnh: " + e.getMessage());
        }
    }

    public void close() {
        if (objectDetector != null) {
            try {
                objectDetector.close();
            } catch (Exception e) {
                Log.e("Detector", "Lỗi khi đóng ObjectDetector: " + e.getMessage());
            }
        }
    }
}