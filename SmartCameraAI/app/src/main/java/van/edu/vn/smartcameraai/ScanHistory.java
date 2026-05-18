package van.edu.vn.smartcameraai;

public class ScanHistory {
    public String objectName;
    public String confidence;
    public String imagePath;
    public long timestamp;

    public ScanHistory() {
    }

    public ScanHistory(String objectName, String confidence, String imagePath, long timestamp) {
        this.objectName = objectName;
        this.confidence = confidence;
        this.imagePath = imagePath;
        this.timestamp = timestamp;
    }
}
