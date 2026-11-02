package com.securestegox;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
public class LoginActivity extends AppCompatActivity {

    // UI COMPONENTS
    private EditText etUsername;
    private EditText etPassword;
    private Button btnLogin;
    private TextView tvRegister;
    private TextView tvForgotPassword;

    // DATABASE
    private DatabaseHelper db;

    // PASSWORD VISIBILITY
    private boolean isPasswordVisible = false;

    // ON CREATE
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(
                R.layout.activity_login
        );

        // DATABASE
        db = new DatabaseHelper(this);

        // CONNECT XML VIEWS
        etUsername =
                findViewById(
                        R.id.etUsername
                );
        etPassword =
                findViewById(
                        R.id.etPassword
                );
        btnLogin =
                findViewById(
                        R.id.btnLogin
                );
        tvRegister =
                findViewById(
                        R.id.tvRegister
                );
        tvForgotPassword =
                findViewById(
                        R.id.tvForgotPassword
                );

        // PASSWORD SHOW / HIDE
        etPassword.setOnTouchListener(
                (v, event) -> {

                    if (event.getAction() ==
                            MotionEvent.ACTION_UP) {

                        // -------------------------------------
                        // GET RIGHT DRAWABLE
                        // -------------------------------------

                        android.graphics.drawable.Drawable drawable =
                                etPassword.getCompoundDrawables()[2];


                        if (drawable != null) {

                            int drawableWidth =
                                    drawable.getBounds().width();


                            // ---------------------------------
                            // CHECK EYE CLICK
                            // ---------------------------------

                            if (event.getX() >=
                                    etPassword.getWidth()
                                            - etPassword.getPaddingEnd()
                                            - drawableWidth
                                            - 30) {


                                // =============================
                                // SAVE CURSOR POSITION
                                // =============================

                                int selection =
                                        etPassword.getSelectionStart();


                                // =============================
                                // SHOW PASSWORD
                                // =============================

                                if (!isPasswordVisible) {

                                    etPassword.setInputType(
                                            InputType.TYPE_CLASS_TEXT
                                                    | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                                    );

                                    isPasswordVisible = true;

                                }


                                // =============================
                                // HIDE PASSWORD
                                // =============================

                                else {

                                    etPassword.setInputType(
                                            InputType.TYPE_CLASS_TEXT
                                                    | InputType.TYPE_TEXT_VARIATION_PASSWORD
                                    );

                                    isPasswordVisible = false;
                                }


                                // =============================
                                // RESTORE CURSOR
                                // =============================

                                etPassword.setSelection(
                                        Math.max(
                                                0,
                                                selection
                                        )
                                );


                                return true;
                            }
                        }
                    }


                    return false;
                }
        );


        // =====================================================
        // LOGIN BUTTON
        // =====================================================

        btnLogin.setOnClickListener(
                v -> loginUser()
        );


        // =====================================================
        // REGISTER
        // =====================================================

        tvRegister.setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            LoginActivity.this,
                            RegisterActivity.class
                    );

            startActivity(intent);
        });


        // =====================================================
        // FORGOT PASSWORD
        // =====================================================

        tvForgotPassword.setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            LoginActivity.this,
                            ForgotPasswordActivity.class
                    );

            startActivity(intent);
        });
    }


    // =========================================================
    // LOGIN USER
    // =========================================================

    private void loginUser() {

        // =====================================================
        // GET USERNAME
        // =====================================================

        String username =
                etUsername.getText()
                        .toString()
                        .trim();


        // =====================================================
        // GET PASSWORD
        // =====================================================

        String password =
                etPassword.getText()
                        .toString()
                        .trim();


        // =====================================================
        // USERNAME VALIDATION
        // =====================================================

        if (username.isEmpty()) {

            etUsername.setError(
                    "Enter username"
            );

            etUsername.requestFocus();

            return;
        }


        // =====================================================
        // PASSWORD VALIDATION
        // =====================================================

        if (password.isEmpty()) {

            etPassword.setError(
                    "Enter password"
            );

            etPassword.requestFocus();

            return;
        }


        // =====================================================
        // CHECK LOGIN
        // =====================================================

        boolean success =
                db.loginUser(
                        username,
                        password
                );


        // =====================================================
        // LOGIN SUCCESS
        // =====================================================

        if (success) {

            // Login history is already saved
            // inside DatabaseHelper.loginUser()


            Toast.makeText(
                    this,
                    "Login Successful ✔",
                    Toast.LENGTH_SHORT
            ).show();


            // =================================================
            // OPEN DASHBOARD
            // =================================================

            Intent intent =
                    new Intent(
                            LoginActivity.this,
                            DashboardActivity.class
                    );


            // =================================================
            // SEND USERNAME
            // =================================================

            intent.putExtra(
                    "username",
                    username
            );


            startActivity(
                    intent
            );


            finish();

        }


        // =====================================================
        // LOGIN FAILED
        // =====================================================

        else {

            Toast.makeText(
                    this,
                    "Invalid Username or Password ❌",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }


    // =========================================================
    // ON DESTROY
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