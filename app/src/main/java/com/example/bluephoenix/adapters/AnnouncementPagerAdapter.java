package com.example.bluephoenix.adapters; // Replace with your actual package name

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bluephoenix.R;
import com.example.bluephoenix.models.AnnouncementItem;

import java.util.List;

public class AnnouncementPagerAdapter extends RecyclerView.Adapter<AnnouncementPagerAdapter.ViewHolder> {

    private List<AnnouncementItem> announcementList;

    public AnnouncementPagerAdapter(List<AnnouncementItem> announcementList) {
        this.announcementList = announcementList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_announcement_page, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AnnouncementItem item = announcementList.get(position);

        if (item.isImage()) {
            holder.imageView.setVisibility(View.VISIBLE);
            holder.textView.setVisibility(View.GONE);
            holder.imageView.setImageResource(item.getImageResId());
            // If you are loading images from URLs (e.g., with Glide or Picasso), you'd do it here
            // Glide.with(holder.itemView.getContext()).load(item.getImageUrl()).into(holder.imageView);
        } else {
            holder.imageView.setVisibility(View.GONE);
            holder.textView.setVisibility(View.VISIBLE);
            holder.textView.setText(item.getTextContent());
        }
    }

    @Override
    public int getItemCount() {
        return announcementList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView textView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.announcement_page_image);
            textView = itemView.findViewById(R.id.announcement_page_text);
        }
    }
}