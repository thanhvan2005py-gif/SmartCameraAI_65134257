package van.edu.vn.smartcameraai;

import com.google.firebase.database.Exclude;

public class ScanHistory {
    public String objectName;
    public String confidence;
    public String imagePath;
    public long timestamp;
    
    @Exclude
    public String key; // Khóa để xác định mục trong Firebase

    public ScanHistory() {
    }

    public ScanHistory(String objectName, String confidence, String imagePath, long timestamp) {
        this.objectName = objectName;
        this.confidence = confidence;
        this.imagePath = imagePath;
        this.timestamp = timestamp;
    }
}
