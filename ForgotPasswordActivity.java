package com.securestegox;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class ForgotPasswordActivity extends AppCompatActivity {

    // =========================================================
    // UI
    // =========================================================

    private EditText etUsername;
    private EditText etEmail;
    private EditText etNewPassword;
    private EditText etConfirmPassword;

    private Button btnResetPassword;
    private TextView tvBackToLogin;

    // =========================================================
    // DATABASE
    // =========================================================

    private DatabaseHelper db;

    // =========================================================
    // ACTIVITY CREATE
    // =========================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        // =====================================================
        // LOAD XML
        // =====================================================

        setContentView(
                R.layout.activity_forgot_password
        );

        // =====================================================
        // DATABASE
        // =====================================================

        db = new DatabaseHelper(this);

        // =====================================================
        // FIND UI
        // =====================================================

        etUsername =
                findViewById(R.id.etUsername);

        etEmail =
                findViewById(R.id.etEmail);

        etNewPassword =
                findViewById(R.id.etNewPassword);

        etConfirmPassword =
                findViewById(R.id.etConfirmPassword);

        btnResetPassword =
                findViewById(R.id.btnResetPassword);

        tvBackToLogin =
                findViewById(R.id.tvBackToLogin);

        // =====================================================
        // RESET PASSWORD
        // =====================================================

        btnResetPassword.setOnClickListener(
                v -> resetPassword()
        );

        // =====================================================
        // BACK TO LOGIN
        // =====================================================

        tvBackToLogin.setOnClickListener(
                v -> goBackToLogin()
        );
    }

    // =========================================================
    // RESET PASSWORD
    // =========================================================

    private void resetPassword() {

        // =====================================================
        // GET VALUES
        // =====================================================

        String username =
                etUsername.getText()
                        .toString()
                        .trim();

        String email =
                etEmail.getText()
                        .toString()
                        .trim();

        String newPassword =
                etNewPassword.getText()
                        .toString();

        String confirmPassword =
                etConfirmPassword.getText()
                        .toString();

        // =====================================================
        // CLEAR OLD ERRORS
        // =====================================================

        etUsername.setError(null);
        etEmail.setError(null);
        etNewPassword.setError(null);
        etConfirmPassword.setError(null);

        // =====================================================
        // EMPTY USERNAME
        // =====================================================

        if (username.isEmpty()) {

            etUsername.setError(
                    "Enter username"
            );

            etUsername.requestFocus();

            return;
        }

        // =====================================================
        // EMPTY EMAIL
        // =====================================================

        if (email.isEmpty()) {

            etEmail.setError(
                    "Enter registered email"
            );

            etEmail.requestFocus();

            return;
        }

        // =====================================================
        // EMAIL VALIDATION
        // =====================================================

        if (!android.util.Patterns
                .EMAIL_ADDRESS
                .matcher(email)
                .matches()) {

            etEmail.setError(
                    "Enter a valid email"
            );

            etEmail.requestFocus();

            return;
        }

        // =====================================================
        // EMPTY NEW PASSWORD
        // =====================================================

        if (newPassword.isEmpty()) {

            etNewPassword.setError(
                    "Enter new password"
            );

            etNewPassword.requestFocus();

            return;
        }

        // =====================================================
        // PASSWORD LENGTH
        // =====================================================

        if (newPassword.length() < 4) {

            etNewPassword.setError(
                    "Minimum 4 characters required"
            );

            etNewPassword.requestFocus();

            return;
        }

        // =====================================================
        // EMPTY CONFIRM PASSWORD
        // =====================================================

        if (confirmPassword.isEmpty()) {

            etConfirmPassword.setError(
                    "Confirm your password"
            );

            etConfirmPassword.requestFocus();

            return;
        }

        // =====================================================
        // PASSWORD MATCH
        // =====================================================

        if (!newPassword.equals(confirmPassword)) {

            etConfirmPassword.setError(
                    "Passwords do not match"
            );

            etConfirmPassword.requestFocus();

            Toast.makeText(
                    this,
                    "Passwords do not match",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        // =====================================================
        // VERIFY USER
        // =====================================================

        btnResetPassword.setEnabled(false);

        boolean userVerified;

        try {

            userVerified =
                    db.verifyUserForPasswordReset(
                            username,
                            email
                    );

        } catch (Exception e) {

            e.printStackTrace();

            btnResetPassword.setEnabled(true);

            Toast.makeText(
                    this,
                    "Unable to verify account",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        // =====================================================
        // USER NOT VERIFIED
        // =====================================================

        if (!userVerified) {

            btnResetPassword.setEnabled(true);

            Toast.makeText(
                    this,
                    "Username and email do not match",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        // =====================================================
        // UPDATE PASSWORD
        // =====================================================

        boolean updated;

        try {

            updated =
                    db.updatePassword(
                            username,
                            newPassword
                    );

        } catch (Exception e) {

            e.printStackTrace();

            btnResetPassword.setEnabled(true);

            Toast.makeText(
                    this,
                    "Password reset failed",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        // =====================================================
        // SUCCESS
        // =====================================================

        if (updated) {

            hideKeyboard();

            Toast.makeText(
                    this,
                    "Password reset successfully ✔",
                    Toast.LENGTH_LONG
            ).show();

            // =================================================
            // GO TO LOGIN
            // =================================================

            Intent intent =
                    new Intent(
                            ForgotPasswordActivity.this,
                            LoginActivity.class
                    );

            intent.setFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_CLEAR_TASK
            );

            startActivity(intent);

            finish();

        } else {

            btnResetPassword.setEnabled(true);

            Toast.makeText(
                    this,
                    "Password reset failed ❌",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    // =========================================================
    // BACK TO LOGIN
    // =========================================================

    private void goBackToLogin() {

        hideKeyboard();

        Intent intent =
                new Intent(
                        ForgotPasswordActivity.this,
                        LoginActivity.class
                );

        intent.setFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP
        );

        startActivity(intent);

        finish();
    }

    // =========================================================
    // HIDE KEYBOARD
    // =========================================================

    private void hideKeyboard() {

        try {

            InputMethodManager imm =
                    (InputMethodManager)
                            getSystemService(
                                    Context.INPUT_METHOD_SERVICE
                            );

            if (imm != null) {

                imm.hideSoftInputFromWindow(
                        getWindow()
                                .getDecorView()
                                .getWindowToken(),
                        0
                );
            }

        } catch (Exception ignored) {
        }
    }

    // =========================================================
    // ACTIVITY DESTROY
    // =========================================================

    @Override
    protected void onDestroy() {

        if (db != null) {

            db.close();

            db = null;
        }

        super.onDestroy();
    }
}