package com.securestegox;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class HistoryAdapter
        extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private List<HistoryModel> historyList;

    public HistoryAdapter(
            List<HistoryModel> historyList) {

        this.historyList = historyList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType) {

        View view =
                LayoutInflater.from(parent.getContext())
                        .inflate(
                                R.layout.item_history,
                                parent,
                                false
                        );

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ViewHolder holder,
            int position) {

        HistoryModel model =
                historyList.get(position);

        holder.tvOperation.setText(
                model.getOperation()
        );

        holder.tvFileName.setText(
                "File / Method: " +
                        model.getFileName()
        );

        holder.tvDateTime.setText(
                model.getDateTime()
        );

        holder.tvStatus.setText(
                "Status: " +
                        model.getStatus()
        );
    }

    @Override
    public int getItemCount() {

        return historyList.size();
    }

    public static class ViewHolder
            extends RecyclerView.ViewHolder {

        TextView tvOperation;
        TextView tvFileName;
        TextView tvDateTime;
        TextView tvStatus;

        public ViewHolder(
                @NonNull View itemView) {

            super(itemView);

            tvOperation =
                    itemView.findViewById(
                            R.id.tvOperation
                    );

            tvFileName =
                    itemView.findViewById(
                            R.id.tvFileName
                    );

            tvDateTime =
                    itemView.findViewById(
                            R.id.tvDateTime
                    );

            tvStatus =
                    itemView.findViewById(
                            R.id.tvStatus
                    );
        }
    }
}