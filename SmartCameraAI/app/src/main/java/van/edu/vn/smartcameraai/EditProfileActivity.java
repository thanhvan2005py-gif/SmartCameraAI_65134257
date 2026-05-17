package van.edu.vn.smartcameraai;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class EditProfileActivity extends AppCompatActivity {

    private TextInputEditText edtName, edtEmail;
    private MaterialButton btnUpdate;
    private ImageButton btnBack;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private String currentUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        // 1. Khởi tạo Firebase
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        FirebaseUser user = mAuth.getCurrentUser();

        if (user == null) {
            finish();
            return;
        }
        currentUid = user.getUid();

        // 2. Ánh xạ View
        edtName = findViewById(R.id.edtEditName);
        edtEmail = findViewById(R.id.edtEditEmail);
        btnUpdate = findViewById(R.id.btnUpdateProfile);
        btnBack = findViewById(R.id.btnBack);
        progressBar = findViewById(R.id.editProgressBar);

        // 3. Tải dữ liệu hiện tại
        loadCurrentData();

        // 4. Xử lý sự kiện
        btnBack.setOnClickListener(v -> finish());

        btnUpdate.setOnClickListener(v -> updateProfile());
    }

    private void loadCurrentData() {
        progressBar.setVisibility(View.VISIBLE);
        mDatabase.child("users").child(currentUid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                progressBar.setVisibility(View.GONE);
                if (snapshot.exists()) {
                    User userModel = snapshot.getValue(User.class);
                    if (userModel != null) {
                        edtName.setText(userModel.name);
                        edtEmail.setText(userModel.email);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(EditProfileActivity.this, "Lỗi tải dữ liệu", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateProfile() {
        String newName = edtName.getText().toString().trim();

        if (TextUtils.isEmpty(newName)) {
            edtName.setError("Vui lòng nhập họ tên");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnUpdate.setEnabled(false);

        // Cập nhật lên Firebase Realtime Database
        mDatabase.child("users").child(currentUid).child("name").setValue(newName)
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    btnUpdate.setEnabled(true);
                    if (task.isSuccessful()) {
                        Toast.makeText(EditProfileActivity.this, "Cập nhật hồ sơ thành công", Toast.LENGTH_SHORT).show();
                        finish(); // Quay lại Fragment
                    } else {
                        Toast.makeText(EditProfileActivity.this, "Cập nhật thất bại", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
