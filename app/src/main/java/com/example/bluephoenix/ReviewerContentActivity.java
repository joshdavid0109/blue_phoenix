package com.example.bluephoenix;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bluephoenix.adapters.ChapterContentAdapter;
import com.example.bluephoenix.models.ChapterContentItem;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.l4digital.fastscroll.FastScrollRecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ReviewerContentActivity extends AppCompatActivity {

    private TextInputLayout backBtn;
    private FastScrollRecyclerView contentRecyclerView;
    private ChapterContentAdapter chapterContentAdapter;
    private List<ChapterContentItem> contentItems;
    private TextView codalTitle1;
    private TextView codalTitle2;
    private ImageButton tocButton;
    private ScrollView tocPopup;
    private LinearLayout tocContent;
    private FastScrollTOCHandler tocHandler;

    private FirebaseFirestore db;

    private String currentMainTopic;
    private String currentSubTopic;

    // These lists will be passed to the FastScrollTOCHandler
    private List<String> chapterTitles;
    private List<Integer> chapterPositions;

    // For tracking scroll position and updating active chapter
    private int currentActiveChapter = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_reviewer_content);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();
        chapterTitles = new ArrayList<>();
        chapterPositions = new ArrayList<>();

        Intent incomingIntent = getIntent();
        if (incomingIntent != null) {
            currentMainTopic = incomingIntent.getStringExtra("MAIN_TOPIC");
            currentSubTopic = incomingIntent.getStringExtra("SUB_TOPIC");
        }

        Log.d("ReviewerContentActivity", "Received MAIN_TOPIC: " + currentMainTopic);
        Log.d("ReviewerContentActivity", "Received SUB_TOPIC: " + currentSubTopic);

        initializeViews();
        setupRecyclerView();
        setupTableOfContentsButton();

        // Set header titles
        if (currentSubTopic != null) {
            codalTitle1.setText(currentSubTopic);
        } else {
            codalTitle1.setText("Select Subtopic");
            Log.e("ReviewerContentActivity", "currentSubTopic is null. Cannot fetch chapters.");
            Toast.makeText(this, "Error: Subtopic not provided.", Toast.LENGTH_LONG).show();
            return;
        }

        if (currentMainTopic != null) {
            codalTitle2.setText(currentMainTopic);
        } else {
            codalTitle2.setText("Select Main Topic");
            Log.e("ReviewerContentActivity", "currentMainTopic is null. Cannot fetch chapters.");
            Toast.makeText(this, "Error: Main Topic not provided.", Toast.LENGTH_LONG).show();
            return;
        }

        fetchAllChaptersContentFromFirestore();
    }

    private void initializeViews() {
        backBtn = findViewById(R.id.arrow_back_ic);
        backBtn.setOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        });

        contentRecyclerView = findViewById(R.id.content_recycler_view_reviewer);
        codalTitle1 = findViewById(R.id.codal_title_1);
        codalTitle2 = findViewById(R.id.codal_title_2);
        tocButton = findViewById(R.id.toc_button);
        tocPopup = findViewById(R.id.toc_popup);
        tocContent = findViewById(R.id.toc_content);
    }

    private void setupRecyclerView() {
        contentItems = new ArrayList<>();
        chapterContentAdapter = new ChapterContentAdapter(contentItems);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        contentRecyclerView.setLayoutManager(layoutManager);
        contentRecyclerView.setAdapter(chapterContentAdapter);

        // Add scroll listener to track current chapter
        contentRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                updateActiveChapter();
            }
        });
    }

    private void updateActiveChapter() {
        if (chapterPositions.isEmpty()) return;

        LinearLayoutManager layoutManager = (LinearLayoutManager) contentRecyclerView.getLayoutManager();
        if (layoutManager == null) return;

        int firstVisiblePosition = layoutManager.findFirstVisibleItemPosition();
        int lastVisiblePosition = layoutManager.findLastVisibleItemPosition();

        // Find which chapter is currently most visible
        int newActiveChapter = -1;
        for (int i = chapterPositions.size() - 1; i >= 0; i--) {
            if (chapterPositions.get(i) <= firstVisiblePosition) {
                newActiveChapter = i;
                break;
            }
        }

        // If we're at the very end, select the last chapter
        if (newActiveChapter == -1 && firstVisiblePosition >= 0) {
            newActiveChapter = 0;
        }

        // Update TOC if active chapter changed
        if (newActiveChapter != currentActiveChapter && newActiveChapter != -1) {
            currentActiveChapter = newActiveChapter;
            if (tocHandler != null) {
                tocHandler.updateActiveChapter(currentActiveChapter);
            }
            Log.d("TOC_SCROLL", "Active chapter changed to: " + currentActiveChapter + " (" + chapterTitles.get(currentActiveChapter) + ")");
        }
    }

    private void setupFastScrollTocHandler() {
        tocHandler = new FastScrollTOCHandler(
                this,
                contentRecyclerView,
                tocPopup,
                tocContent,
                chapterTitles,
                chapterPositions
        );

        // Set initial active chapter
        if (!chapterPositions.isEmpty()) {
            currentActiveChapter = 0;
            tocHandler.updateActiveChapter(currentActiveChapter);
        }
    }

    private void setupTableOfContentsButton() {
        tocButton.setOnClickListener(v -> {
            if (tocHandler != null) {
                if (tocPopup.getVisibility() == View.VISIBLE) {
                    tocHandler.forceHideToc();
                } else {
                    tocHandler.forceShowToc();
                }
            }
        });
    }

    private void fetchAllChaptersContentFromFirestore() {
        if (currentMainTopic == null || currentSubTopic == null) {
            Log.e("ReviewerContentActivity", "Cannot fetch chapters: Main Topic or Subtopic is null.");
            Toast.makeText(this, "Error: Missing topic information to load chapters.", Toast.LENGTH_LONG).show();
            return;
        }

        String chaptersCollectionPath = "codals/" + currentMainTopic + "/subtopics/" + currentSubTopic + "/chapters";

        Log.d("ReviewerContentActivity", "Fetching all chapter content from: " + chaptersCollectionPath);

        db.collection(chaptersCollectionPath)
                .orderBy(com.google.firebase.firestore.FieldPath.documentId(), Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<ChapterContentItem> newContentItems = new ArrayList<>();
                    chapterTitles.clear();
                    chapterPositions.clear();

                    if (queryDocumentSnapshots.isEmpty()) {
                        Log.w("ReviewerContentActivity", "No chapters found in: " + chaptersCollectionPath);
                        Toast.makeText(this, "No chapters found for this subtopic.", Toast.LENGTH_SHORT).show();
                        contentItems.clear();
                        contentItems.add(new ChapterContentItem(ChapterContentItem.VIEW_TYPE_TEXT, "No content available for this subtopic."));
                        chapterContentAdapter.notifyDataSetChanged();
                    } else {
                        Log.d("TOC_DEBUG_ACTIVITY", "--- Populating Chapter Data for TOC ---");
                        int currentAdapterPosition = 0;

                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            String chapterDocumentId = document.getId();

                            // Store chapter title and its adapter position
                            chapterTitles.add(chapterDocumentId);
                            chapterPositions.add(currentAdapterPosition);
                            Log.d("TOC_DEBUG_ACTIVITY", "Adding chapter '" + chapterDocumentId + "' at adapter position: " + currentAdapterPosition);

                            newContentItems.add(new ChapterContentItem(ChapterContentItem.VIEW_TYPE_CHAPTER_TITLE, chapterDocumentId));
                            currentAdapterPosition++;

                            Object sectionsObject = document.get("sections");
                            if (sectionsObject instanceof List) {
                                List<Map<String, Object>> sections = (List<Map<String, Object>>) sectionsObject;
                                for (Map<String, Object> sectionMap : sections) {
                                    if (sectionMap.containsKey("content") && sectionMap.get("content") instanceof String) {
                                        String textContent = (String) sectionMap.get("content");
                                        newContentItems.add(new ChapterContentItem(ChapterContentItem.VIEW_TYPE_TEXT, textContent));
                                        currentAdapterPosition++;
                                    } else {
                                        Log.w("ReviewerContentActivity", "Section map in " + chapterDocumentId + " has no 'content' field or it's not a String. Map: " + sectionMap);
                                        newContentItems.add(new ChapterContentItem(ChapterContentItem.VIEW_TYPE_TEXT, "Error: Malformed content section."));
                                        currentAdapterPosition++;
                                    }
                                }
                            } else if (sectionsObject == null) {
                                Log.w("ReviewerContentActivity", "Document " + chapterDocumentId + " has no 'sections' field.");
                                newContentItems.add(new ChapterContentItem(ChapterContentItem.VIEW_TYPE_TEXT, "No content sections defined for " + chapterDocumentId + "."));
                                currentAdapterPosition++;
                            } else {
                                Log.e("ReviewerContentActivity", "Document " + chapterDocumentId + " 'sections' field is not a List. Actual type: " + sectionsObject.getClass().getSimpleName());
                                newContentItems.add(new ChapterContentItem(ChapterContentItem.VIEW_TYPE_TEXT, "Error: Content for " + chapterDocumentId + " is in an unexpected format."));
                                currentAdapterPosition++;
                            }
                        }

                        Log.d("ReviewerContentActivity", "Successfully loaded content for " + queryDocumentSnapshots.size() + " chapters.");
                        Log.d("TOC_DEBUG_ACTIVITY", "Final chapterTitles: " + chapterTitles.toString());
                        Log.d("TOC_DEBUG_ACTIVITY", "Final chapterPositions: " + chapterPositions.toString());
                        Log.d("TOC_DEBUG_ACTIVITY", "Total content items in RecyclerView: " + newContentItems.size());
                    }

                    // Update adapter data
                    chapterContentAdapter.updateData(newContentItems);

                    // Initialize or update TOC handler
                    if (tocHandler == null) {
                        setupFastScrollTocHandler();
                    } else {
                        tocHandler.updateChapterData(chapterTitles, chapterPositions);
                        if (!chapterPositions.isEmpty()) {
                            currentActiveChapter = 0;
                            tocHandler.updateActiveChapter(currentActiveChapter);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("ReviewerContentActivity", "Error fetching all chapters content from Firestore: " + chaptersCollectionPath, e);
                    Toast.makeText(this, "Failed to load all chapter content: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    contentItems.clear();
                    contentItems.add(new ChapterContentItem(ChapterContentItem.VIEW_TYPE_TEXT, "Error loading content: " + e.getMessage()));
                    chapterContentAdapter.notifyDataSetChanged();

                    // Clear TOC data if content load fails
                    chapterTitles.clear();
                    chapterPositions.clear();
                    if (tocHandler != null) {
                        tocHandler.updateChapterData(chapterTitles, chapterPositions);
                    }
                });
    }

    @Override
    public void onBackPressed() {
        if (tocPopup.getVisibility() == View.VISIBLE) {
            tocHandler.forceHideToc();
        } else {
            super.onBackPressed();
        }
    }
}