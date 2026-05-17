package van.edu.vn.smartcameraai;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class CaNhanFragment extends Fragment {

    private TextView tvUserEmail, tvUserName;
    private View btnLogout, btnChangePassword, btnEditProfile, btnScanHistory;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    public CaNhanFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_ca_nhan, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Khởi tạo Firebase
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        // 2. Ánh xạ View từ fragment_ca_nhan.xml
        tvUserEmail = view.findViewById(R.id.tvUserEmail);
        tvUserName = view.findViewById(R.id.tvUserName);
        btnLogout = view.findViewById(R.id.btnLogout);
        btnChangePassword = view.findViewById(R.id.btnChangePassword);
        btnEditProfile = view.findViewById(R.id.btnEditProfile);
        btnScanHistory = view.findViewById(R.id.btnScanHistory);

        // 3. Kiểm tra trạng thái đăng nhập và hiển thị thông tin
        if (currentUser != null) {
            loadUserData(currentUser.getUid());
        } else {
            // Nếu chưa đăng nhập, chuyển hướng về màn hình Login
            redirectToLogin();
        }

        // 4. Xử lý sự kiện Đăng xuất
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                mAuth.signOut();
                Toast.makeText(getContext(), "Đã đăng xuất thành công", Toast.LENGTH_SHORT).show();
                if (getActivity() != null) {
                    Intent intent = new Intent(getActivity(), Login.class);
                    // Đóng tất cả Activity và mở Login
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    getActivity().finishAffinity();
                }
            });
        }

        // Mở màn hình Đổi mật khẩu
        if (btnChangePassword != null) {
            btnChangePassword.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), ChangePasswordActivity.class);
                startActivity(intent);
            });
        }

        // Chuyển sang màn hình Chỉnh sửa hồ sơ
        if (btnEditProfile != null) {
            btnEditProfile.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), EditProfileActivity.class);
                startActivity(intent);
            });
        }
        
        if (btnScanHistory != null) {
            btnScanHistory.setOnClickListener(v -> Toast.makeText(getContext(), "Chức năng Lịch sử quét đang phát triển", Toast.LENGTH_SHORT).show());
        }
    }

    private void loadUserData(String uid) {
        // Sử dụng addValueEventListener để tự động cập nhật UI khi dữ liệu trong database thay đổi (sau khi Edit Profile)
        mDatabase.child("users").child(uid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return; // Kiểm tra Fragment còn gắn vào Activity không

                if (snapshot.exists()) {
                    User user = snapshot.getValue(User.class);
                    if (user != null) {
                        tvUserName.setText(user.name);
                        tvUserEmail.setText(user.email);
                    }
                } else {
                    // Fallback nếu không có trong database
                    FirebaseUser firebaseUser = mAuth.getCurrentUser();
                    if (firebaseUser != null) {
                        tvUserEmail.setText(firebaseUser.getEmail());
                        tvUserName.setText("Người dùng AI");
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (isAdded()) {
                    Toast.makeText(getContext(), "Lỗi tải dữ liệu: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void redirectToLogin() {
        if (getActivity() != null) {
            Intent intent = new Intent(getActivity(), Login.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            getActivity().finish();
        }
    }
}
