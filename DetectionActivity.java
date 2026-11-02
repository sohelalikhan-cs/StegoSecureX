package com.securestegox;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import java.io.*;
public class DetectionActivity extends AppCompatActivity {
    Button btnSelectFile, btnDetect, btnBack, btnLogout;
    TextView tvFile, tvResult;
    Uri fileUri;
    static final int PICK = 101;
    final String MARKER = "\n---STEGOX---\n";
    DatabaseHelper db;
    String currentUser;
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_detection);
        db = new DatabaseHelper(this);
        currentUser = getIntent().getStringExtra("username");
        btnSelectFile = findViewById(R.id.btnSelectFile);
        btnDetect     = findViewById(R.id.btnDetect);
        tvFile        = findViewById(R.id.tvFile);
        tvResult      = findViewById(R.id.tvResult);
        btnBack       = findViewById(R.id.btnBack);
        btnLogout     = findViewById(R.id.btnLogout);
        btnSelectFile.setOnClickListener(v -> pick());
        btnDetect.setOnClickListener(v -> detect());
        btnBack.setOnClickListener(v -> {
            Intent i = new Intent(this, DashboardActivity.class);
            i.putExtra("username", currentUser);
            startActivity(i);
            finish();
        });
        btnLogout.setOnClickListener(v -> {
            Intent i = new Intent(this, LoginActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
        });
    }
    // ================= FILE PICK =================
    private void pick() {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.setType("*/*");
        startActivityForResult(i, PICK);
    }

    @Override
    protected void onActivityResult(int r, int c, Intent d) {
        super.onActivityResult(r, c, d);
        if (c == RESULT_OK && d != null) {
            fileUri = d.getData();
            tvFile.setText(getName(fileUri));
            tvResult.setText("");
        }
    }
    // ================= DETECT =================
    private void detect() {
        try {
            if (fileUri == null) {
                toast("Select file first");
                return;
            }
            byte[] data = read(fileUri);
            if (data == null || data.length == 0) {
                tvResult.setText("Invalid or empty file ");
                logHistory("INVALID_FILE");
                return;
            }
            byte[] markerBytes = MARKER.getBytes();
            int pos = indexOf(data, markerBytes);
            if (pos == -1) {
                tvResult.setText("📄 NORMAL FILE\nNo hidden data found ");
                logHistory("NO_HIDDEN_DATA");
                return;
            }
            int dataStart = pos + markerBytes.length;
            if (dataStart >= data.length) {
                tvResult.setText("⚠ Corrupted Stego File");
                logHistory("CORRUPTED_FILE");
                return;
            }
            tvResult.setText(
                    "🔐 STEGO FILE DETECTED ✔\n" +
                            "Hidden data present after marker\n\n" +
                            "Marker Position: " + pos
            );
            logHistory("STEGO_DETECTED");
        } catch (Exception e) {
            e.printStackTrace();
            toast("Detection failed ");
            logHistory("FAILED");
        }
    }
    // ================= HISTORY LOGGER =================
    private void logHistory(String status) {
        if (currentUser != null && fileUri != null) {
            db.insertHistory(
                    currentUser,
                    "DETECTION",
                    getName(fileUri),
                    fileUri.toString(),
                    status
            );
        }
    }
    // ================= UTIL =================
    private byte[] read(Uri u) {
        try (InputStream in = getContentResolver().openInputStream(u);
             ByteArrayOutputStream b = new ByteArrayOutputStream()) {
            if (in == null) return null;
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) != -1) {
                b.write(buf, 0, n);
            }
            return b.toByteArray();
        } catch (Exception e) {
            return null;
        }
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
    private String getName(Uri u) {
        Cursor c = getContentResolver().query(u, null, null, null, null);
        if (c != null && c.moveToFirst()) {
            @SuppressLint("Range")
            String n = c.getString(
                    c.getColumnIndex(OpenableColumns.DISPLAY_NAME));
            c.close();
            return n;
        }
        return "file";
    }
    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }
}
