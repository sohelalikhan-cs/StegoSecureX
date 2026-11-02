package com.securestegox;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public class ScanKeyActivity extends AppCompatActivity {

    // =========================================================
    // UI
    // =========================================================

    private EditText etReceivedKey;

    private Button btnStartScanner;
    private Button btnGalleryQR;
    private Button btnContinue;
    private Button btnBackDashboard;


    // =========================================================
    // QR SCANNERS
    // =========================================================

    private ActivityResultLauncher<ScanOptions> barcodeLauncher;

    private ActivityResultLauncher<String> galleryLauncher;


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
                R.layout.activity_scan_key
        );


        // =====================================================
        // CONNECT XML
        // =====================================================

        etReceivedKey =
                findViewById(
                        R.id.etReceivedKey
                );

        btnStartScanner =
                findViewById(
                        R.id.btnStartScanner
                );

        btnGalleryQR =
                findViewById(
                        R.id.btnGalleryQR
                );

        btnContinue =
                findViewById(
                        R.id.btnContinue
                );

        btnBackDashboard =
                findViewById(
                        R.id.btnBackDashboard
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
        // CAMERA SCANNER
        // =====================================================

        barcodeLauncher =
                registerForActivityResult(
                        new ScanContract(),
                        result -> {

                            if (result == null ||
                                    result.getContents() == null) {

                                Toast.makeText(
                                        this,
                                        "QR scan cancelled",
                                        Toast.LENGTH_SHORT
                                ).show();

                                return;
                            }


                            String qrPayload =
                                    result.getContents().trim();


                            if (qrPayload.isEmpty()) {

                                Toast.makeText(
                                        this,
                                        "QR contains no data",
                                        Toast.LENGTH_SHORT
                                ).show();

                                return;
                            }


                            processQRPayload(
                                    qrPayload,
                                    "CAMERA"
                            );
                        }
                );


        // =====================================================
        // GALLERY SCANNER
        // =====================================================

        galleryLauncher =
                registerForActivityResult(
                        new ActivityResultContracts.GetContent(),
                        uri -> {

                            if (uri == null) {

                                Toast.makeText(
                                        this,
                                        "No QR image selected",
                                        Toast.LENGTH_SHORT
                                ).show();

                                return;
                            }


                            scanQRFromImage(uri);
                        }
                );


        // =====================================================
        // BUTTON ACTIONS
        // =====================================================

        btnStartScanner.setOnClickListener(
                v -> startQRScanner()
        );


        btnGalleryQR.setOnClickListener(
                v -> selectQRFromGallery()
        );


        btnContinue.setOnClickListener(
                v -> continueWithManualKey()
        );


        btnBackDashboard.setOnClickListener(
                v -> finish()
        );
    }


    // =========================================================
    // START CAMERA QR SCANNER
    // =========================================================

    private void startQRScanner() {

        ScanOptions options =
                new ScanOptions();


        options.setPrompt(
                "Scan SecureStegoX QR Code"
        );


        options.setBeepEnabled(true);


        options.setOrientationLocked(false);


        options.setDesiredBarcodeFormats(
                ScanOptions.QR_CODE
        );


        barcodeLauncher.launch(
                options
        );
    }


    // =========================================================
    // SELECT QR FROM GALLERY
    // =========================================================

    private void selectQRFromGallery() {

        galleryLauncher.launch(
                "image/*"
        );
    }


    // =========================================================
    // LOAD BITMAP
    // =========================================================

    private Bitmap loadBitmap(
            Uri uri) throws Exception {

        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.P) {


            ImageDecoder.Source source =
                    ImageDecoder.createSource(
                            getContentResolver(),
                            uri
                    );


            return ImageDecoder.decodeBitmap(
                    source,
                    (decoder, info, src) -> {

                        decoder.setAllocator(
                                ImageDecoder.ALLOCATOR_SOFTWARE
                        );

                        decoder.setMutableRequired(
                                false
                        );
                    }
            );


        } else {

            return MediaStore.Images.Media.getBitmap(
                    getContentResolver(),
                    uri
            );
        }
    }


    // =========================================================
    // SCAN QR FROM GALLERY IMAGE
    // =========================================================

    private void scanQRFromImage(
            Uri imageUri) {

        Bitmap bitmap = null;


        try {

            Toast.makeText(
                    this,
                    "Reading SecureStegoX QR...",
                    Toast.LENGTH_SHORT
            ).show();


            bitmap =
                    loadBitmap(
                            imageUri
                    );


            if (bitmap == null) {

                Toast.makeText(
                        this,
                        "Unable to load QR image",
                        Toast.LENGTH_LONG
                ).show();

                return;
            }


            int width =
                    bitmap.getWidth();


            int height =
                    bitmap.getHeight();


            int[] pixels =
                    new int[
                            width * height
                            ];


            bitmap.getPixels(
                    pixels,
                    0,
                    width,
                    0,
                    0,
                    width,
                    height
            );


            RGBLuminanceSource source =
                    new RGBLuminanceSource(
                            width,
                            height,
                            pixels
                    );


            BinaryBitmap binaryBitmap =
                    new BinaryBitmap(
                            new HybridBinarizer(
                                    source
                            )
                    );


            // =================================================
            // ZXING HINTS
            // =================================================

            Map<DecodeHintType, Object> hints =
                    new EnumMap<>(
                            DecodeHintType.class
                    );


            hints.put(
                    DecodeHintType.TRY_HARDER,
                    Boolean.TRUE
            );


            hints.put(
                    DecodeHintType.POSSIBLE_FORMATS,
                    Collections.singletonList(
                            BarcodeFormat.QR_CODE
                    )
            );


            // =================================================
            // READ QR
            // =================================================

            Result result = null;


            try {

                QRCodeReader reader =
                        new QRCodeReader();


                result =
                        reader.decode(
                                binaryBitmap,
                                hints
                        );


            } catch (NotFoundException e) {

                MultiFormatReader reader =
                        new MultiFormatReader();


                reader.setHints(
                        hints
                );


                result =
                        reader.decode(
                                binaryBitmap
                        );
            }


            // =================================================
            // PROCESS RESULT
            // =================================================

            if (result == null ||
                    result.getText() == null ||
                    result.getText().trim().isEmpty()) {


                addHistory(
                        "SCAN KEY - GALLERY",
                        "QR Secret Key",
                        "FAILED - EMPTY QR"
                );


                Toast.makeText(
                        this,
                        "No valid QR data found",
                        Toast.LENGTH_LONG
                ).show();


                return;
            }


            String qrPayload =
                    result.getText().trim();


            processQRPayload(
                    qrPayload,
                    "GALLERY"
            );


        } catch (Exception e) {

            e.printStackTrace();


            addHistory(
                    "SCAN KEY - GALLERY",
                    "QR Secret Key",
                    "FAILED"
            );


            Toast.makeText(
                    this,
                    "Unable to read QR image",
                    Toast.LENGTH_LONG
            ).show();


        } finally {

            if (bitmap != null &&
                    !bitmap.isRecycled()) {

                bitmap.recycle();
            }
        }
    }


    // =========================================================
    // PROCESS SECURESTEGOX QR PAYLOAD
    // =========================================================

    private void processQRPayload(
            String qrPayload,
            String source) {


        if (qrPayload == null ||
                qrPayload.trim().isEmpty()) {

            Toast.makeText(
                    this,
                    "Invalid QR data",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }


        qrPayload =
                qrPayload.trim();


        // =====================================================
        // IMPORTANT SECURITY CHECK
        //
        // QR MUST BE A SECURESTEGOX ENCRYPTED QR.
        //
        // Plain text secret keys are NOT accepted here.
        // =====================================================

        if (!qrPayload.startsWith(
                SecurityUtils.QR_PREFIX
        )) {


            addHistory(
                    "SCAN KEY - " + source,
                    "Unknown QR",
                    "FAILED - NOT SECURESTEGOX QR"
            );


            Toast.makeText(
                    this,
                    "Invalid QR! This is not a SecureStegoX QR code.",
                    Toast.LENGTH_LONG
            ).show();


            return;
        }


        // =====================================================
        // DECRYPT QR PAYLOAD
        // =====================================================

        String decryptedKey;


        try {

            decryptedKey =
                    SecurityUtils.decryptQRPayload(
                            qrPayload
                    );


        } catch (Exception e) {

            e.printStackTrace();


            addHistory(
                    "SCAN KEY - " + source,
                    "SecureStegoX QR",
                    "FAILED - DECRYPTION ERROR"
            );


            Toast.makeText(
                    this,
                    "❌ SecureStegoX QR could not be verified.",
                    Toast.LENGTH_LONG
            ).show();


            return;
        }


        // =====================================================
        // VALIDATE DECRYPTED KEY
        // =====================================================

        if (decryptedKey == null ||
                decryptedKey.trim().isEmpty()) {


            addHistory(
                    "SCAN KEY - " + source,
                    "SecureStegoX QR",
                    "FAILED - INVALID KEY"
            );


            Toast.makeText(
                    this,
                    "❌ QR does not contain a valid secret key.",
                    Toast.LENGTH_LONG
            ).show();


            return;
        }


        decryptedKey =
                decryptedKey.trim();


        // =====================================================
        // PUT KEY IN FIELD
        //
        // It is immediately passed to DecodeActivity.
        // =====================================================

        etReceivedKey.setText(
                decryptedKey
        );


        addHistory(
                "SCAN KEY - " + source,
                "SecureStegoX QR",
                "SUCCESS"
        );


        Toast.makeText(
                this,
                "✓ Secure QR verified successfully",
                Toast.LENGTH_SHORT
        ).show();


        // =====================================================
        // DIRECTLY OPEN DECODE
        // =====================================================

        openDecodeActivity(
                decryptedKey
        );
    }


    // =========================================================
    // MANUAL KEY
    // =========================================================

    private void continueWithManualKey() {

        String key =
                etReceivedKey
                        .getText()
                        .toString()
                        .trim();


        if (key.isEmpty()) {

            etReceivedKey.setError(
                    "Enter secret key"
            );


            etReceivedKey.requestFocus();


            Toast.makeText(
                    this,
                    "Please enter secret key",
                    Toast.LENGTH_SHORT
            ).show();


            return;
        }


        addHistory(
                "RECEIVE KEY - MANUAL",
                "Secret Key",
                "SUCCESS"
        );


        openDecodeActivity(
                key
        );
    }


    // =========================================================
    // OPEN DECODE ACTIVITY
    // =========================================================

    private void openDecodeActivity(
            String key) {


        if (key == null ||
                key.trim().isEmpty()) {

            Toast.makeText(
                    this,
                    "Invalid secret key ❌",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }


        Intent intent =
                new Intent(
                        ScanKeyActivity.this,
                        DecodeActivity.class
                );


        intent.putExtra(
                "RECEIVED_SECRET_KEY",
                key.trim()
        );


        intent.putExtra(
                "username",
                username
        );


        startActivity(
                intent
        );


        // Close Receive Key screen
        // so user does not return to it
        // when pressing back from Decode.

        finish();
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


        /*
         * SECURITY:
         *
         * Actual secret key is NEVER
         * stored inside history.
         */

        db.insertHistory(
                username,
                operation,
                fileName,
                "",
                status
        );
    }


    // =========================================================
    // DESTROY
    // =========================================================

    @Override
    protected void onDestroy() {

        if (db != null) {

            db.close();
        }


        super.onDestroy();
    }
}