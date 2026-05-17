package van.edu.vn.smartcameraai;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ChangePasswordActivity extends AppCompatActivity {

    private TextInputEditText edtNewPass, edtConfirmPass;
    private MaterialButton btnSubmit;
    private ImageButton btnBack;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();

        // Kiểm tra đăng nhập
        if (user == null) {
            startActivity(new Intent(this, Login.class));
            finish();
            return;
        }

        edtNewPass = findViewById(R.id.edtNewPassword);
        edtConfirmPass = findViewById(R.id.edtConfirmPassword);
        btnSubmit = findViewById(R.id.btnSubmitChange);
        btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());

        btnSubmit.setOnClickListener(v -> {
            String newPass = edtNewPass.getText().toString().trim();
            String confirmPass = edtConfirmPass.getText().toString().trim();

            if (validateInput(newPass, confirmPass)) {
                // Thực hiện đổi mật khẩu trên Firebase
                user.updatePassword(newPass)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(this, "Đổi mật khẩu thành công!", Toast.LENGTH_SHORT).show();
                                finish(); // Quay lại màn hình trước đó
                            } else {
                                Toast.makeText(this, "Lỗi: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                            }
                        });
            }
        });
    }

    private boolean validateInput(String pass, String confirm) {
        if (pass.isEmpty()) {
            edtNewPass.setError("Không được để trống");
            return false;
        }
        if (pass.length() < 6) {
            edtNewPass.setError("Mật khẩu phải tối thiểu 6 ký tự");
            return false;
        }
        if (!pass.equals(confirm)) {
            edtConfirmPass.setError("Mật khẩu xác nhận không khớp");
            return false;
        }
        return true;
    }
}
