package van.edu.vn.smartcameraai;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private List<ScanHistory> historyList;

    public HistoryAdapter(List<ScanHistory> historyList) {
        this.historyList = historyList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ScanHistory history = historyList.get(position);
        holder.tvName.setText(history.objectName);
        holder.tvConfidence.setText("Độ tin cậy: " + history.confidence);
        
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        holder.tvDate.setText(sdf.format(new Date(history.timestamp)));

        if (history.imagePath != null) {
            File imgFile = new File(history.imagePath);
            if (imgFile.exists()) {
                Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                holder.imgView.setImageBitmap(myBitmap);
            }
        }
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgView;
        TextView tvName, tvConfidence, tvDate;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgView = itemView.findViewById(R.id.imgHistory);
            tvName = itemView.findViewById(R.id.tvHistoryName);
            tvConfidence = itemView.findViewById(R.id.tvHistoryConfidence);
            tvDate = itemView.findViewById(R.id.tvHistoryDate);
        }
    }
}
