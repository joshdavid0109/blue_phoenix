package com.example.bluephoenix.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.bluephoenix.R;
import com.example.bluephoenix.models.ChapterContentItem;

import java.util.List;

// Import for L4Digital FastScrollRecyclerView and FastScroller
import com.l4digital.fastscroll.FastScroller; // Import FastScroller
import com.l4digital.fastscroll.FastScrollRecyclerView; // Still needed for the class reference in XML

public class ChapterContentAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
        // Implement FastScroller.SectionIndexer for the popup text
        implements FastScroller.SectionIndexer
{

    private List<ChapterContentItem> chapterContentList;

    public ChapterContentAdapter(List<ChapterContentItem> chapterContentList) {
        this.chapterContentList = chapterContentList;
    }

    // region ViewHolder classes (These remain the same and should be correct based on your XML IDs)
    public static class ChapterTitleViewHolder extends RecyclerView.ViewHolder {
        public TextView chapterTitleTextView;

        public ChapterTitleViewHolder(View itemView) {
            super(itemView);
            chapterTitleTextView = itemView.findViewById(R.id.chapter_title_text_view);
        }
    }

    public static class ContentTextViewHolder extends RecyclerView.ViewHolder {
        public TextView contentTextView;

        public ContentTextViewHolder(View itemView) {
            super(itemView);
            contentTextView = itemView.findViewById(R.id.content_text_view);
        }
    }

    public static class ImageViewHolder extends RecyclerView.ViewHolder {
        public ImageView contentImageView;
        public TextView imageCaptionTextView;

        public ImageViewHolder(View itemView) {
            super(itemView);
            contentImageView = itemView.findViewById(R.id.image_content_view);
            imageCaptionTextView = itemView.findViewById(R.id.image_caption_view);
        }
    }
    // endregion

    @Override
    public int getItemViewType(int position) {
        return chapterContentList.get(position).viewType;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view;

        switch (viewType) {
            case ChapterContentItem.VIEW_TYPE_CHAPTER_TITLE:
                view = inflater.inflate(R.layout.item_chapter_title, parent, false);
                return new ChapterTitleViewHolder(view);
            case ChapterContentItem.VIEW_TYPE_TEXT:
                view = inflater.inflate(R.layout.item_content_text, parent, false);
                return new ContentTextViewHolder(view);
            case ChapterContentItem.VIEW_TYPE_IMAGE:
                view = inflater.inflate(R.layout.item_content_image, parent, false);
                return new ImageViewHolder(view);
            default:
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
                if (currentItem.imageUrl != null && !currentItem.imageUrl.isEmpty()) {
                    Glide.with(imageHolder.itemView.getContext())
                            .load(currentItem.imageUrl)
                            .into(imageHolder.contentImageView);
                } else {
                    imageHolder.contentImageView.setImageDrawable(null);
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
     * Implementation of FastScroller.SectionIndexer from L4Digital/FastScroll library (v2.1.0).
     * This method provides the text for the FastScroll popup bubble.
     */
    @Override
    public String getSectionText(int position) { // New method name
        if (position < 0 || position >= chapterContentList.size()) {
            return "";
        }

        ChapterContentItem item = chapterContentList.get(position);

        if (item.viewType == ChapterContentItem.VIEW_TYPE_CHAPTER_TITLE) {
            return item.textContent; // This text will appear in the scroll bubble
        } else {
            return ""; // For other content types, don't show specific text in the bubble
        }
    }

    // Existing updateData method
    public void updateData(List<ChapterContentItem> newData) {
        this.chapterContentList.clear();
        this.chapterContentList.addAll(newData);
        notifyDataSetChanged();
    }
}