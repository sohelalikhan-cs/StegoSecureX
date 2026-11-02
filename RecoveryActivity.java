package com.securestegox;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
public class RecoveryActivity extends AppCompatActivity {
    private static final int PICK_FILE_REQUEST = 101;
    private static final String MARKER = "\n---STEGOX---\n";
    private EditText etUsername, etPassword, etNewKey;
    private TextView tvSelectedFile;
    private Button btnSelectFile, btnRecover;
    private Button btnBack, btnLogout;
    private Uri fileUri;
    private String fileName;
    private DatabaseHelper db;
    // ✅ Added for session + history
    private String currentUser;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recovery);
        db = new DatabaseHelper(this);
        // ✅ Get logged in user from intent
        currentUser = getIntent().getStringExtra("username");
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        etNewKey   = findViewById(R.id.etNewKey);
        tvSelectedFile = findViewById(R.id.tvSelectedFile);
        btnSelectFile = findViewById(R.id.btnSelectFile);
        btnRecover    = findViewById(R.id.btnRecover);
        btnBack       = findViewById(R.id.btnBack);
        btnLogout     = findViewById(R.id.btnLogout);
        btnSelectFile.setOnClickListener(v -> selectFile());
        btnRecover.setOnClickListener(v -> recoverKey());
        btnBack.setOnClickListener(v -> {
            Intent i = new Intent(this, DashboardActivity.class);
            i.putExtra("username", currentUser);
            startActivity(i);
            finish();
        });
        btnLogout.setOnClickListener(v -> {
            Intent intent = new Intent(RecoveryActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            toast("Logged out successfully");
        });
    }
    // ================= FILE SELECT =================
    private void selectFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION |
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        startActivityForResult(intent, PICK_FILE_REQUEST);
    }
    @Override
    protected void onActivityResult(int requestCode,
                                    int resultCode,
                                    @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_FILE_REQUEST &&
                resultCode == RESULT_OK &&
                data != null) {
            fileUri = data.getData();
            if (fileUri != null) {
                try {
                    getContentResolver().takePersistableUriPermission(
                            fileUri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION |
                                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    );
                } catch (Exception ignored) {}
                fileName = getFileName(fileUri);
                tvSelectedFile.setText("Selected: " + fileName);
                toast("File Selected ✔");
            }
        }
    }
    // ================= RECOVERY =================
    private void recoverKey() {
        try {
            String username = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            String newKey   = etNewKey.getText().toString().trim();
            if (fileUri == null) {
                toast("Select file first");
                return;
            }
            if (username.isEmpty() || password.isEmpty() || newKey.isEmpty()) {
                toast("Fill all fields");
                return;
            }
            if (!db.checkUser(username, password)) {
                toast("Invalid Username or Password");
                logHistory("INVALID_LOGIN");
                return;
            }
            String oldKey = db.getKey(username, fileName);
            if (oldKey == null) {
                toast("No key found for this file");
                logHistory("KEY_NOT_FOUND");
                return;
            }
            byte[] fileBytes = readBytes(fileUri);
            byte[] markerBytes = MARKER.getBytes("UTF-8");
            int markerPos = indexOf(fileBytes, markerBytes);
            if (markerPos == -1) {
                toast("Invalid Stego File");
                logHistory("INVALID_STEGO_FILE");
                return;
            }
            int metaStart = markerPos + markerBytes.length;
            ByteArrayOutputStream metaBuffer = new ByteArrayOutputStream();
            int i = metaStart;
            while (i < fileBytes.length - 1) {
                if (fileBytes[i] == ':' && fileBytes[i + 1] == ':') {
                    i += 2;
                    break;
                }
                metaBuffer.write(fileBytes[i]);
                i++;
            }
            if (i >= fileBytes.length) {
                toast("Corrupted Stego File");
                logHistory("CORRUPTED_FILE");
                return;
            }
            String fileMeta = new String(metaBuffer.toByteArray(), "UTF-8");
            byte[] encryptedData = new byte[fileBytes.length - i];
            System.arraycopy(fileBytes, i, encryptedData, 0, encryptedData.length);
            if (encryptedData.length == 0) {
                toast("No encrypted data found");
                logHistory("NO_ENCRYPTED_DATA");
                return;
            }
            byte[] originalData = CryptoUtils.aesDecrypt(encryptedData, oldKey);
            if (originalData == null) {
                toast("Old key invalid");
                logHistory("OLD_KEY_INVALID");
                return;
            }
            byte[] newEncrypted = CryptoUtils.aesEncrypt(originalData, newKey);
            if (newEncrypted == null) {
                toast("Encryption failed");
                logHistory("ENCRYPTION_FAILED");
                return;
            }
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bos.write(fileBytes, 0, metaStart);
            bos.write((fileMeta + "::").getBytes("UTF-8"));
            bos.write(newEncrypted);
            byte[] updatedFile = bos.toByteArray();
            OutputStream os = getContentResolver()
                    .openOutputStream(fileUri, "rwt");
            if (os == null) {
                toast("Write permission denied");
                logHistory("WRITE_PERMISSION_DENIED");
                return;
            }
            os.write(updatedFile);
            os.flush();
            os.close();
            boolean updated = db.updateKey(username, fileName, newKey);
            if (!updated) {
                toast("Database update failed");
                logHistory("DB_UPDATE_FAILED");
                return;
            }
            toast("Recovery Successful ✔ New Key Applied");
            logHistory("RECOVERY_SUCCESS");
        } catch (Exception e) {
            e.printStackTrace();
            toast("Recovery Failed ❌");
            logHistory("RECOVERY_FAILED");
        }
    }
    // ================= HISTORY LOGGER =================
    private void logHistory(String status) {
        if (currentUser != null && fileUri != null) {
            db.insertHistory(
                    currentUser,
                    "RECOVERY",
                    fileName,
                    fileUri.toString(),
                    status
            );
        }
    }
    // ================= UTIL METHODS =================
    private byte[] readBytes(Uri uri) throws Exception {
        InputStream is = getContentResolver().openInputStream(uri);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[4096];
        int nRead;
        while ((nRead = is.read(data)) != -1) {
            buffer.write(data, 0, nRead);
        }
        is.close();
        return buffer.toByteArray();
    }
    private int indexOf(byte[] data, byte[] pattern) {
        outer:
        for (int i = 0; i <= data.length - pattern.length; i++) {
            for (int j = 0; j < pattern.length; j++) {
                if (data[i + j] != pattern[j])
                    continue outer;
            }
            return i;
        }
        return -1;
    }
    private String getFileName(Uri uri) {
        Cursor cursor = getContentResolver()
                .query(uri, null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            int index = cursor.getColumnIndex(
                    OpenableColumns.DISPLAY_NAME);
            if (index != -1) {
                String name = cursor.getString(index);
                cursor.close();
                return name;
            }
            cursor.close();
        }
        return "file";
    }
    private void toast(String msg) {
        Toast.makeText(this, msg,
                Toast.LENGTH_SHORT).show();
    }
}
