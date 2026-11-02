package com.securestegox;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.webkit.MimeTypeMap;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class EncodeActivity extends AppCompatActivity {

    // UI
    private Button btnCarrier;
    private Button btnSecret;
    private Button btnEncode;
    private Button btnShareKey;
    private Button btnBack;
    private Button btnLogout;
    private EditText etMsg;
    private EditText etKey;
    private TextView tvCarrier;
    private TextView tvSecret;

    // FILE DATA
    private Uri carrierUri;
    private Uri secretUri;
    private Uri savedStegoUri;
    private byte[] stegoBytes;
    private String carrierName = "encoded_file";
    private String savedFileName = "";

    // STEGO MARKER
    private static final String MARKER =
            "\n---STEGOX---\n";

    // REQUEST CODES
    private static final int REQUEST_CARRIER = 1;
    private static final int REQUEST_SECRET = 2;

    // DATABASE
    private DatabaseHelper db;
    private String currentUser;
    // MESSAGE CHANGE FLAG
    private boolean changingMessage = false;

    // ON CREATE
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_encode);

        // DATABASE
        db = new DatabaseHelper(this);

        // LOGGED-IN USER
        currentUser =
                getIntent().getStringExtra("username");

        // CONNECT UI
        btnCarrier =
                findViewById(R.id.btnSelectCarrier);
        btnSecret =
                findViewById(R.id.btnSelectSecretFile);
        btnEncode =
                findViewById(R.id.btnEncode);
        btnShareKey =
                findViewById(R.id.btnShareKey);
        btnBack =
                findViewById(R.id.btnBack);
        btnLogout =
                findViewById(R.id.btnLogout);
        etMsg =
                findViewById(R.id.etMessage);
        etKey =
                findViewById(R.id.etSecretKey);
        tvCarrier =
                findViewById(R.id.tvCarrierFile);
        tvSecret =
                findViewById(R.id.tvSecretFile);

        // INITIAL STATE
        tvCarrier.setText(
                "No carrier file selected"
        );

        tvSecret.setText(
                "No secret file selected"
        );

        // Share Key disabled initially
        btnShareKey.setEnabled(false);

        // PASSWORD / MESSAGE EYE
        setupPasswordEye(etMsg);
        setupPasswordEye(etKey);

        // SECRET MESSAGE CHANGE
        etMsg.addTextChangedListener(
                new TextWatcher() {
                    @Override
                    public void beforeTextChanged(
                            CharSequence s,
                            int start,
                            int count,
                            int after) {
                    }
                    @Override
                    public void onTextChanged(
                            CharSequence s,
                            int start,
                            int before,
                            int count) {
                        if (changingMessage) {
                            return;
                        }
                        if (s.toString().trim().length() > 0 &&
                                secretUri != null) {
                            secretUri = null;
                            tvSecret.setText(
                                    "No secret file selected"
                            );
                            resetEncodedData();
                            toast(
                                    "Secret file removed. Using secret message."
                            );
                        }
                        // New message means old encoded file
                        // is no longer valid.
                        if (s.toString().trim().length() > 0) {
                            resetEncodedData();
                        }
                    }
                    @Override
                    public void afterTextChanged(
                            Editable s) {
                    }
                }
        );

        // SECRET KEY CHANGE
        etKey.addTextChangedListener(
                new TextWatcher() {
                    @Override
                    public void beforeTextChanged(
                            CharSequence s,
                            int start,
                            int count,
                            int after) {
                    }
                    @Override
                    public void onTextChanged(
                            CharSequence s,
                            int start,
                            int before,
                            int count) {
                        // Changing key invalidates
                        // previously encoded file.
                        resetEncodedData();
                    }

                    @Override
                    public void afterTextChanged(
                            Editable s) {
                    }
                }
        );

        // CARRIER
        btnCarrier.setOnClickListener(
                v -> pickFile(REQUEST_CARRIER)
        );

        // SECRET FILE
        btnSecret.setOnClickListener(
                v -> pickFile(REQUEST_SECRET)
        );

        // ENCODE
        btnEncode.setOnClickListener(
                v -> encode()
        );

        // SHARE KEY
        btnShareKey.setOnClickListener(
                v -> openShareKey()
        );

        // BACK
        btnBack.setOnClickListener(
                v -> goToDashboard()
        );

        // LOGOUT
        btnLogout.setOnClickListener(
                v -> logout()
        );
    }

    // PASSWORD / MESSAGE EYE
    private void setupPasswordEye(EditText editText) {
        editText.setOnTouchListener(
                (v, event) -> {
                    if (event.getAction() ==
                            MotionEvent.ACTION_UP) {
                        int drawableRight =
                                editText.getCompoundDrawables()[2] != null
                                        ? editText.getCompoundDrawables()[2]
                                        .getBounds()
                                        .width()
                                        : 0;
                        if (drawableRight > 0 &&
                                event.getX() >=
                                        editText.getWidth()
                                                - editText.getPaddingEnd()
                                                - drawableRight) {
                            int selection =
                                    editText.getSelectionStart();
                            boolean hidden =
                                    editText.getTransformationMethod()
                                            instanceof PasswordTransformationMethod;
                            if (hidden) {
                                // SHOW
                                editText.setTransformationMethod(
                                        HideReturnsTransformationMethod
                                                .getInstance()
                                );
                            } else {
                                // HIDE
                                editText.setTransformationMethod(
                                        PasswordTransformationMethod
                                                .getInstance()
                                );
                            }
                            editText.setSelection(
                                    Math.max(
                                            0,
                                            selection
                                    )
                            );
                            return true;
                        }
                    }
                    return false;
                }
        );
    }

    // =========================================================
    // PICK FILE
    // =========================================================

    private void pickFile(int requestCode) {

        Intent intent =
                new Intent(
                        Intent.ACTION_OPEN_DOCUMENT
                );

        intent.addCategory(
                Intent.CATEGORY_OPENABLE
        );

        intent.setType("*/*");

        intent.addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION |
                        Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
        );

        startActivityForResult(
                intent,
                requestCode
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

        if (resultCode != RESULT_OK ||
                data == null ||
                data.getData() == null) {

            return;
        }

        Uri selectedUri =
                data.getData();


        // =====================================================
        // TAKE PERSISTABLE PERMISSION
        // =====================================================

        try {

            getContentResolver()
                    .takePersistableUriPermission(
                            selectedUri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                    );

        } catch (Exception ignored) {
        }


        // =====================================================
        // CARRIER
        // =====================================================

        if (requestCode == REQUEST_CARRIER) {

            if (!validateReadableFile(selectedUri)) {

                toast(
                        "Unable to read selected carrier file."
                );

                return;
            }

            carrierUri =
                    selectedUri;

            carrierName =
                    getFileName(carrierUri);

            if (carrierName == null ||
                    carrierName.trim().isEmpty() ||
                    carrierName.equals("file")) {

                carrierName =
                        "encoded_file";
            }

            tvCarrier.setText(
                    carrierName
            );

            resetEncodedData();

            toast(
                    "Carrier file selected"
            );
        }


        // =====================================================
        // SECRET FILE
        // =====================================================

        else if (requestCode == REQUEST_SECRET) {

            if (!validateReadableFile(selectedUri)) {

                toast(
                        "Unable to read selected secret file."
                );

                return;
            }

            secretUri =
                    selectedUri;

            String secretFileName =
                    getFileName(secretUri);

            if (secretFileName == null ||
                    secretFileName.trim().isEmpty()) {

                secretFileName =
                        "secret_file";
            }

            tvSecret.setText(
                    secretFileName
            );


            // Clear secret message

            changingMessage = true;

            etMsg.setText("");

            changingMessage = false;


            resetEncodedData();

            toast(
                    "Secret file selected"
            );
        }
    }


    // =========================================================
    // ENCODE
    // =========================================================

    private void encode() {

        // =====================================================
        // USER VALIDATION
        // =====================================================

        if (currentUser == null ||
                currentUser.trim().isEmpty()) {

            toast(
                    "User session expired. Please login again."
            );

            return;
        }


        // =====================================================
        // CARRIER VALIDATION
        // =====================================================

        if (carrierUri == null) {

            toast(
                    "Please select a carrier file first."
            );

            return;
        }

        if (!validateReadableFile(carrierUri)) {

            toast(
                    "Selected carrier file cannot be read."
            );

            return;
        }


        // =====================================================
        // SECRET KEY VALIDATION
        // =====================================================

        String key =
                getSecretKey();

        if (key.isEmpty()) {

            etKey.setError(
                    "Enter secret key"
            );

            etKey.requestFocus();

            toast(
                    "Please enter the secret key."
            );

            return;
        }


        // =====================================================
        // KEY LENGTH VALIDATION
        // =====================================================

        if (key.length() < 4) {

            etKey.setError(
                    "Secret key must be at least 4 characters"
            );

            etKey.requestFocus();

            toast(
                    "Secret key must be at least 4 characters."
            );

            return;
        }


        // =====================================================
        // SECRET MESSAGE
        // =====================================================

        String message =
                etMsg.getText()
                        .toString()
                        .trim();

        boolean hasMessage =
                !message.isEmpty();


        // =====================================================
        // SECRET FILE
        // =====================================================

        boolean hasSecretFile =
                secretUri != null;


        // =====================================================
        // BOTH SELECTED
        // =====================================================

        if (hasMessage &&
                hasSecretFile) {

            toast(
                    "Use either Secret File OR Secret Message."
            );

            return;
        }


        // =====================================================
        // NOTHING SELECTED
        // =====================================================

        if (!hasSecretFile &&
                !hasMessage) {

            toast(
                    "Select a secret file or enter a secret message."
            );

            return;
        }


        // =====================================================
        // RESET OLD DATA
        // =====================================================

        savedStegoUri = null;
        savedFileName = "";
        stegoBytes = null;

        btnShareKey.setEnabled(false);

        btnEncode.setEnabled(false);


        try {

            // =================================================
            // READ CARRIER
            // =================================================

            byte[] carrier =
                    readBytes(carrierUri);

            if (carrier.length == 0) {

                throw new IOException(
                        "Carrier file is empty."
                );
            }


            // =================================================
            // READ SECRET
            // =================================================

            byte[] secret;

            String fileMeta;


            if (hasSecretFile) {

                if (!validateReadableFile(secretUri)) {

                    throw new IOException(
                            "Secret file cannot be read."
                    );
                }

                secret =
                        readBytes(secretUri);

                if (secret.length == 0) {

                    throw new IOException(
                            "Secret file is empty."
                    );
                }

                fileMeta =
                        getFileName(secretUri);

                if (fileMeta == null ||
                        fileMeta.trim().isEmpty()) {

                    fileMeta =
                            "secret_file";
                }

            } else {

                secret =
                        message.getBytes(
                                StandardCharsets.UTF_8
                        );

                if (secret.length == 0) {

                    throw new IOException(
                            "Secret message is empty."
                    );
                }

                fileMeta =
                        "TEXT";
            }


            // =================================================
            // ENCRYPT
            // =================================================

            byte[] encrypted =
                    CryptoUtils.aesEncrypt(
                            secret,
                            key
                    );

            if (encrypted == null ||
                    encrypted.length == 0) {

                throw new IOException(
                        "Encryption failed."
                );
            }


            // =================================================
            // CREATE STEGO DATA
            // =================================================

            ByteArrayOutputStream bos =
                    new ByteArrayOutputStream(
                            carrier.length +
                                    MARKER.length() +
                                    fileMeta.length() +
                                    encrypted.length +
                                    10
                    );

            bos.write(carrier);

            bos.write(
                    MARKER.getBytes(
                            StandardCharsets.UTF_8
                    )
            );

            bos.write(
                    (fileMeta + "::")
                            .getBytes(
                                    StandardCharsets.UTF_8
                            )
            );

            bos.write(encrypted);

            stegoBytes =
                    bos.toByteArray();


            // =================================================
            // VALIDATE GENERATED DATA
            // =================================================

            if (stegoBytes.length <=
                    carrier.length) {

                throw new IOException(
                        "Stego data generation failed."
                );
            }


            // =================================================
            // SAVE
            // =================================================

            toast(
                    "Encoding successful. Saving file..."
            );

            boolean saved =
                    saveFile();

            if (saved) {

                toast(
                        "Encoded file saved successfully."
                );

            } else {

                toast(
                        "Encoded file could not be saved."
                );
            }

        } catch (Exception e) {

            e.printStackTrace();

            stegoBytes = null;
            savedStegoUri = null;
            savedFileName = "";

            btnShareKey.setEnabled(false);

            insertHistory(
                    "ENCODE",
                    carrierName,
                    carrierUri != null
                            ? carrierUri.toString()
                            : "",
                    "FAILED"
            );

            toast(
                    "Encoding failed."
            );

        } finally {

            if (!isFinishing()) {

                btnEncode.setEnabled(true);
            }
        }
    }


    // =========================================================
    // SAVE ENCODED FILE
    // =========================================================

    private boolean saveFile() {

        if (stegoBytes == null ||
                stegoBytes.length == 0) {

            toast(
                    "Please encode the file first."
            );

            return false;
        }


        if (currentUser == null ||
                currentUser.trim().isEmpty()) {

            toast(
                    "User session error. Please login again."
            );

            return false;
        }


        String key =
                getSecretKey();

        if (key.isEmpty()) {

            toast(
                    "Secret key is missing."
            );

            return false;
        }


        if (carrierUri == null) {

            toast(
                    "Carrier file is missing."
            );

            return false;
        }


        Uri savedUri = null;


        try {

            // =================================================
            // OUTPUT NAME
            // =================================================

            String outputName =
                    createOutputFileName(
                            carrierName
                    );


            // =================================================
            // MIME
            // =================================================

            String mimeType =
                    getMimeType(
                            carrierUri,
                            outputName
                    );


            // =================================================
            // VALUES
            // =================================================

            ContentValues values =
                    new ContentValues();

            values.put(
                    MediaStore.Files.FileColumns.DISPLAY_NAME,
                    outputName
            );

            values.put(
                    MediaStore.Files.FileColumns.MIME_TYPE,
                    mimeType
            );

            values.put(
                    MediaStore.Files.FileColumns.RELATIVE_PATH,
                    Environment.DIRECTORY_DOWNLOADS +
                            "/StegoSecureX"
            );


            // =================================================
            // CREATE
            // =================================================

            Uri collection =
                    MediaStore.Files.getContentUri(
                            "external"
                    );

            savedUri =
                    getContentResolver().insert(
                            collection,
                            values
                    );

            if (savedUri == null) {

                throw new IOException(
                        "Unable to create output file."
                );
            }


            // =================================================
            // WRITE
            // =================================================

            try (
                    OutputStream outputStream =
                            getContentResolver()
                                    .openOutputStream(
                                            savedUri
                                    )
            ) {

                if (outputStream == null) {

                    throw new IOException(
                            "Output stream unavailable."
                    );
                }

                outputStream.write(
                        stegoBytes
                );

                outputStream.flush();
            }


            // =================================================
            // VERIFY
            // =================================================

            if (!verifySavedFile(savedUri)) {

                throw new IOException(
                        "Saved file could not be verified."
                );
            }


            // =================================================
            // VALIDATE STEGO
            // =================================================

            if (!validateStegoFile(savedUri)) {

                throw new IOException(
                        "Invalid SecureStegoX file."
                );
            }


            // =================================================
            // DATABASE
            // =================================================

            boolean savedInDatabase =
                    db.saveFile(
                            currentUser,
                            outputName,
                            key
                    );


            if (!savedInDatabase) {

                try {

                    getContentResolver().delete(
                            savedUri,
                            null,
                            null
                    );

                } catch (Exception ignored) {
                }


                insertHistory(
                        "ENCODE",
                        outputName,
                        "",
                        "DB_FAILED"
                );

                toast(
                        "File save failed in database."
                );

                return false;
            }


            // =================================================
            // SUCCESS INFO
            // =================================================

            savedStegoUri =
                    savedUri;

            savedFileName =
                    outputName;


            // =================================================
            // HISTORY
            // =================================================

            insertHistory(
                    "ENCODE",
                    outputName,
                    savedUri.toString(),
                    "SUCCESS"
            );


            // =================================================
            // ENABLE SHARE KEY
            // =================================================

            btnShareKey.setEnabled(true);


            // =================================================
            // CLEAR MEMORY
            // =================================================

            stegoBytes = null;


            return true;


        } catch (Exception e) {

            e.printStackTrace();


            if (savedUri != null) {

                try {

                    getContentResolver().delete(
                            savedUri,
                            null,
                            null
                    );

                } catch (Exception ignored) {
                }
            }


            insertHistory(
                    "ENCODE",
                    carrierName,
                    "",
                    "FAILED"
            );


            toast(
                    "Save failed."
            );

            return false;
        }
    }


    // =========================================================
    // SHARE KEY
    // =========================================================

    private void openShareKey() {

        // =====================================================
        // FIRST VALIDATION
        // =====================================================

        if (currentUser == null ||
                currentUser.trim().isEmpty()) {

            toast(
                    "User session expired. Please login again."
            );

            return;
        }


        // =====================================================
        // ENCODE VALIDATION
        // =====================================================

        if (savedStegoUri == null ||
                savedFileName == null ||
                savedFileName.trim().isEmpty()) {

            toast(
                    "Please encode and save the file first."
            );

            return;
        }


        // =====================================================
        // FILE VALIDATION
        // =====================================================

        if (!validateReadableFile(savedStegoUri)) {

            btnShareKey.setEnabled(false);

            savedStegoUri = null;
            savedFileName = "";

            toast(
                    "Saved encoded file is not accessible."
            );

            return;
        }


        // =====================================================
        // STEGO VALIDATION
        // =====================================================

        if (!validateStegoFile(savedStegoUri)) {

            btnShareKey.setEnabled(false);

            savedStegoUri = null;
            savedFileName = "";

            toast(
                    "Please encode the file first."
            );

            return;
        }


        // =====================================================
        // OPEN SHARE KEY
        // =====================================================

        Intent intent =
                new Intent(
                        EncodeActivity.this,
                        ShareKeyActivity.class
                );

        intent.putExtra(
                "username",
                currentUser
        );

        intent.putExtra(
                "encodedFileUri",
                savedStegoUri.toString()
        );

        intent.putExtra(
                "encodedFileName",
                savedFileName
        );

        startActivity(intent);
    }


    // =========================================================
    // VALIDATE STEGO FILE
    // =========================================================

    private boolean validateStegoFile(Uri uri) {

        if (uri == null) {
            return false;
        }

        try (
                InputStream input =
                        getContentResolver()
                                .openInputStream(uri)
        ) {

            if (input == null) {
                return false;
            }

            byte[] markerBytes =
                    MARKER.getBytes(
                            StandardCharsets.UTF_8
                    );

            int matched = 0;

            int value;


            while ((value = input.read()) != -1) {

                if ((byte) value ==
                        markerBytes[matched]) {

                    matched++;

                    if (matched ==
                            markerBytes.length) {

                        return true;
                    }

                } else {

                    matched = 0;
                }
            }

        } catch (Exception e) {

            e.printStackTrace();
        }

        return false;
    }


    // =========================================================
    // CREATE OUTPUT FILE NAME
    // =========================================================

    private String createOutputFileName(
            String originalName) {

        if (originalName == null ||
                originalName.trim().isEmpty() ||
                originalName.equals("file")) {

            return "SecureStegoX_Encoded.bin";
        }


        String name =
                originalName.trim();

        int dotIndex =
                name.lastIndexOf(".");


        if (dotIndex > 0 &&
                dotIndex < name.length() - 1) {

            String baseName =
                    name.substring(
                            0,
                            dotIndex
                    );

            String extension =
                    name.substring(
                            dotIndex
                    );

            return baseName +
                    "_SecureStegoX" +
                    extension;
        }


        return name +
                "_SecureStegoX";
    }


    // =========================================================
    // GET MIME TYPE
    // =========================================================

    private String getMimeType(
            Uri uri,
            String fileName) {

        if (uri != null) {

            try {

                String type =
                        getContentResolver()
                                .getType(uri);

                if (type != null &&
                        !type.trim().isEmpty() &&
                        !type.equals("*/*")) {

                    return type;
                }

            } catch (Exception ignored) {
            }
        }


        String extension =
                MimeTypeMap.getFileExtensionFromUrl(
                        fileName
                );


        if (extension != null &&
                !extension.isEmpty()) {

            String mime =
                    MimeTypeMap.getSingleton()
                            .getMimeTypeFromExtension(
                                    extension.toLowerCase()
                            );

            if (mime != null &&
                    !mime.isEmpty()) {

                return mime;
            }
        }


        return "application/octet-stream";
    }


    // =========================================================
    // VERIFY SAVED FILE
    // =========================================================

    private boolean verifySavedFile(Uri uri) {

        if (uri == null) {
            return false;
        }

        try {

            Cursor cursor =
                    getContentResolver().query(
                            uri,
                            new String[]{
                                    OpenableColumns.SIZE
                            },
                            null,
                            null,
                            null
                    );


            if (cursor != null) {

                try {

                    if (cursor.moveToFirst()) {

                        int index =
                                cursor.getColumnIndex(
                                        OpenableColumns.SIZE
                                );

                        if (index >= 0 &&
                                !cursor.isNull(index)) {

                            long actualLength =
                                    cursor.getLong(index);

                            if (stegoBytes != null) {

                                return actualLength ==
                                        stegoBytes.length;
                            }
                        }
                    }

                } finally {

                    cursor.close();
                }
            }


            // Fallback

            try (
                    InputStream input =
                            getContentResolver()
                                    .openInputStream(uri)
            ) {

                return input != null;
            }

        } catch (Exception e) {

            e.printStackTrace();

            return false;
        }
    }


    // =========================================================
    // VALIDATE READABLE FILE
    // =========================================================

    private boolean validateReadableFile(Uri uri) {

        if (uri == null) {
            return false;
        }

        try (
                InputStream inputStream =
                        getContentResolver()
                                .openInputStream(uri)
        ) {

            if (inputStream == null) {
                return false;
            }

            return inputStream.read() != -1;

        } catch (Exception e) {

            return false;
        }
    }


    // =========================================================
    // RESET ENCODED DATA
    // =========================================================

    private void resetEncodedData() {

        stegoBytes = null;

        savedStegoUri = null;

        savedFileName = "";

        if (btnShareKey != null) {

            btnShareKey.setEnabled(false);
        }
    }


    // =========================================================
    // GET SECRET KEY
    // =========================================================

    private String getSecretKey() {

        if (etKey == null) {
            return "";
        }

        return etKey.getText()
                .toString()
                .trim();
    }


    // =========================================================
    // READ FILE BYTES
    // =========================================================

    private byte[] readBytes(
            Uri uri) throws Exception {

        if (uri == null) {

            throw new IOException(
                    "File URI is null."
            );
        }


        ContentResolver resolver =
                getContentResolver();


        try (
                InputStream inputStream =
                        resolver.openInputStream(uri);

                ByteArrayOutputStream buffer =
                        new ByteArrayOutputStream()
        ) {

            if (inputStream == null) {

                throw new IOException(
                        "Unable to open selected file."
                );
            }


            byte[] data =
                    new byte[8192];

            int bytesRead;


            while (
                    (bytesRead =
                            inputStream.read(data)) != -1
            ) {

                if (bytesRead > 0) {

                    buffer.write(
                            data,
                            0,
                            bytesRead
                    );
                }
            }


            return buffer.toByteArray();
        }
    }


    // =========================================================
    // GET FILE NAME
    // =========================================================

    private String getFileName(
            Uri uri) {

        if (uri == null) {
            return "file";
        }


        Cursor cursor = null;


        try {

            cursor =
                    getContentResolver().query(
                            uri,
                            new String[]{
                                    OpenableColumns.DISPLAY_NAME
                            },
                            null,
                            null,
                            null
                    );


            if (cursor != null &&
                    cursor.moveToFirst()) {

                int index =
                        cursor.getColumnIndex(
                                OpenableColumns.DISPLAY_NAME
                        );


                if (index >= 0) {

                    String name =
                            cursor.getString(index);


                    if (name != null &&
                            !name.trim().isEmpty()) {

                        return name;
                    }
                }
            }

        } catch (Exception e) {

            e.printStackTrace();

        } finally {

            if (cursor != null) {

                cursor.close();
            }
        }


        String path =
                uri.getLastPathSegment();


        if (path != null &&
                !path.trim().isEmpty()) {

            return path;
        }


        return "file";
    }


    // =========================================================
    // INSERT HISTORY
    // =========================================================

    private void insertHistory(
            String operation,
            String fileName,
            String filePath,
            String status) {

        if (db == null) {
            return;
        }

        if (currentUser == null ||
                currentUser.trim().isEmpty()) {

            return;
        }


        try {

            db.insertHistory(
                    currentUser,
                    operation,
                    fileName,
                    filePath,
                    status
            );

        } catch (Exception e) {

            e.printStackTrace();
        }
    }


    // =========================================================
    // DASHBOARD
    // =========================================================

    private void goToDashboard() {

        Intent intent =
                new Intent(
                        EncodeActivity.this,
                        DashboardActivity.class
                );

        intent.putExtra(
                "username",
                currentUser
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
                        EncodeActivity.this,
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
    // TOAST
    // =========================================================

    private void toast(
            String message) {

        Toast.makeText(
                EncodeActivity.this,
                message,
                Toast.LENGTH_SHORT
        ).show();
    }


    // =========================================================
    // DESTROY
    // =========================================================

    @Override
    protected void onDestroy() {

        stegoBytes = null;

        if (db != null) {

            db.close();

            db = null;
        }

        super.onDestroy();
    }
}