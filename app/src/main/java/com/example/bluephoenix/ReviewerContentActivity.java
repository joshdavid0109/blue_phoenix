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
    private FastScrollRecyclerView contentRecyclerView; // Changed to FastScrollRecyclerView
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
        chapterPositions = new ArrayList<>(); // Initialize here

        Intent incomingIntent = getIntent();
        if (incomingIntent != null) {
            currentMainTopic = incomingIntent.getStringExtra("MAIN_TOPIC");
            currentSubTopic = incomingIntent.getStringExtra("SUB_TOPIC");
        }

        Log.d("ReviewerContentActivity", "Received MAIN_TOPIC: " + currentMainTopic);
        Log.d("ReviewerContentActivity", "Received SUB_TOPIC: " + currentSubTopic);

        initializeViews();
        setupRecyclerView();
        // Removed direct call to setupFastScrollTocHandler() here
        setupTableOfContentsButton(); // Renamed and simplified

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
        contentRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        contentRecyclerView.setAdapter(chapterContentAdapter);
    }

    // This method is now private and called only when the data is ready
    private void setupFastScrollTocHandler() {
        tocHandler = new FastScrollTOCHandler(
                this,
                contentRecyclerView,
                tocPopup,
                tocContent,
                chapterTitles, // These lists should now be populated
                chapterPositions // These lists should now be populated
        );
    }

    private void setupTableOfContentsButton() {
        tocButton.setOnClickListener(v -> {
            if (tocHandler != null) { // Ensure handler is initialized
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
                        // ... (handle empty) ...
                    } else {
                        Log.d("TOC_DEBUG_ACTIVITY", "--- Populating Chapter Data for TOC ---");
                        int currentAdapterPosition = 0; // Track adapter position manually

                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            String chapterDocumentId = document.getId();

                            // Store chapter title and its adapter position *before* adding the chapter title item
                            chapterTitles.add(chapterDocumentId);
                            chapterPositions.add(currentAdapterPosition); // Use manual counter
                            Log.d("TOC_DEBUG_ACTIVITY", "Adding chapter '" + chapterDocumentId + "' at proposed adapter position: " + currentAdapterPosition);


                            newContentItems.add(new ChapterContentItem(ChapterContentItem.VIEW_TYPE_CHAPTER_TITLE, chapterDocumentId));
                            currentAdapterPosition++; // Increment for the chapter title itself

                            Object sectionsObject = document.get("sections");
                            if (sectionsObject instanceof List) {
                                List<Map<String, Object>> sections = (List<Map<String, Object>>) sectionsObject;
                                for (Map<String, Object> sectionMap : sections) {
                                    if (sectionMap.containsKey("content") && sectionMap.get("content") instanceof String) {
                                        String textContent = (String) sectionMap.get("content");
                                        newContentItems.add(new ChapterContentItem(ChapterContentItem.VIEW_TYPE_TEXT, textContent));
                                        currentAdapterPosition++; // Increment for each text item
                                    } else {
                                        Log.w("ReviewerContentActivity", "Section map in " + chapterDocumentId + " has no 'content' field or it's not a String. Map: " + sectionMap);
                                        newContentItems.add(new ChapterContentItem(ChapterContentItem.VIEW_TYPE_TEXT, "Error: Malformed content section."));
                                        currentAdapterPosition++; // Increment for error text item too
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
                        Log.d("TOC_DEBUG_ACTIVITY", "Total content items in RecyclerView: " + newContentItems.size() + " (should equal currentAdapterPosition: " + currentAdapterPosition + ")");
                    }
                    chapterContentAdapter.updateData(newContentItems);

                    if (tocHandler == null) {
                        setupFastScrollTocHandler();
                    } else {
                        tocHandler.updateChapterData(chapterTitles, chapterPositions);
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
            tocHandler.forceHideToc(); // Use handler to hide
        } else {
            super.onBackPressed();
        }
    }
}