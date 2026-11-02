package com.securestegox;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;

import java.io.InputStream;
import java.io.OutputStream;


public class ShareKeyActivity extends AppCompatActivity {

    // =========================================================
    // UI
    // =========================================================

    private TextView tvSelectedFile;

    private EditText etSecretKey;

    private Button btnSelectFile;
    private Button btnGenerateSaveQR;
    private Button btnCopyShareKey;
    private Button btnBackDashboard;
    private Button btnLogout;


    // =========================================================
    // FILE / QR
    // =========================================================

    private Uri selectedFileUri;

    private Bitmap generatedQRBitmap;

    private Uri savedQRUri;

    private static final int FILE_PICKER_REQUEST = 100;


    // =========================================================
    // STEGO MARKER
    // =========================================================

    private static final String STEGO_MARKER =
            "STEGOX";


    // =========================================================
    // DATABASE
    // =========================================================

    private DatabaseHelper db;

    private String username;


    // =========================================================
    // ON CREATE
    // =========================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_share_key
        );


        // =====================================================
        // CONNECT XML
        // =====================================================

        tvSelectedFile =
                findViewById(
                        R.id.tvSelectedFile
                );

        etSecretKey =
                findViewById(
                        R.id.etSecretKey
                );

        btnSelectFile =
                findViewById(
                        R.id.btnSelectFile
                );

        btnGenerateSaveQR =
                findViewById(
                        R.id.btnGenerateSaveQR
                );

        btnCopyShareKey =
                findViewById(
                        R.id.btnCopyShareKey
                );

        btnBackDashboard =
                findViewById(
                        R.id.btnBackDashboard
                );

        btnLogout =
                findViewById(
                        R.id.btnLogout
                );


        // =====================================================
        // DATABASE
        // =====================================================

        db =
                new DatabaseHelper(this);


        // =====================================================
        // USERNAME
        // =====================================================

        username =
                getIntent().getStringExtra(
                        "username"
                );


        if (username == null ||
                username.trim().isEmpty()) {

            username = "User";
        }


        // =====================================================
        // RECEIVE FILE FROM ENCODE ACTIVITY
        // =====================================================

        String encodedUri =
                getIntent().getStringExtra(
                        "encodedFileUri"
                );


        String encodedName =
                getIntent().getStringExtra(
                        "encodedFileName"
                );


        if (encodedUri != null &&
                !encodedUri.trim().isEmpty()) {

            try {

                Uri uri =
                        Uri.parse(encodedUri);


                if (isStegoFile(uri)) {

                    selectedFileUri =
                            uri;


                    if (encodedName != null &&
                            !encodedName.trim().isEmpty()) {

                        tvSelectedFile.setText(
                                encodedName
                        );

                    } else {

                        tvSelectedFile.setText(
                                getFileName(uri)
                        );
                    }

                }

            } catch (Exception e) {

                e.printStackTrace();
            }
        }


        // =====================================================
        // BUTTON ACTIONS
        // =====================================================

        btnSelectFile.setOnClickListener(
                v -> selectStegoFile()
        );


        btnGenerateSaveQR.setOnClickListener(
                v -> generateAndSaveQR()
        );


        btnCopyShareKey.setOnClickListener(
                v -> copyAndShareKey()
        );


        btnBackDashboard.setOnClickListener(
                v -> goToDashboard()
        );


        btnLogout.setOnClickListener(
                v -> logout()
        );
    }


    // =========================================================
    // SELECT STEGO FILE
    // =========================================================

    private void selectStegoFile() {

        Intent intent =
                new Intent(
                        Intent.ACTION_OPEN_DOCUMENT
                );

        intent.addCategory(
                Intent.CATEGORY_OPENABLE
        );

        intent.setType("*/*");

        intent.addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION
        );

        startActivityForResult(
                intent,
                FILE_PICKER_REQUEST
        );
    }


    // =========================================================
    // FILE RESULT
    // =========================================================

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data) {

        super.onActivityResult(
                requestCode,
                resultCode,
                data
        );


        if (requestCode != FILE_PICKER_REQUEST ||
                resultCode != RESULT_OK ||
                data == null ||
                data.getData() == null) {

            return;
        }


        Uri uri =
                data.getData();


        // =====================================================
        // VALIDATE STEGO FILE
        // =====================================================

        if (!isStegoFile(uri)) {

            selectedFileUri = null;

            tvSelectedFile.setText(
                    "No valid stego file selected"
            );


            Toast.makeText(
                    this,
                    "Invalid file! Please select a SecureStegoX stego file.",
                    Toast.LENGTH_LONG
            ).show();


            addHistory(
                    "SELECT FILE",
                    getFileName(uri),
                    "FAILED - NOT A STEGO FILE"
            );

            return;
        }


        // =====================================================
        // SAVE SELECTED FILE
        // =====================================================

        selectedFileUri = uri;


        String fileName =
                getFileName(uri);


        tvSelectedFile.setText(
                fileName
        );


        Toast.makeText(
                this,
                "SecureStegoX stego file selected ✔",
                Toast.LENGTH_SHORT
        ).show();


        addHistory(
                "SELECT STEGO FILE",
                fileName,
                "SUCCESS"
        );
    }


    // =========================================================
    // CHECK STEGO FILE
    // =========================================================

    private boolean isStegoFile(Uri uri) {

        if (uri == null) {

            return false;
        }


        InputStream inputStream = null;


        try {

            inputStream =
                    getContentResolver()
                            .openInputStream(uri);


            if (inputStream == null) {

                return false;
            }


            byte[] buffer =
                    new byte[8192];


            byte[] markerBytes =
                    STEGO_MARKER.getBytes(
                            java.nio.charset.StandardCharsets.UTF_8
                    );


            int markerIndex = 0;

            int bytesRead;


            while ((bytesRead =
                    inputStream.read(buffer)) != -1) {


                for (int i = 0;
                     i < bytesRead;
                     i++) {


                    if (buffer[i] ==
                            markerBytes[markerIndex]) {

                        markerIndex++;


                        if (markerIndex ==
                                markerBytes.length) {

                            inputStream.close();

                            return true;
                        }

                    } else {

                        if (buffer[i] ==
                                markerBytes[0]) {

                            markerIndex = 1;

                        } else {

                            markerIndex = 0;
                        }
                    }
                }
            }


            inputStream.close();

            return false;


        } catch (Exception e) {

            e.printStackTrace();


            try {

                if (inputStream != null) {

                    inputStream.close();
                }

            } catch (Exception ignored) {
            }


            return false;
        }
    }


    // =========================================================
    // VALIDATE SELECTED FILE
    // =========================================================

    private boolean validateSelectedStegoFile() {

        if (selectedFileUri == null) {

            Toast.makeText(
                    this,
                    "Please select a valid SecureStegoX stego file first.",
                    Toast.LENGTH_LONG
            ).show();

            return false;
        }


        if (!isStegoFile(selectedFileUri)) {

            Toast.makeText(
                    this,
                    "Invalid SecureStegoX stego file.",
                    Toast.LENGTH_LONG
            ).show();

            return false;
        }


        return true;
    }


    // =========================================================
    // GET FILE NAME
    // =========================================================

    private String getFileName(Uri uri) {

        if (uri == null) {

            return "Selected file";
        }


        String fileName = null;


        if ("content".equals(uri.getScheme())) {

            Cursor cursor =
                    getContentResolver().query(
                            uri,
                            null,
                            null,
                            null,
                            null
                    );


            if (cursor != null) {

                try {

                    int nameIndex =
                            cursor.getColumnIndex(
                                    OpenableColumns.DISPLAY_NAME
                            );


                    if (nameIndex >= 0 &&
                            cursor.moveToFirst()) {

                        fileName =
                                cursor.getString(
                                        nameIndex
                                );
                    }

                } finally {

                    cursor.close();
                }
            }
        }


        if (fileName == null) {

            fileName =
                    uri.getLastPathSegment();
        }


        if (fileName == null ||
                fileName.trim().isEmpty()) {

            fileName =
                    "Selected file";
        }


        return fileName;
    }


    // =========================================================
    // GET SECRET KEY
    // =========================================================

    private String getSecretKey() {

        return etSecretKey
                .getText()
                .toString()
                .trim();
    }


    // =========================================================
    // VALIDATE KEY
    // =========================================================

    private boolean validateKey() {

        String key =
                getSecretKey();


        if (key.isEmpty()) {

            etSecretKey.setError(
                    "Enter secret key"
            );


            etSecretKey.requestFocus();


            Toast.makeText(
                    this,
                    "Please enter secret key.",
                    Toast.LENGTH_SHORT
            ).show();


            return false;
        }


        return true;
    }


    // =========================================================
    // GET SELECTED FILE NAME
    // =========================================================

    private String getSelectedFileName() {

        if (selectedFileUri != null) {

            return getFileName(
                    selectedFileUri
            );
        }


        return "SecureStegoX File";
    }


    // =========================================================
    // GENERATE + SAVE QR
    // =========================================================

    private void generateAndSaveQR() {

        // =====================================================
        // FILE CHECK
        // =====================================================

        if (!validateSelectedStegoFile()) {

            return;
        }


        // =====================================================
        // KEY CHECK
        // =====================================================

        if (!validateKey()) {

            return;
        }


        String key =
                getSecretKey();


        try {

            // =================================================
            // ENCRYPT KEY FOR QR
            // =================================================

            String encryptedQRPayload =
                    SecurityUtils.encryptQRPayload(
                            key
                    );


            // =================================================
            // GENERATE QR MATRIX
            // =================================================

            MultiFormatWriter writer =
                    new MultiFormatWriter();


            BitMatrix matrix =
                    writer.encode(
                            encryptedQRPayload,
                            BarcodeFormat.QR_CODE,
                            600,
                            600
                    );


            int width =
                    matrix.getWidth();


            int height =
                    matrix.getHeight();


            // =================================================
            // CREATE BITMAP
            // =================================================

            if (generatedQRBitmap != null &&
                    !generatedQRBitmap.isRecycled()) {

                generatedQRBitmap.recycle();
            }


            generatedQRBitmap =
                    Bitmap.createBitmap(
                            width,
                            height,
                            Bitmap.Config.ARGB_8888
                    );


            // =================================================
            // DRAW QR
            // =================================================

            for (int x = 0;
                 x < width;
                 x++) {

                for (int y = 0;
                     y < height;
                     y++) {

                    generatedQRBitmap.setPixel(
                            x,
                            y,
                            matrix.get(x, y)
                                    ? Color.BLACK
                                    : Color.WHITE
                    );
                }
            }


            // =================================================
            // SAVE QR
            // =================================================

            boolean saved =
                    saveQRCode();


            if (!saved) {

                return;
            }


            // =================================================
            // HISTORY
            // =================================================

            addHistory(
                    "GENERATE & SAVE QR",
                    getSelectedFileName(),
                    "SUCCESS"
            );


            // =================================================
            // SHOW QR
            // =================================================

            showQRDialog();


        } catch (Exception e) {

            e.printStackTrace();


            addHistory(
                    "GENERATE & SAVE QR",
                    getSelectedFileName(),
                    "FAILED"
            );


            Toast.makeText(
                    this,
                    "QR generation failed.",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }


    // =========================================================
    // SAVE QR
    // =========================================================

    private boolean saveQRCode() {

        if (generatedQRBitmap == null ||
                generatedQRBitmap.isRecycled()) {

            Toast.makeText(
                    this,
                    "QR code is not generated.",
                    Toast.LENGTH_SHORT
            ).show();

            return false;
        }


        try {

            String fileName =
                    "SecureStegoX_QR_" +
                            System.currentTimeMillis() +
                            ".png";


            ContentValues values =
                    new ContentValues();


            values.put(
                    MediaStore.Images.Media.DISPLAY_NAME,
                    fileName
            );


            values.put(
                    MediaStore.Images.Media.MIME_TYPE,
                    "image/png"
            );


            values.put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES +
                            "/SecureStegoX"
            );


            Uri imageUri =
                    getContentResolver().insert(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            values
                    );


            if (imageUri == null) {

                Toast.makeText(
                        this,
                        "Unable to save QR.",
                        Toast.LENGTH_SHORT
                ).show();

                return false;
            }


            OutputStream outputStream =
                    getContentResolver()
                            .openOutputStream(
                                    imageUri
                            );


            if (outputStream == null) {

                return false;
            }


            boolean compressed =
                    generatedQRBitmap.compress(
                            Bitmap.CompressFormat.PNG,
                            100,
                            outputStream
                    );


            outputStream.close();


            if (!compressed) {

                getContentResolver().delete(
                        imageUri,
                        null,
                        null
                );

                return false;
            }


            savedQRUri =
                    imageUri;


            addHistory(
                    "SAVE QR",
                    fileName,
                    "SUCCESS"
            );


            Toast.makeText(
                    this,
                    "QR saved in Pictures/SecureStegoX ✔",
                    Toast.LENGTH_LONG
            ).show();


            return true;


        } catch (Exception e) {

            e.printStackTrace();


            addHistory(
                    "SAVE QR",
                    "QR Code",
                    "FAILED"
            );


            Toast.makeText(
                    this,
                    "QR save failed.",
                    Toast.LENGTH_SHORT
            ).show();


            return false;
        }
    }


    // =========================================================
    // SHOW QR DIALOG
    // =========================================================

    private void showQRDialog() {

        if (generatedQRBitmap == null) {

            return;
        }


        ImageView qrImage =
                new ImageView(this);


        qrImage.setImageBitmap(
                generatedQRBitmap
        );


        qrImage.setPadding(
                25,
                25,
                25,
                25
        );


        qrImage.setAdjustViewBounds(
                true
        );


        LinearLayout container =
                new LinearLayout(this);


        container.setOrientation(
                LinearLayout.VERTICAL
        );


        container.setGravity(
                Gravity.CENTER
        );


        container.setPadding(
                20,
                10,
                20,
                10
        );


        container.addView(
                qrImage
        );


        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setTitle(
                                "SecureStegoX QR Code"
                        )
                        .setMessage(
                                "Encrypted QR generated successfully.\n\n" +
                                        "Use RECEIVE KEY to recover the secret key."
                        )
                        .setView(
                                container
                        )
                        .setPositiveButton(
                                "DONE",
                                null
                        )
                        .create();


        dialog.show();
    }


    // =========================================================
    // COPY KEY + SHARE QR
    // =========================================================

    private void copyAndShareKey() {

        // =====================================================
        // FILE CHECK
        // =====================================================

        if (!validateSelectedStegoFile()) {

            return;
        }


        // =====================================================
        // KEY CHECK
        // =====================================================

        if (!validateKey()) {

            return;
        }


        // =====================================================
        // QR CHECK
        // =====================================================

        if (generatedQRBitmap == null ||
                generatedQRBitmap.isRecycled()) {

            Toast.makeText(
                    this,
                    "Please generate QR code first.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }


        String key =
                getSecretKey();


        try {

            // =================================================
            // COPY ACTUAL KEY TO CLIPBOARD
            // =================================================

            ClipboardManager clipboard =
                    (ClipboardManager)
                            getSystemService(
                                    Context.CLIPBOARD_SERVICE
                            );


            ClipData clip =
                    ClipData.newPlainText(
                            "SecureStegoX Secret Key",
                            key
                    );


            clipboard.setPrimaryClip(
                    clip
            );


            addHistory(
                    "COPY KEY",
                    getSelectedFileName(),
                    "SUCCESS"
            );


            // =================================================
            // SHARE GENERATED QR
            // =================================================

            if (savedQRUri == null) {

                if (!saveQRCode()) {

                    return;
                }
            }


            Intent shareIntent =
                    new Intent(
                            Intent.ACTION_SEND
                    );


            shareIntent.setType(
                    "image/png"
            );


            shareIntent.putExtra(
                    Intent.EXTRA_STREAM,
                    savedQRUri
            );


            shareIntent.addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
            );


            addHistory(
                    "SHARE QR",
                    getSelectedFileName(),
                    "SUCCESS"
            );


            Toast.makeText(
                    this,
                    "Secret key copied ✔",
                    Toast.LENGTH_SHORT
            ).show();


            startActivity(
                    Intent.createChooser(
                            shareIntent,
                            "Share SecureStegoX QR"
                    )
            );


        } catch (Exception e) {

            e.printStackTrace();


            addHistory(
                    "COPY & SHARE",
                    getSelectedFileName(),
                    "FAILED"
            );


            Toast.makeText(
                    this,
                    "Copy/share failed.",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }


    // =========================================================
    // ADD HISTORY
    // =========================================================

    private void addHistory(
            String operation,
            String fileName,
            String status) {


        if (db == null) {

            db =
                    new DatabaseHelper(this);
        }


        if (username == null ||
                username.trim().isEmpty() ||
                username.equals("User")) {

            return;
        }


        // Secret key is NEVER stored in history.

        db.insertHistory(
                username,
                operation,
                fileName,
                "",
                status
        );
    }


    // =========================================================
    // BACK TO DASHBOARD
    // =========================================================

    private void goToDashboard() {

        Intent intent =
                new Intent(
                        ShareKeyActivity.this,
                        DashboardActivity.class
                );


        intent.putExtra(
                "username",
                username
        );


        startActivity(intent);

        finish();
    }


    // =========================================================
    // LOGOUT
    // =========================================================

    private void logout() {

        Intent intent =
                new Intent(
                        ShareKeyActivity.this,
                        LoginActivity.class
                );


        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
        );


        startActivity(intent);

        finish();
    }


    // =========================================================
    // ON DESTROY
    // =========================================================

    @Override
    protected void onDestroy() {

        if (generatedQRBitmap != null &&
                !generatedQRBitmap.isRecycled()) {

            generatedQRBitmap.recycle();

            generatedQRBitmap = null;
        }


        if (db != null) {

            db.close();

            db = null;
        }


        super.onDestroy();
    }
}