package com.example.bluephoenix.adapters;

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
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bluephoenix.R;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// import com.bumptech.glide.Glide; // Uncomment if using Glide

public class ChapterViewerActivity extends AppCompatActivity {

    private TextView chapterTitleTextView;
    private TextView mainTopicTitleTextView;
    private RecyclerView contentRecyclerView;
    private ContentAdapter contentAdapter;
    private List<ChapterContentItem> contentItems;
    private FirebaseFirestore db;

    private String mainTopicName;
    private String subtopicName;
    // This subcollectionPath should now be the full path to the DOCUMENT that contains the 'chapters' subcollection.
    // Example: "codals/Remedial Law/subtopics/Civil Procedure"
    private String subcollectionDocumentPath;
    private String chaptersCollectionPath; // The actual path to the 'chapters' subcollection

    private ImageView menuIcon;
    private TextInputLayout backArrowIcon;
    private DrawerLayout drawerLayout;
    private Button continueSessionPlaceholderButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chapter_viewer);

        chapterTitleTextView = findViewById(R.id.chapter_title_text_view);
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

        if (getIntent().hasExtra("SUBCOLLECTION_PATH") && getIntent().hasExtra("SUBTOPIC_NAME") && getIntent().hasExtra("MAIN_TOPIC_NAME")) {
            // Renamed for clarity: subcollectionPath is now subcollectionDocumentPath
            // It should be the path to the document containing 'chapters', e.g., "codals/Remedial Law/subtopics/Civil Procedure"
            subcollectionDocumentPath = getIntent().getStringExtra("SUBCOLLECTION_PATH");
            subtopicName = getIntent().getStringExtra("SUBTOPIC_NAME");
            mainTopicName = getIntent().getStringExtra("MAIN_TOPIC_NAME");

            // Construct the correct path to the 'chapters' subcollection
            // This will now be: "codals/Remedial Law/subtopics/Civil Procedure/chapters"
            // This path has 5 segments (odd), which is valid for a collection reference.
            chaptersCollectionPath = subcollectionDocumentPath + "/chapters";


            mainTopicTitleTextView.setText(mainTopicName);
            chapterTitleTextView.setText(subtopicName);

            Log.d("ChapterViewerActivity", "Loading chapters from: " + chaptersCollectionPath);
            fetchChaptersAndContent(chaptersCollectionPath); // Pass the CORRECT path
        } else {
            Log.e("ChapterViewerActivity", "Missing required intent extras.");
            Toast.makeText(this, "Error: Chapter data missing.", Toast.LENGTH_LONG).show();
            finish();
        }

        backArrowIcon.setOnClickListener(v -> onBackPressed());

        menuIcon.setOnClickListener(v -> {
            if (drawerLayout != null) {
                drawerLayout.openDrawer(androidx.core.view.GravityCompat.START);
            }
        });
    }

    private void fetchChaptersAndContent(String path) {
        db.collection(path)
                .orderBy(com.google.firebase.firestore.FieldPath.documentId(), Query.Direction.ASCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        contentItems.clear();
                        if (task.getResult() != null && !task.getResult().isEmpty()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                String chapterTitle = document.getId(); // E.g., "CHAPTER I. GENERAL PROVISIONS"

                                // Safely retrieve the 'sections' field as a List
                                Object sectionsObject = document.get("sections");
                                if (sectionsObject instanceof List) {
                                    List<Map<String, Object>> sections = new ArrayList<>();
                                    // Iterate safely through the list elements, checking if each is a Map
                                    for (Object sectionItem : (List<?>) sectionsObject) {
                                        if (sectionItem instanceof Map) {
                                            sections.add((Map<String, Object>) sectionItem);
                                        } else {
                                            Log.w("ChapterViewerActivity", "Chapter document " + chapterTitle + ": Found non-Map element in 'sections' list. Skipping it.");
                                            // You could also add a placeholder/error item here if needed for UI
                                        }
                                    }

                                    if (!sections.isEmpty()) {
                                        contentItems.add(new ChapterContentItem(ContentAdapter.VIEW_TYPE_CHAPTER_TITLE, chapterTitle));

                                        for (Map<String, Object> section : sections) {
                                            // Safely retrieve 'type'
                                            String type = null;
                                            if (section.containsKey("type") && section.get("type") instanceof String) {
                                                type = (String) section.get("type");
                                            } else {
                                                Log.w("ChapterViewerActivity", "Chapter document " + chapterTitle + ": Section missing or invalid 'type' field. Skipping section.");
                                                continue; // Skip this section if type is missing or invalid
                                            }

                                            if ("text".equals(type)) {
                                                // Safely retrieve 'value' for text
                                                String text = null;
                                                if (section.containsKey("value") && section.get("value") instanceof String) {
                                                    text = (String) section.get("value");
                                                } else {
                                                    Log.w("ChapterViewerActivity", "Chapter document " + chapterTitle + ": Text section missing or invalid 'value' field.");
                                                }
                                                if (text != null) {
                                                    contentItems.add(new ChapterContentItem(ContentAdapter.VIEW_TYPE_TEXT, text));
                                                }
                                            } else if ("image".equals(type)) {
                                                // Safely retrieve 'url' and 'caption' for image
                                                String imageUrl = null;
                                                String imageCaption = null;

                                                if (section.containsKey("url") && section.get("url") instanceof String) {
                                                    imageUrl = (String) section.get("url");
                                                } else {
                                                    Log.w("ChapterViewerActivity", "Chapter document " + chapterTitle + ": Image section missing or invalid 'url' field.");
                                                }

                                                if (section.containsKey("caption") && section.get("caption") instanceof String) {
                                                    imageCaption = (String) section.get("caption");
                                                } else {
                                                    Log.i("ChapterViewerActivity", "Chapter document " + chapterTitle + ": Image section missing 'caption' field or it's not a String.");
                                                }

                                                if (imageUrl != null) {
                                                    contentItems.add(new ChapterContentItem(ContentAdapter.VIEW_TYPE_IMAGE, imageUrl, imageCaption));
                                                }
                                            } else {
                                                Log.w("ChapterViewerActivity", "Chapter document " + chapterTitle + ": Unknown section type: " + type + ". Skipping section.");
                                            }
                                        }
                                    } else {
                                        Log.w("ChapterViewerActivity", "Chapter document " + chapterTitle + " has 'sections' field, but it's empty or contains no valid section maps.");
                                    }
                                } else {
                                    Log.w("ChapterViewerActivity", "Chapter document " + chapterTitle + " has no 'sections' field or it's not a List type. Actual type: " + (sectionsObject != null ? sectionsObject.getClass().getSimpleName() : "null"));
                                }
                            }
                            contentAdapter.notifyDataSetChanged();
                            if (contentItems.isEmpty()) {
                                Toast.makeText(this, "No content found for " + subtopicName + ".", Toast.LENGTH_SHORT).show();
                            } else {
                                Log.d("ChapterViewerActivity", "Successfully loaded " + contentItems.size() + " content items for " + subtopicName);
                            }
                        } else {
                            Toast.makeText(this, "No chapters found for " + subtopicName + ".", Toast.LENGTH_SHORT).show();
                            Log.d("ChapterViewerActivity", "Task result is empty for chapters under: " + path);
                        }
                    } else {
                        Log.e("ChapterViewerActivity", "Error getting chapter documents: ", task.getException());
                        Toast.makeText(this, "Error loading chapters: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    public static class ChapterContentItem {
        public int viewType;
        public String textContent;
        public String imageUrl;
        public String imageCaption;

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
        public static final int VIEW_TYPE_IMAGE = 2;

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
                    // Implement image loading here (e.g., Glide)
                    // You MUST uncomment and use a library like Glide or Picasso for real image loading
                    // Glide.with(imageHolder.imageView.getContext()).load(item.imageUrl).into(imageHolder.imageView);
                    imageHolder.imageView.setImageResource(R.drawable.ic_launcher_background); // Still a placeholder, ensure you load actual images
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
                chapterTitleTextView = itemView.findViewById(R.id.chapter_title_text_view_item);
            }
        }

        public static class TextViewHolder extends RecyclerView.ViewHolder {
            TextView textView;
            public TextViewHolder(@NonNull View itemView) {
                super(itemView);
                textView = itemView.findViewById(R.id.text_content_view);
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