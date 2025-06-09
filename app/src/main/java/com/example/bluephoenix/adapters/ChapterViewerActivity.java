package com.example.bluephoenix.adapters; // Changed package to 'activities'

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bluephoenix.R;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot; // Not strictly needed for single document get, but good to keep if you used it before

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.bumptech.glide.Glide; // Keep this uncommented if you use images.

public class ChapterViewerActivity extends AppCompatActivity {

    // Removed chapterTitleTextView as it was causing ambiguity with subtopic name
    private TextView mainTopicTitleTextView; // This will show the main topic (e.g., "Remedial Law")
    private TextView subtopicHeaderTextView; // New TextView to show the subtopic in the header (e.g., "Land Titles and Deeds")
    private RecyclerView contentRecyclerView;
    private ContentAdapter contentAdapter;
    private List<ChapterContentItem> contentItems;
    private FirebaseFirestore db;

    private String mainTopicName;
    private String subtopicName;
    private String chapterDocumentId;

    private ImageView menuIcon;
    private TextInputLayout backArrowIcon;
    private DrawerLayout drawerLayout;
    private Button continueSessionPlaceholderButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chapter_viewer);

        // Initialize UI components
        // chapterTitleTextView removed from here, and the original R.id.chapter_title_text_view
        // might need to be renamed/repurposed in your activity_chapter_viewer.xml.
        // For clarity, let's assume you'll add a new TextView for the subtopic.
        mainTopicTitleTextView = findViewById(R.id.main_topic_title_text_view);
        contentRecyclerView = findViewById(R.id.content_recycler_view);

        menuIcon = findViewById(R.id.menu_icon);
        backArrowIcon = findViewById(R.id.arrow_back_ic);
        drawerLayout = findViewById(R.id.drawer_layout);
        continueSessionPlaceholderButton = findViewById(R.id.button_continue_session_placeholder);

        db = FirebaseFirestore.getInstance();
        contentItems = new ArrayList<>();
        contentAdapter = new ContentAdapter(contentItems);
        contentRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        contentRecyclerView.setAdapter(contentAdapter);

        // Retrieve data from Intent
        if (getIntent().hasExtra("MAIN_TOPIC") &&
                getIntent().hasExtra("SUB_TOPIC") &&
                getIntent().hasExtra("CHAPTER_DOCUMENT_ID")) {

            mainTopicName = getIntent().getStringExtra("MAIN_TOPIC");
            subtopicName = getIntent().getStringExtra("SUB_TOPIC");
            chapterDocumentId = getIntent().getStringExtra("CHAPTER_DOCUMENT_ID");

            // Set header texts
            mainTopicTitleTextView.setText(mainTopicName);
            subtopicHeaderTextView.setText(subtopicName); // Display the subtopic name here

            String chapterDocumentPath = "codals/" + mainTopicName + "/subtopics/" + subtopicName + "/chapters/" + chapterDocumentId;

            Log.d("ChapterViewerActivity", "Attempting to load content from: " + chapterDocumentPath);
            fetchChapterContent(chapterDocumentPath);
        } else {
            Log.e("ChapterViewerActivity", "Missing required intent extras: MAIN_TOPIC, SUB_TOPIC, CHAPTER_DOCUMENT_ID.");
            Toast.makeText(this, "Error: Chapter data missing.", Toast.LENGTH_LONG).show();
            finish();
        }

        // Set up click listeners
        backArrowIcon.setOnClickListener(v -> onBackPressed());

        menuIcon.setOnClickListener(v -> {
            if (drawerLayout != null) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

        continueSessionPlaceholderButton.setOnClickListener(v -> {
            Toast.makeText(this, "Continue Session functionality coming soon!", Toast.LENGTH_SHORT).show();
        });
    }

    private void fetchChapterContent(String documentPath) {
        db.document(documentPath)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String actualChapterTitle = documentSnapshot.getId(); // e.g., "CHAPTER II. THE LAND REGISTRATION COMMISSI..."
                        Log.d("ChapterViewerActivity", "Fetched document: " + actualChapterTitle);

                        contentItems.clear(); // Clear previous content

                        // Always add the actual chapter title as the first item in the RecyclerView content
                        contentItems.add(new ChapterContentItem(ContentAdapter.VIEW_TYPE_CHAPTER_TITLE, actualChapterTitle));

                        // --- CRITICAL CHANGE HERE TO MATCH YOUR FIRESTORE STRUCTURE ---
                        Object sectionsObject = documentSnapshot.get("sections");
                        if (sectionsObject instanceof List) {
                            List<Map<String, Object>> sections = (List<Map<String, Object>>) sectionsObject;

                            if (sections.isEmpty()) {
                                Log.w("ChapterViewerActivity", "Document " + actualChapterTitle + " has an empty 'sections' array.");
                                contentItems.add(new ChapterContentItem(ContentAdapter.VIEW_TYPE_TEXT, "No content sections available for this chapter."));
                            }

                            for (Map<String, Object> sectionMap : sections) {
                                // Based on image_605273.png, content is directly under a 'content' key
                                if (sectionMap.containsKey("content") && sectionMap.get("content") instanceof String) {
                                    String textContent = (String) sectionMap.get("content");
                                    contentItems.add(new ChapterContentItem(ContentAdapter.VIEW_TYPE_TEXT, textContent));
                                    Log.d("ChapterViewerActivity", "Added text content: " + textContent.substring(0, Math.min(textContent.length(), 50)) + "...");
                                } else {
                                    // If you eventually introduce other types (like images) in this same 'sections' array,
                                    // you would add more `if/else if` blocks here to check for 'image' or 'url' etc.
                                    // For now, logging a warning for unexpected map structures.
                                    Log.w("ChapterViewerActivity", "Section map in " + actualChapterTitle + " has no 'content' field or it's not a String. Map: " + sectionMap);
                                }
                            }
                            // --- END OF CRITICAL CHANGE ---

                            contentAdapter.notifyDataSetChanged();
                            if (contentItems.size() <= 1) { // Only the chapter title was added, no actual content
                                Toast.makeText(ChapterViewerActivity.this, "No specific content found for " + actualChapterTitle + ".", Toast.LENGTH_SHORT).show();
                            } else {
                                Log.d("ChapterViewerActivity", "Successfully loaded " + (contentItems.size() - 1) + " content items for " + actualChapterTitle);
                            }
                        } else if (sectionsObject == null) {
                            Log.w("ChapterViewerActivity", "Document " + actualChapterTitle + " has no 'sections' field.");
                            Toast.makeText(ChapterViewerActivity.this, "No content sections defined for this chapter.", Toast.LENGTH_SHORT).show();
                            contentItems.add(new ChapterContentItem(ContentAdapter.VIEW_TYPE_TEXT, "No content sections available for this chapter."));
                            contentAdapter.notifyDataSetChanged();
                        } else {
                            Log.e("ChapterViewerActivity", "Document " + actualChapterTitle + " 'sections' field is not a List. Actual type: " + sectionsObject.getClass().getSimpleName());
                            Toast.makeText(ChapterViewerActivity.this, "Content format error.", Toast.LENGTH_SHORT).show();
                            contentItems.add(new ChapterContentItem(ContentAdapter.VIEW_TYPE_TEXT, "Error: Content is in an unexpected format."));
                            contentAdapter.notifyDataSetChanged();
                        }
                    } else {
                        Log.e("ChapterViewerActivity", "Document does not exist at " + documentPath);
                        Toast.makeText(ChapterViewerActivity.this, "Chapter not found.", Toast.LENGTH_SHORT).show();
                        contentItems.clear();
                        contentItems.add(new ChapterContentItem(ContentAdapter.VIEW_TYPE_TEXT, "Chapter document not found."));
                        contentAdapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("ChapterViewerActivity", "Error loading chapter content from " + documentPath, e);
                    Toast.makeText(ChapterViewerActivity.this, "Failed to load content: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    contentItems.clear();
                    contentItems.add(new ChapterContentItem(ContentAdapter.VIEW_TYPE_TEXT, "Error loading content: " + e.getMessage()));
                    contentAdapter.notifyDataSetChanged();
                });
    }

    // --- ContentItem and Adapter classes remain the same except for Glide uncomment ---

    public static class ChapterContentItem {
        public int viewType;
        public String textContent;
        public String imageUrl; // Still keep for future image support if you change Firestore
        public String imageCaption; // Still keep for future image support if you change Firestore

        public ChapterContentItem(int viewType, String text) {
            this.viewType = viewType;
            this.textContent = text;
            this.imageUrl = null;
            this.imageCaption = null;
        }

        public ChapterContentItem(int viewType, String imageUrl, String imageCaption) {
            this.viewType = viewType;
            this.imageUrl = imageUrl;
            this.imageCaption = imageCaption;
            this.textContent = null;
        }
    }

    public static class ContentAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        public static final int VIEW_TYPE_CHAPTER_TITLE = 0;
        public static final int VIEW_TYPE_TEXT = 1;
        public static final int VIEW_TYPE_IMAGE = 2; // Still keep for future image support

        private List<ChapterContentItem> contentItems;

        public ContentAdapter(List<ChapterContentItem> contentItems) {
            this.contentItems = contentItems;
        }

        @Override
        public int getItemViewType(int position) {
            return contentItems.get(position).viewType;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            View view;
            switch (viewType) {
                case VIEW_TYPE_CHAPTER_TITLE:
                    view = inflater.inflate(R.layout.item_chapter_title, parent, false);
                    return new ChapterTitleViewHolder(view);
                case VIEW_TYPE_TEXT:
                    view = inflater.inflate(R.layout.item_content_text, parent, false);
                    return new TextViewHolder(view);
                case VIEW_TYPE_IMAGE:
                    // For now, this case might not be hit if your Firestore only has 'text' content in 'sections'.
                    // Keep it for future compatibility if you add images following the old 'type' structure,
                    // or if you modify Firestore structure to support images using the 'content' field differently.
                    view = inflater.inflate(R.layout.item_content_image, parent, false);
                    return new ImageViewHolder(view);
                default:
                    throw new IllegalArgumentException("Unknown view type: " + viewType);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            ChapterContentItem item = contentItems.get(position);
            switch (item.viewType) {
                case VIEW_TYPE_CHAPTER_TITLE:
                    ((ChapterTitleViewHolder) holder).chapterTitleTextView.setText(item.textContent);
                    break;
                case VIEW_TYPE_TEXT:
                    ((TextViewHolder) holder).textView.setText(item.textContent);
                    break;
                case VIEW_TYPE_IMAGE:
                    ImageViewHolder imageHolder = (ImageViewHolder) holder;
                    if (item.imageUrl != null && !item.imageUrl.isEmpty()) {
                        // UNCOMMENT THIS FOR REAL IMAGE LOADING AND ENSURE GLIDE DEPENDENCY!
                        Glide.with(imageHolder.imageView.getContext())
                                .load(item.imageUrl)
                                .placeholder(R.drawable.ic_launcher_background)
                                .error(R.drawable.ic_error_outline)
                                .into(imageHolder.imageView);
                    } else {
                        imageHolder.imageView.setImageResource(R.drawable.ic_broken_image);
                    }
                    imageHolder.imageCaptionTextView.setText(item.imageCaption);
                    imageHolder.imageCaptionTextView.setVisibility(item.imageCaption != null && !item.imageCaption.isEmpty() ? View.VISIBLE : View.GONE);
                    break;
            }
        }

        @Override
        public int getItemCount() {
            return contentItems.size();
        }

        public static class ChapterTitleViewHolder extends RecyclerView.ViewHolder {
            TextView chapterTitleTextView;
            public ChapterTitleViewHolder(@NonNull View itemView) {
                super(itemView);
                chapterTitleTextView = itemView.findViewById(R.id.chapter_title_text_view);
            }
        }

        public static class TextViewHolder extends RecyclerView.ViewHolder {
            TextView textView;
            public TextViewHolder(@NonNull View itemView) {
                super(itemView);
                textView = itemView.findViewById(R.id.content_text_view);
            }
        }

        public static class ImageViewHolder extends RecyclerView.ViewHolder {
            ImageView imageView;
            TextView imageCaptionTextView;
            public ImageViewHolder(@NonNull View itemView) {
                super(itemView);
                imageView = itemView.findViewById(R.id.image_content_view);
                imageCaptionTextView = itemView.findViewById(R.id.image_caption_view);
            }
        }
    }
}