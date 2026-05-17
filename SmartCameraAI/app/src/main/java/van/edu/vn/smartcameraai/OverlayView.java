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
    private Paint textPaint = new Paint();
    private float scaleFactor = 1f;
    private int imageWidth = 1;
    private int imageHeight = 1;

    public OverlayView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initPaints();
    }

    private void initPaints() {
        boxPaint.setColor(Color.parseColor("#FF6D00"));
        boxPaint.setStrokeWidth(8f);
        boxPaint.setStyle(Paint.Style.STROKE);

        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(44f);
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setFakeBoldText(true);
    }

    public void setResults(List<Detection> results, int imageWidth, int imageHeight) {
        this.results = results;
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
        
        // Tính toán tỉ lệ giữa ảnh và view
        scaleFactor = Math.max(getWidth() * 1f / imageWidth, getHeight() * 1f / imageHeight);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        for (Detection detection : results) {
            RectF box = detection.getBoundingBox();

            float left = box.left * scaleFactor;
            float top = box.top * scaleFactor;
            float right = box.right * scaleFactor;
            float bottom = box.bottom * scaleFactor;

            // Vẽ khung bao quanh vật thể
            canvas.drawRect(left, top, right, bottom, boxPaint);

            // Vẽ nhãn và độ tin cậy
            String label = detection.getCategories().get(0).getLabel() + " " +
                    Math.round(detection.getCategories().get(0).getScore() * 100) + "%";
            
            canvas.drawText(label, left, top - 10, textPaint);
        }
    }
}
