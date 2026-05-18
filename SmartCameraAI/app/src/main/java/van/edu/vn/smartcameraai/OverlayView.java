package van.edu.vn.smartcameraai;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import org.tensorflow.lite.task.vision.detector.Detection;

import java.util.ArrayList;
import java.util.List;

public class OverlayView extends View {

    private List<Detection> results = new ArrayList<>();
    private Paint boxPaint = new Paint();
    private Paint textBackgroundPaint = new Paint(); // Bổ sung cọ vẽ nền chữ
    private Paint textPaint = new Paint();

    private float scaleFactor = 1f;
    // Bổ sung bù trừ tọa độ phòng trường hợp ảnh bị cắt (crop) viền
    private float offsetX = 0f;
    private float offsetY = 0f;

    public OverlayView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initPaints();
    }

    private void initPaints() {
        // Cọ vẽ khung chữ nhật
        boxPaint.setColor(Color.parseColor("#FF6D00"));
        boxPaint.setStrokeWidth(8f);
        boxPaint.setStyle(Paint.Style.STROKE);

        // Cọ vẽ nền chữ (cùng màu cam với khung)
        textBackgroundPaint.setColor(Color.parseColor("#FF6D00"));
        textBackgroundPaint.setStyle(Paint.Style.FILL);

        // Cọ vẽ chữ
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(44f);
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setFakeBoldText(true);
    }

    public void setResults(List<Detection> results, int imageWidth, int imageHeight) {
        // 1. Tạo bản sao của list để tránh xung đột đa luồng
        this.results = new ArrayList<>(results);

        // 2. Tính toán tỷ lệ (Scale)
        scaleFactor = Math.max(getWidth() * 1f / imageWidth, getHeight() * 1f / imageHeight);

        // 3. Tính toán độ lệch (Offset) để đưa khung về đúng tâm nếu ảnh bị crop
        float scaledWidth = imageWidth * scaleFactor;
        float scaledHeight = imageHeight * scaleFactor;

        offsetX = (getWidth() - scaledWidth) / 2f;
        offsetY = (getHeight() - scaledHeight) / 2f;

        // Yêu cầu vẽ lại view
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        for (Detection detection : results) {
            RectF box = detection.getBoundingBox();

            // Áp dụng scale và cộng thêm độ lệch (offset)
            float left = box.left * scaleFactor + offsetX;
            float top = box.top * scaleFactor + offsetY;
            float right = box.right * scaleFactor + offsetX;
            float bottom = box.bottom * scaleFactor + offsetY;

            // Vẽ khung bao quanh vật thể
            canvas.drawRect(left, top, right, bottom, boxPaint);

            // Xử lý nội dung nhãn
            String label = detection.getCategories().get(0).getLabel() + " " +
                    Math.round(detection.getCategories().get(0).getScore() * 100) + "%";

            // Vẽ nền chữ
            float textWidth = textPaint.measureText(label);
            float textHeight = textPaint.getTextSize();
            canvas.drawRect(
                    left,
                    top - textHeight - 10,
                    left + textWidth + 10,
                    top,
                    textBackgroundPaint
            );

            // Vẽ nhãn và độ tin cậy đè lên nền
            canvas.drawText(label, left + 5, top - 10, textPaint);
        }
    }
}