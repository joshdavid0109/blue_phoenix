package com.example.bluephoenix.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView; // Make sure to import ImageView
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide; // Assuming you are using Glide for image loading
import com.example.bluephoenix.R;
import com.example.bluephoenix.models.ChapterContentItem;

import java.util.List;

// Import for FastScrollRecyclerView.SectionedAdapter
import com.l4digital.fastscroll.FastScrollRecyclerView;


public class ChapterContentAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
        implements RecyclerView.SmoothScroller // Implement this interface
{

    private List<ChapterContentItem> chapterContentList;

    public ChapterContentAdapter(List<ChapterContentItem> chapterContentList) {
        this.chapterContentList = chapterContentList;
    }

    // region ViewHolder classes
    // ViewHolder for chapter title/header items
    public static class ChapterTitleViewHolder extends RecyclerView.ViewHolder {
        public TextView chapterTitleTextView;

        public ChapterTitleViewHolder(View itemView) {
            super(itemView);
            chapterTitleTextView = itemView.findViewById(R.id.chapter_title_text_view); // Make sure this ID is correct for your header layout
        }
    }

    // ViewHolder for regular text content items
    public static class ContentTextViewHolder extends RecyclerView.ViewHolder {
        public TextView contentTextView;

        public ContentTextViewHolder(View itemView) {
            super(itemView);
            contentTextView = itemView.findViewById(R.id.content_text_view); // Make sure this ID is correct for your text content layout
        }
    }

    // ViewHolder for image items
    public static class ImageViewHolder extends RecyclerView.ViewHolder {
        public ImageView contentImageView;
        public TextView imageCaptionTextView;

        public ImageViewHolder(View itemView) {
            super(itemView);
            contentImageView = itemView.findViewById(R.id.image_content_view); // Make sure this ID is correct for your image layout
            imageCaptionTextView = itemView.findViewById(R.id.image_caption_view); // Make sure this ID is correct for your image layout
        }
    }
    // endregion

    @Override
    public int getItemViewType(int position) {
        // Return the viewType from your ChapterContentItem model
        return chapterContentList.get(position).viewType;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view;

        switch (viewType) {
            case ChapterContentItem.VIEW_TYPE_CHAPTER_TITLE:
                view = inflater.inflate(R.layout.item_chapter_title, parent, false); // Your header layout
                return new ChapterTitleViewHolder(view);
            case ChapterContentItem.VIEW_TYPE_TEXT:
                view = inflater.inflate(R.layout.item_content_text, parent, false); // Your text content layout
                return new ContentTextViewHolder(view);
            case ChapterContentItem.VIEW_TYPE_IMAGE:
                view = inflater.inflate(R.layout.item_content_image, parent, false); // Your image content layout
                return new ImageViewHolder(view);
            default:
                // Fallback, though ideally all types are handled
                view = inflater.inflate(R.layout.item_content_text, parent, false);
                return new ContentTextViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChapterContentItem currentItem = chapterContentList.get(position);

        switch (holder.getItemViewType()) {
            case ChapterContentItem.VIEW_TYPE_CHAPTER_TITLE:
                ChapterTitleViewHolder titleHolder = (ChapterTitleViewHolder) holder;
                titleHolder.chapterTitleTextView.setText(currentItem.textContent);
                break;
            case ChapterContentItem.VIEW_TYPE_TEXT:
                ContentTextViewHolder textHolder = (ContentTextViewHolder) holder;
                textHolder.contentTextView.setText(currentItem.textContent);
                break;
            case ChapterContentItem.VIEW_TYPE_IMAGE:
                ImageViewHolder imageHolder = (ImageViewHolder) holder;
                // Load image using Glide (assuming you have Glide setup in your project)
                if (currentItem.imageUrl != null && !currentItem.imageUrl.isEmpty()) {
                    Glide.with(imageHolder.itemView.getContext())
                            .load(currentItem.imageUrl)
                            .into(imageHolder.contentImageView);
                } else {
                    imageHolder.contentImageView.setImageDrawable(null); // Clear previous image if URL is null/empty
                }
                imageHolder.imageCaptionTextView.setText(currentItem.imageCaption);
                imageHolder.imageCaptionTextView.setVisibility(
                        (currentItem.imageCaption != null && !currentItem.imageCaption.isEmpty()) ? View.VISIBLE : View.GONE
                );
                break;
        }
    }

    @Override
    public int getItemCount() {
        return chapterContentList.size();
    }

    /**
     * Implementation of FastScrollRecyclerView.SectionedAdapter.
     * This method is called by the FastScroll library to get the text to display in the popup bubble
     * for a given position.
     */
    @Override
    public String getSectionName(int position) {
        if (position < 0 || position >= chapterContentList.size()) {
            return ""; // Handle out of bounds
        }

        ChapterContentItem item = chapterContentList.get(position);

        // If the item is a chapter title, return its text content for the bubble.
        // This is what will show the "header" or "label" in the fast scroll bubble.
        if (item.viewType == ChapterContentItem.VIEW_TYPE_CHAPTER_TITLE) {
            return item.textContent;
        } else {
            // For regular text or image content, you might:
            // 1. Return the first letter of the content (if applicable and desired).
            // 2. Return an empty string or null to indicate no specific label for these items,
            //    meaning the bubble will only show labels when it hits a header.
            // 3. Return a generic marker or the position index if you need some feedback.

            // Option 1: Show the first character of regular text content if it's long enough
            if (item.viewType == ChapterContentItem.VIEW_TYPE_TEXT && item.textContent != null && !item.textContent.isEmpty()) {
                return String.valueOf(item.textContent.charAt(0)).toUpperCase();
            }

            // Option 2: Only show chapter titles in the bubble. Return an empty string for other types.
            return "";
        }
    }
}