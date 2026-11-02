package com.securestegox;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class HistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerHistory;

    private TextView tvHistoryUser;
    private TextView tvEmpty;

    private Button btnClearHistory;
    private Button btnBack;
    private Button btnLogout;

    private DatabaseHelper db;

    private HistoryAdapter adapter;

    private List<HistoryModel> historyList;

    private String username;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_history
        );

        username =
                getIntent()
                        .getStringExtra("username");

        if (username == null ||
                username.trim().isEmpty()) {

            username = "User";
        }

        initViews();

        setupRecyclerView();

        loadHistory();

        setListeners();
    }

    // =========================================================
    // INITIALIZE
    // =========================================================

    private void initViews() {

        recyclerHistory =
                findViewById(
                        R.id.recyclerHistory
                );

        tvHistoryUser =
                findViewById(
                        R.id.tvHistoryUser
                );

        tvEmpty =
                findViewById(
                        R.id.tvEmpty
                );

        btnClearHistory =
                findViewById(
                        R.id.btnClearHistory
                );

        btnBack =
                findViewById(
                        R.id.btnBack
                );

        btnLogout =
                findViewById(
                        R.id.btnLogout
                );

        tvHistoryUser.setText(
                "User: " + username
        );

        db =
                new DatabaseHelper(this);

        historyList =
                new ArrayList<>();
    }

    // =========================================================
    // RECYCLER VIEW
    // =========================================================

    private void setupRecyclerView() {

        recyclerHistory.setLayoutManager(
                new LinearLayoutManager(this)
        );

        adapter =
                new HistoryAdapter(historyList);

        recyclerHistory.setAdapter(adapter);
    }

    // =========================================================
    // LOAD HISTORY
    // =========================================================

    private void loadHistory() {

        historyList.clear();

        List<HistoryModel> data =
                db.getUserHistory(username);

        if (data != null &&
                !data.isEmpty()) {

            historyList.addAll(data);

            tvEmpty.setVisibility(
                    View.GONE
            );

            recyclerHistory.setVisibility(
                    View.VISIBLE
            );

        } else {

            tvEmpty.setVisibility(
                    View.VISIBLE
            );

            recyclerHistory.setVisibility(
                    View.GONE
            );
        }

        adapter.notifyDataSetChanged();
    }

    // =========================================================
    // BUTTONS
    // =========================================================

    private void setListeners() {

        // CLEAR HISTORY

        btnClearHistory.setOnClickListener(v -> {

            db.clearUserHistory(username);

            Toast.makeText(
                    this,
                    "History Cleared ✔",
                    Toast.LENGTH_SHORT
            ).show();

            loadHistory();
        });

        // BACK

        btnBack.setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            this,
                            DashboardActivity.class
                    );

            intent.putExtra(
                    "username",
                    username
            );

            startActivity(intent);

            finish();
        });

        // LOGOUT

        btnLogout.setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            this,
                            LoginActivity.class
                    );

            intent.setFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK |
                            Intent.FLAG_ACTIVITY_CLEAR_TASK
            );

            startActivity(intent);

            finish();
        });
    }
}