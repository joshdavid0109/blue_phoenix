package com.example.bluephoenix.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView; // If you want to customize the title based on the button

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bluephoenix.R;
import com.example.bluephoenix.models.ReviewerButton;

import java.util.List;

public class ReviewerButtonAdapter extends RecyclerView.Adapter<ReviewerButtonAdapter.ButtonViewHolder> {

    private List<ReviewerButton> buttonList;
    private OnButtonClickListener listener;

    public interface OnButtonClickListener {
        void onButtonClick(int position, ReviewerButton button);
        void onHeaderTitleChange(String title1, String title2); // Callback to change titles
    }

    public ReviewerButtonAdapter(List<ReviewerButton> buttonList, OnButtonClickListener listener) {
        this.buttonList = buttonList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ButtonViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_reviewer_button, parent, false);
        return new ButtonViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ButtonViewHolder holder, int position) {
        ReviewerButton currentButton = buttonList.get(position);
        holder.button.setText(currentButton.getButtonText());

        // Set an OnClickListener for each button
        holder.button.setOnClickListener(v -> {
            if (listener != null) {
                listener.onButtonClick(position, currentButton);
            }
        });

        // Example of changing titles based on the first item (or any logic you prefer)
        // This is just an example; you might want to handle title changes differently.
        if (position == 0) {
            if (listener != null) {
                // Here, you would pass the specific titles for the current section.
                // For demonstration, let's just use placeholder strings.
                listener.onHeaderTitleChange("Civil Procedure", "Remedial Law");
            }
        }
    }

    @Override
    public int getItemCount() {
        return buttonList.size();
    }

    public static class ButtonViewHolder extends RecyclerView.ViewHolder {
        Button button;

        public ButtonViewHolder(@NonNull View itemView) {
            super(itemView);
            button = itemView.findViewById(R.id.reviewer_button_item);
        }
    }

    // Method to update the data in the adapter
    public void updateData(List<ReviewerButton> newList) {
        this.buttonList.clear();
        this.buttonList.addAll(newList);
        notifyDataSetChanged();
    }
}