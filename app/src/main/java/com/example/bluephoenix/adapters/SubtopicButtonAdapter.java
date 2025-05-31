package com.example.bluephoenix.adapters; // Ensure this package is correct

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bluephoenix.R;

import java.util.List;

public class SubtopicButtonAdapter extends RecyclerView.Adapter<SubtopicButtonAdapter.SubtopicButtonViewHolder> {

    private List<String> subtopicNames;
    private OnSubtopicClickListener listener;

    public interface OnSubtopicClickListener {
        void onSubtopicClick(String subtopicName);
    }

    public SubtopicButtonAdapter(List<String> subtopicNames, OnSubtopicClickListener listener) {
        this.subtopicNames = subtopicNames;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SubtopicButtonViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_subtopic_button, parent, false);
        return new SubtopicButtonViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SubtopicButtonViewHolder holder, int position) {
        String subtopicName = subtopicNames.get(position);
        holder.subtopicButton.setText(subtopicName);

        holder.subtopicButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSubtopicClick(subtopicName);
            }
        });
    }

    @Override
    public int getItemCount() {
        return subtopicNames.size();
    }

    public void updateData(List<String> newSubtopicNames) {
        this.subtopicNames.clear();
        this.subtopicNames.addAll(newSubtopicNames);
        notifyDataSetChanged();
    }

    public static class SubtopicButtonViewHolder extends RecyclerView.ViewHolder {
        Button subtopicButton;

        public SubtopicButtonViewHolder(@NonNull View itemView) {
            super(itemView);
            subtopicButton = itemView.findViewById(R.id.subtopic_button);
        }
    }
}