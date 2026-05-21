package van.edu.vn.smartcameraai;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HistoryActivity extends AppCompatActivity implements HistoryAdapter.OnHistoryItemClickListener {

    private RecyclerView rvHistory;
    private HistoryAdapter adapter;
    private List<ScanHistory> historyList;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private ImageButton btnBack;

    private DatabaseReference mDatabase;
    private String currentUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        rvHistory = findViewById(R.id.rvHistory);
        progressBar = findViewById(R.id.historyProgressBar);
        tvEmpty = findViewById(R.id.tvEmptyHistory);
        btnBack = findViewById(R.id.btnBackHistory);

        btnBack.setOnClickListener(v -> finish());

        rvHistory.setLayoutManager(new LinearLayoutManager(this));
        historyList = new ArrayList<>();
        adapter = new HistoryAdapter(historyList, this);
        rvHistory.setAdapter(adapter);

        currentUid = FirebaseAuth.getInstance().getUid();
        if (currentUid != null) {
            mDatabase = FirebaseDatabase.getInstance().getReference("scan_history").child(currentUid);
            loadHistory();
        } else {
            finish();
        }
    }

    private void loadHistory() {
        progressBar.setVisibility(View.VISIBLE);
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                progressBar.setVisibility(View.GONE);
                historyList.clear();
                if (snapshot.exists()) {
                    for (DataSnapshot data : snapshot.getChildren()) {
                        ScanHistory history = data.getValue(ScanHistory.class);
                        if (history != null) {
                            history.key = data.getKey(); // Lưu lại key để xóa
                            historyList.add(history);
                        }
                    }
                    Collections.reverse(historyList);
                    adapter.notifyDataSetChanged();
                    tvEmpty.setVisibility(View.GONE);
                } else {
                    adapter.notifyDataSetChanged();
                    tvEmpty.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(HistoryActivity.this, "Lỗi: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDeleteClick(ScanHistory history) {
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận xóa")
                .setMessage("Bạn có chắc muốn xóa mục này không?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    if (history.key != null) {
                        mDatabase.child(history.key).removeValue().addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(HistoryActivity.this, "Đã xóa lịch sử", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(HistoryActivity.this, "Xóa thất bại", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
}
