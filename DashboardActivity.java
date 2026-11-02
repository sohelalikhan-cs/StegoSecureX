package com.securestegox;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
public class DashboardActivity extends AppCompatActivity {
    private Button btnEncode, btnDecode, btnDetection,
            btnRecovery, btnHistory, btnShareKey,
            btnReceiveKey, btnLogout;

    private TextView tvWelcome;
    private String username;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);
        // logged-in username
        username = getIntent().getStringExtra("username");
        if (username == null || username.trim().isEmpty()) {
            username = "User";
        }
        initViews();
        setListeners();
    }
    // INITIALIZE VIEWS
    private void initViews() {
        tvWelcome = findViewById(R.id.tvWelcome);
        btnEncode = findViewById(R.id.btnEncode);
        btnDecode = findViewById(R.id.btnDecode);
        btnDetection = findViewById(R.id.btnDetection);
        btnRecovery = findViewById(R.id.btnRecovery);
        btnHistory = findViewById(R.id.btnHistory);
        btnShareKey = findViewById(R.id.btnShareKey);
        btnReceiveKey = findViewById(R.id.btnReceiveKey);
        btnLogout = findViewById(R.id.btnLogout);
        // Welcome message
        tvWelcome.setText("Welcome, " + username);
    }
    // BUTTON LISTENERS
    private void setListeners() {
        // Encode
        btnEncode.setOnClickListener(v ->
                openActivity(EncodeActivity.class));
        // Decode
        btnDecode.setOnClickListener(v ->
                openActivity(DecodeActivity.class));
        // Detection
        btnDetection.setOnClickListener(v ->
                openActivity(DetectionActivity.class));
        // Recovery
        btnRecovery.setOnClickListener(v ->
                openActivity(RecoveryActivity.class));
        // History
        btnHistory.setOnClickListener(v ->
                openActivity(HistoryActivity.class));
        // Share Key
        btnShareKey.setOnClickListener(v ->
                openActivity(ShareKeyActivity.class));
        // Receive Key
        btnReceiveKey.setOnClickListener(v ->
                openActivity(ScanKeyActivity.class));
        // Logout
        btnLogout.setOnClickListener(v -> showLogoutDialog());
    }
    // OPEN ACTIVITY
    private void openActivity(Class<?> activityClass) {
        Intent intent = new Intent(this, activityClass);
        // Send username to next activity
        intent.putExtra("username", username);
        startActivity(intent);
    }
    // LOGOUT CONFIRMATION
    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setNegativeButton("CANCEL", null)
                .setPositiveButton("YES, LOGOUT",
                        (dialog, which) -> logout())
                .show();
    }
    // LOGOUT
    private void logout() {
        Intent intent =
                new Intent(
                        DashboardActivity.this,
                        LoginActivity.class
                );
        // Clear Dashboard and previous activities
        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
        );
        startActivity(intent);
        finish();
    }
}