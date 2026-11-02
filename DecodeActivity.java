package com.securestegox;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class DecodeActivity extends AppCompatActivity {

    // UI components
    private Button btnSelect, btnDecode, btnSave, btnOpen, btnBack, btnLogout;
    private EditText etKey;
    private TextView tvFile, tvResult;

    // File data
    private Uri stegoUri;
    private Uri decodedUri;
    private byte[] decodedBytes;
    private String decodedFileName;

    // Stego configuration
    private static final int PICK = 11;
    private static final String MARKER = "\n---STEGOX---\n";

    // Database and logged-in user
    private DatabaseHelper db;
    private String currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_decode);

        // Initialize database
        db = new DatabaseHelper(this);

        // Get logged-in username
        currentUser = getIntent().getStringExtra("username");

        // Connect XML views
        btnSelect = findViewById(R.id.btnSelectStego);
        btnDecode = findViewById(R.id.btnDecode);
        btnSave = findViewById(R.id.btnSave);
        btnOpen = findViewById(R.id.btnOpen);
        btnBack = findViewById(R.id.btnBack);
        btnLogout = findViewById(R.id.btnLogout);

        etKey = findViewById(R.id.etSecretKey);
        tvFile = findViewById(R.id.tvSelectedFile);
        tvResult = findViewById(R.id.tvDecodedText);

        // Receive secret key from Receive Key screen
        String receivedKey = getIntent().getStringExtra("RECEIVED_SECRET_KEY");

        if (receivedKey != null && !receivedKey.trim().isEmpty()) {
            etKey.setText(receivedKey.trim());
            toast("✓ Secret key received");
        }

        // Hide result buttons until a file is successfully decoded
        btnSave.setVisibility(View.GONE);
        btnOpen.setVisibility(View.GONE);

        // Button actions
        btnSelect.setOnClickListener(v -> pick());
        btnDecode.setOnClickListener(v -> decode());
        btnSave.setOnClickListener(v -> saveFile());
        btnOpen.setOnClickListener(v -> openFile());
        btnBack.setOnClickListener(v -> finish());
        btnLogout.setOnClickListener(v -> logout());
    }

    // Select encoded/stego file
    private void pick() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, PICK);
    }

    // Handle selected file
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK && resultCode == RESULT_OK && data != null && data.getData() != null) {
            stegoUri = data.getData();

            tvFile.setText(getName(stegoUri));
            tvResult.setText("");

            decodedBytes = null;
            decodedFileName = null;
            decodedUri = null;

            btnSave.setVisibility(View.GONE);
            btnOpen.setVisibility(View.GONE);

            toast("✓ Stego file selected");
        }
    }

    // Decode hidden data from selected file
    private void decode() {
        try {
            // Validate selected file
            if (stegoUri == null) {
                toast("Please select encoded file");
                return;
            }

            // Validate secret key
            String key = etKey.getText().toString().trim();

            if (key.isEmpty()) {
                etKey.setError("Enter secret key");
                toast("Please enter secret key");
                return;
            }

            // Read selected stego file
            byte[] fileBytes = read(stegoUri);
            byte[] markerBytes = MARKER.getBytes(StandardCharsets.UTF_8);

            // Find steganography marker
            int pos = indexOf(fileBytes, markerBytes);

            if (pos < 0) {
                tvResult.setText("No hidden data found");
                hideResultButtons();
                toast("No hidden data found");

                insertHistory(
                        "DECODE",
                        getName(stegoUri),
                        stegoUri.toString(),
                        "NO_HIDDEN_DATA"
                );
                return;
            }

            // Read hidden file metadata
            int start = pos + markerBytes.length;
            ByteArrayOutputStream metaBuffer = new ByteArrayOutputStream();
            int i = start;
            boolean separatorFound = false;

            while (i < fileBytes.length - 1) {
                if (fileBytes[i] == ':' && fileBytes[i + 1] == ':') {
                    i += 2;
                    separatorFound = true;
                    break;
                }

                metaBuffer.write(fileBytes[i]);
                i++;
            }

            // Validate metadata separator
            if (!separatorFound) {
                tvResult.setText("Invalid SecureStegoX file");
                hideResultButtons();
                toast("Invalid encoded file");

                insertHistory(
                        "DECODE",
                        getName(stegoUri),
                        stegoUri.toString(),
                        "INVALID_METADATA"
                );
                return;
            }

            String fileMeta = new String(
                    metaBuffer.toByteArray(),
                    StandardCharsets.UTF_8
            );

            // Validate encrypted data
            if (i >= fileBytes.length) {
                tvResult.setText("No encrypted data found");
                hideResultButtons();
                toast("No encrypted data found");
                return;
            }

            byte[] encrypted = new byte[fileBytes.length - i];

            System.arraycopy(
                    fileBytes,
                    i,
                    encrypted,
                    0,
                    encrypted.length
            );

            // Decrypt hidden data using secret key
            try {
                decodedBytes = CryptoUtils.aesDecrypt(encrypted, key);
            } catch (Exception e) {
                decodedBytes = null;

                tvResult.setText("Wrong Secret Key");
                hideResultButtons();
                toast("Wrong Secret Key");

                insertHistory(
                        "DECODE",
                        getName(stegoUri),
                        stegoUri.toString(),
                        "WRONG_KEY"
                );
                return;
            }

            // Validate decrypted data
            if (decodedBytes == null || decodedBytes.length == 0) {
                tvResult.setText("Decryption failed");
                hideResultButtons();
                toast("Decryption failed");

                insertHistory(
                        "DECODE",
                        getName(stegoUri),
                        stegoUri.toString(),
                        "DECRYPT_FAILED"
                );
                return;
            }

            hideResultButtons();

            // Display decoded text
            if ("TEXT".equals(fileMeta)) {
                String text = new String(
                        decodedBytes,
                        StandardCharsets.UTF_8
                );

                tvResult.setText(
                        "DECODED TEXT\n\n" + text
                );
            } else {
                // Display recovered file information
                decodedFileName = fileMeta;

                tvResult.setText(
                        "Hidden file detected\n\n" +
                                "File: " + decodedFileName +
                                "\n\nYou can Save or Open the recovered file."
                );

                btnSave.setVisibility(View.VISIBLE);
                btnOpen.setVisibility(View.VISIBLE);
            }

            // Record successful decoding
            insertHistory(
                    "DECODE",
                    getName(stegoUri),
                    stegoUri.toString(),
                    "SUCCESS"
            );

            toast("✓ Decode successful");

        } catch (Exception e) {
            e.printStackTrace();

            tvResult.setText("Decode failed");
            hideResultButtons();
            toast("Decode failed");

            insertHistory(
                    "DECODE",
                    stegoUri != null ? getName(stegoUri) : "Unknown",
                    stegoUri != null ? stegoUri.toString() : "",
                    "FAILED"
            );
        }
    }

    // Save recovered hidden file
    private void saveFile() {
        try {
            if (decodedBytes == null || decodedFileName == null) {
                toast("Nothing to save");
                return;
            }

            ContentValues values = new ContentValues();

            values.put(
                    MediaStore.Files.FileColumns.DISPLAY_NAME,
                    decodedFileName
            );

            values.put(
                    MediaStore.Files.FileColumns.RELATIVE_PATH,
                    "Download/StegoSecureX"
            );

            decodedUri = getContentResolver().insert(
                    MediaStore.Files.getContentUri("external"),
                    values
            );

            if (decodedUri == null) {
                toast("Save failed");

                insertHistory(
                        "SAVE_DECODED_FILE",
                        decodedFileName,
                        "",
                        "FAILED"
                );
                return;
            }

            OutputStream outputStream =
                    getContentResolver().openOutputStream(decodedUri);

            if (outputStream == null) {
                toast("Unable to save file");
                return;
            }

            outputStream.write(decodedBytes);
            outputStream.flush();
            outputStream.close();

            toast("✓ Saved in Download/StegoSecureX");

            insertHistory(
                    "SAVE_DECODED_FILE",
                    decodedFileName,
                    decodedUri.toString(),
                    "SUCCESS"
            );

        } catch (Exception e) {
            e.printStackTrace();

            toast("Save failed");

            insertHistory(
                    "SAVE_DECODED_FILE",
                    decodedFileName != null ? decodedFileName : "Unknown",
                    "",
                    "FAILED"
            );
        }
    }

    // Open recovered file using suitable application
    private void openFile() {
        try {
            if (decodedUri == null) {
                toast("Save file first");
                return;
            }

            if (decodedFileName == null) {
                toast("File information unavailable");
                return;
            }

            int dot = decodedFileName.lastIndexOf(".");

            String extension = dot >= 0
                    ? decodedFileName.substring(dot + 1)
                    : "";

            String mime = MimeTypeMap.getSingleton()
                    .getMimeTypeFromExtension(extension.toLowerCase());

            if (mime == null) {
                mime = "*/*";
            }

            Intent intent = new Intent(Intent.ACTION_VIEW);

            intent.setDataAndType(
                    decodedUri,
                    mime
            );

            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(
                    Intent.createChooser(intent, "Open with")
            );

        } catch (Exception e) {
            e.printStackTrace();
            toast("No suitable app found to open this file");
        }
    }

    // Read complete file into byte array
    private byte[] read(Uri uri) throws Exception {
        InputStream inputStream =
                getContentResolver().openInputStream(uri);

        if (inputStream == null) {
            throw new Exception("Unable to open file");
        }

        ByteArrayOutputStream output =
                new ByteArrayOutputStream();

        byte[] buffer = new byte[4096];
        int length;

        while ((length = inputStream.read(buffer)) != -1) {
            output.write(buffer, 0, length);
        }

        inputStream.close();

        return output.toByteArray();
    }

    // Find marker inside byte array
    private int indexOf(byte[] data, byte[] pattern) {
        if (pattern.length == 0) {
            return 0;
        }

        if (data.length < pattern.length) {
            return -1;
        }

        outer:
        for (int i = 0; i <= data.length - pattern.length; i++) {
            for (int j = 0; j < pattern.length; j++) {
                if (data[i + j] != pattern[j]) {
                    continue outer;
                }
            }

            return i;
        }

        return -1;
    }

    // Get selected file name
    private String getName(Uri uri) {
        if (uri == null) {
            return "file";
        }

        Cursor cursor = getContentResolver().query(
                uri,
                null,
                null,
                null,
                null
        );

        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(
                            OpenableColumns.DISPLAY_NAME
                    );

                    if (index != -1) {
                        String name = cursor.getString(index);

                        if (name != null && !name.isEmpty()) {
                            return name;
                        }
                    }
                }
            } finally {
                cursor.close();
            }
        }

        String fallback = uri.getLastPathSegment();

        return fallback != null ? fallback : "file";
    }

    // Insert operation into history
    private void insertHistory(
            String action,
            String fileName,
            String filePath,
            String status) {

        if (currentUser == null || currentUser.trim().isEmpty()) {
            return;
        }

        try {
            db.insertHistory(
                    currentUser,
                    action,
                    fileName,
                    filePath,
                    status
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Hide Save and Open buttons
    private void hideResultButtons() {
        btnSave.setVisibility(View.GONE);
        btnOpen.setVisibility(View.GONE);
    }

    // Logout and clear activity stack
    private void logout() {
        Intent intent = new Intent(
                this,
                LoginActivity.class
        );

        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }

    // Display Toast message
    private void toast(String message) {
        Toast.makeText(
                this,
                message,
                Toast.LENGTH_SHORT
        ).show();
    }
}