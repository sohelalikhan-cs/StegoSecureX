package com.securestegox;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.text.method.HideReturnsTransformationMethod;
import android.util.Patterns;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class RegisterActivity extends AppCompatActivity {

    // =========================================================
    // UI COMPONENTS
    // =========================================================

    private EditText etFullName;
    private EditText etUsername;
    private EditText etEmail;
    private EditText etPassword;

    private Button btnRegister;
    private TextView tvBackToLogin;

    // =========================================================
    // DATABASE
    // =========================================================

    private DatabaseHelper db;


    // =========================================================
    // ON CREATE
    // =========================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_register);


        // =====================================================
        // DATABASE
        // =====================================================

        db = new DatabaseHelper(this);


        // =====================================================
        // CONNECT XML
        // =====================================================

        etFullName = findViewById(R.id.etFullName);
        etUsername = findViewById(R.id.etUsername);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);

        btnRegister = findViewById(R.id.btnRegister);
        tvBackToLogin = findViewById(R.id.tvBackToLogin);


        // =====================================================
        // PASSWORD SHOW / HIDE
        // =====================================================

        etPassword.setOnTouchListener((v, event) -> {

            if (event.getAction() == MotionEvent.ACTION_UP) {

                int drawableRightWidth =
                        etPassword.getCompoundDrawables()[2] != null
                                ? etPassword.getCompoundDrawables()[2]
                                .getBounds()
                                .width()
                                : 0;


                // Check whether right-side eye was clicked
                if (drawableRightWidth > 0 &&
                        event.getX() >=
                                etPassword.getWidth()
                                        - etPassword.getPaddingEnd()
                                        - drawableRightWidth) {


                    int cursorPosition =
                            etPassword.getSelectionStart();


                    // =================================================
                    // IF PASSWORD IS HIDDEN
                    // =================================================

                    if (etPassword.getTransformationMethod()
                            instanceof PasswordTransformationMethod) {

                        // SHOW PASSWORD

                        etPassword.setTransformationMethod(
                                HideReturnsTransformationMethod.getInstance()
                        );

                    }

                    // =================================================
                    // IF PASSWORD IS VISIBLE
                    // =================================================

                    else {

                        // HIDE PASSWORD

                        etPassword.setTransformationMethod(
                                PasswordTransformationMethod.getInstance()
                        );
                    }


                    // Keep cursor position
                    etPassword.setSelection(
                            Math.max(
                                    0,
                                    cursorPosition
                            )
                    );


                    return true;
                }
            }


            return false;
        });


        // =====================================================
        // REGISTER BUTTON
        // =====================================================

        btnRegister.setOnClickListener(
                v -> registerUser()
        );


        // =====================================================
        // BACK TO LOGIN
        // =====================================================

        tvBackToLogin.setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            RegisterActivity.this,
                            LoginActivity.class
                    );

            startActivity(intent);

            finish();
        });
    }


    // =========================================================
    // REGISTER USER
    // =========================================================

    private void registerUser() {

        // =====================================================
        // GET INPUT
        // =====================================================

        String fullname =
                etFullName.getText()
                        .toString()
                        .trim();

        String username =
                etUsername.getText()
                        .toString()
                        .trim();

        String email =
                etEmail.getText()
                        .toString()
                        .trim();

        String password =
                etPassword.getText()
                        .toString()
                        .trim();


        // =====================================================
        // EMPTY FIELD VALIDATION
        // =====================================================

        if (fullname.isEmpty() ||
                username.isEmpty() ||
                email.isEmpty() ||
                password.isEmpty()) {

            Toast.makeText(
                    this,
                    "Please fill all fields",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }


        // =====================================================
        // EMAIL VALIDATION
        // =====================================================

        if (!Patterns.EMAIL_ADDRESS
                .matcher(email)
                .matches()) {

            etEmail.setError(
                    "Enter valid email"
            );

            etEmail.requestFocus();

            return;
        }


        // =====================================================
        // PASSWORD VALIDATION
        // =====================================================

        if (password.length() < 4) {

            etPassword.setError(
                    "Password must be at least 4 characters"
            );

            etPassword.requestFocus();

            return;
        }


        // =====================================================
        // USERNAME CHECK
        // =====================================================

        if (db.checkUserExists(username)) {

            etUsername.setError(
                    "Username already exists"
            );

            etUsername.requestFocus();

            Toast.makeText(
                    this,
                    "Username already exists",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }


        // =====================================================
        // REGISTER USER
        // =====================================================

        boolean success =
                db.registerUser(
                        fullname,
                        username,
                        email,
                        password
                );


        // =====================================================
        // REGISTRATION SUCCESS
        // =====================================================

        if (success) {

            Toast.makeText(
                    this,
                    "Registration Successful ✔",
                    Toast.LENGTH_SHORT
            ).show();


            Intent intent =
                    new Intent(
                            RegisterActivity.this,
                            LoginActivity.class
                    );

            startActivity(intent);

            finish();

        }

        // =====================================================
        // REGISTRATION FAILED
        // =====================================================

        else {

            Toast.makeText(
                    this,
                    "Registration Failed ",
                    Toast.LENGTH_SHORT
            ).show();
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