// app/src/main/java/com/example/bluephoenix/ReviewerContentActivity.java
package com.example.bluephoenix;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater; // Import LayoutInflater - still needed if you use it elsewhere, but not for sticky header now
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
// import com.example.bluephoenix.adapters.StickyHeaderItemDecoration; // REMOVED IMPORT
import com.example.bluephoenix.models.ChapterContentItem;

import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ReviewerContentActivity extends AppCompatActivity {

    private TextInputLayout backBtn;
    private RecyclerView contentRecyclerView;
    private ChapterContentAdapter chapterContentAdapter;
    private List<ChapterContentItem> contentItems;
    private TextView codalTitle1;
    private TextView codalTitle2;

    private FirebaseFirestore db;

    private String currentMainTopic;
    private String currentSubTopic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_reviewer_content);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();

        Intent incomingIntent = getIntent();
        if (incomingIntent != null) {
            currentMainTopic = incomingIntent.getStringExtra("MAIN_TOPIC");
            currentSubTopic = incomingIntent.getStringExtra("SUB_TOPIC");
        }

        Log.d("ReviewerContentActivity", "Received MAIN_TOPIC: " + currentMainTopic);
        Log.d("ReviewerContentActivity", "Received SUB_TOPIC: " + currentSubTopic);

        backBtn = findViewById(R.id.arrow_back_ic);
        backBtn.setOnClickListener(v -> {
            Intent intent = new Intent(ReviewerContentActivity.this, ReviewerActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        });

        contentRecyclerView = findViewById(R.id.content_recycler_view_reviewer);
        codalTitle1 = findViewById(R.id.codal_title_1);
        codalTitle2 = findViewById(R.id.codal_title_2);

        contentItems = new ArrayList<>();
        chapterContentAdapter = new ChapterContentAdapter(contentItems);
        contentRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        contentRecyclerView.setAdapter(chapterContentAdapter);

        // OLD: NEW: Add the StickyHeaderItemDecoration
        // OLD: // You need to pass the adapter and a LayoutInflater to the decoration
        // OLD: contentRecyclerView.addItemDecoration(new StickyHeaderItemDecoration(chapterContentAdapter, getLayoutInflater()));
        // The above lines are now commented out or removed to disable the sticky header.

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
                    if (queryDocumentSnapshots.isEmpty()) {
                        Log.d("ReviewerContentActivity", "No chapters found at: " + chaptersCollectionPath);
                        Toast.makeText(this, "No chapters available for " + currentSubTopic, Toast.LENGTH_SHORT).show();
                        newContentItems.add(new ChapterContentItem(ChapterContentItem.VIEW_TYPE_TEXT, "No chapters found for this subtopic."));
                    } else {
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            String chapterDocumentId = document.getId();
                            Log.d("ReviewerContentActivity", "Processing chapter: " + chapterDocumentId);

                            newContentItems.add(new ChapterContentItem(ChapterContentItem.VIEW_TYPE_CHAPTER_TITLE, chapterDocumentId));

                            Object sectionsObject = document.get("sections");
                            if (sectionsObject instanceof List) {
                                List<Map<String, Object>> sections = (List<Map<String, Object>>) sectionsObject;
                                if (sections.isEmpty()) {
                                    Log.w("ReviewerContentActivity", "Chapter " + chapterDocumentId + " has an empty 'sections' array.");
                                    newContentItems.add(new ChapterContentItem(ChapterContentItem.VIEW_TYPE_TEXT, "No content for this chapter."));
                                } else {
                                    for (Map<String, Object> sectionMap : sections) {
                                        if (sectionMap.containsKey("content") && sectionMap.get("content") instanceof String) {
                                            String textContent = (String) sectionMap.get("content");
                                            newContentItems.add(new ChapterContentItem(ChapterContentItem.VIEW_TYPE_TEXT, textContent));
                                        } else {
                                            Log.w("ReviewerContentActivity", "Section map in " + chapterDocumentId + " has no 'content' field or it's not a String. Map: " + sectionMap);
                                            newContentItems.add(new ChapterContentItem(ChapterContentItem.VIEW_TYPE_TEXT, "Error: Malformed content section."));
                                        }
                                    }
                                }
                            } else if (sectionsObject == null) {
                                Log.w("ReviewerContentActivity", "Document " + chapterDocumentId + " has no 'sections' field.");
                                newContentItems.add(new ChapterContentItem(ChapterContentItem.VIEW_TYPE_TEXT, "No content sections defined for " + chapterDocumentId + "."));
                            } else {
                                Log.e("ReviewerContentActivity", "Document " + chapterDocumentId + " 'sections' field is not a List. Actual type: " + sectionsObject.getClass().getSimpleName());
                                newContentItems.add(new ChapterContentItem(ChapterContentItem.VIEW_TYPE_TEXT, "Error: Content for " + chapterDocumentId + " is in an unexpected format."));
                            }
                        }
                        Log.d("ReviewerContentActivity", "Successfully loaded content for " + queryDocumentSnapshots.size() + " chapters.");
                    }
                    chapterContentAdapter.updateData(newContentItems);
                })
                .addOnFailureListener(e -> {
                    Log.e("ReviewerContentActivity", "Error fetching all chapters content from Firestore: " + chaptersCollectionPath, e);
                    Toast.makeText(this, "Failed to load all chapter content: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    contentItems.clear();
                    contentItems.add(new ChapterContentItem(ChapterContentItem.VIEW_TYPE_TEXT, "Error loading content: " + e.getMessage()));
                    chapterContentAdapter.notifyDataSetChanged();
                });
    }
}