package com.example.bluephoenix;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ReviewerActivity extends AppCompatActivity {

    public static final String EXTRA_SELECTED_MAIN_TOPIC = "selected_main_topic";

    private TextView codalTitleTextView;
    private RecyclerView subtopicButtonsRecyclerView;
    private SubtopicAdapter subtopicAdapter;
    private List<Subtopic> subtopicList;
    private FirebaseFirestore db;

    private String selectedMainTopic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reviewer);

        codalTitleTextView = findViewById(R.id.codal_title_1);
        subtopicButtonsRecyclerView = findViewById(R.id.subtopic_buttons_recyclerview);

        db = FirebaseFirestore.getInstance();
        subtopicList = new ArrayList<>();

        subtopicButtonsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        if (getIntent().hasExtra(EXTRA_SELECTED_MAIN_TOPIC)) {
            selectedMainTopic = getIntent().getStringExtra(EXTRA_SELECTED_MAIN_TOPIC);
            codalTitleTextView.setText(selectedMainTopic);
            Log.d("ReviewerActivity", "Selected Main Topic: " + selectedMainTopic);

            subtopicAdapter = new SubtopicAdapter(subtopicList, selectedMainTopic);
            subtopicButtonsRecyclerView.setAdapter(subtopicAdapter);

            fetchSubtopics(selectedMainTopic);
        } else {
            Log.e("ReviewerActivity", "No main topic passed to ReviewerActivity. Finishing activity.");
            Toast.makeText(this, "Error: No topic selected.", Toast.LENGTH_LONG).show();
            finish();
        }

        findViewById(R.id.arrow_back_ic).setOnClickListener(v -> onBackPressed());

        findViewById(R.id.menu_icon).setOnClickListener(v -> {
            androidx.drawerlayout.widget.DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
            if (drawerLayout != null) {
                drawerLayout.openDrawer(androidx.core.view.GravityCompat.START);
            }
        });
    }

    private void fetchSubtopics(String mainTopic) {
        if (mainTopic == null || mainTopic.trim().isEmpty()) {
            Log.e("ReviewerActivity", "Main topic is null or empty. Cannot fetch subtopics.");
            Toast.makeText(this, "Error: Invalid main topic.", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d("ReviewerActivity", "Attempting to fetch subtopic documents from: codals/" + mainTopic + "/subtopics");

        db.collection("codals")
                .document(mainTopic)
                .collection("subtopics")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        subtopicList.clear();

                        if (task.getResult() != null && !task.getResult().isEmpty()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                String subtopicTitle = document.getId(); // e.g., "Civil Procedure"

                                // Construct the *full* document path to the subtopic document itself.
                                // This path will be "codals/Remedial Law/subtopics/Civil Procedure"
                                String fullSubtopicDocumentPath = "codals/" + mainTopic + "/subtopics/" + subtopicTitle;

                                // Add the Subtopic object to the list, passing the full document path
                                subtopicList.add(new Subtopic(subtopicTitle, fullSubtopicDocumentPath));
                                Log.d("ReviewerActivity", "Found subtopic document: " + subtopicTitle + " with path: " + fullSubtopicDocumentPath);
                            }
                            subtopicAdapter.notifyDataSetChanged();

                            if (subtopicList.isEmpty()) {
                                Toast.makeText(this, "No subtopics found under '" + mainTopic + "'.", Toast.LENGTH_SHORT).show();
                                Log.d("ReviewerActivity", "Subtopic list is empty after processing documents for: " + mainTopic);
                            } else {
                                Log.d("ReviewerActivity", "Successfully fetched " + subtopicList.size() + " subtopics for: " + mainTopic);
                            }
                        } else {
                            Toast.makeText(this, "No subtopics found for '" + mainTopic + "'.", Toast.LENGTH_SHORT).show();
                            Log.d("ReviewerActivity", "Task result is empty (no documents in 'subtopics' subcollection) for: " + mainTopic);
                        }
                    } else {
                        Log.e("ReviewerActivity", "Error getting subtopic documents for '" + mainTopic + "': ", task.getException());
                        Toast.makeText(this, "Error fetching subtopics: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    // --- Subtopic Model Class ---
    public static class Subtopic {
        private String title;
        // This subcollectionPath is now the *full document path* to the subtopic,
        // e.g., "codals/Remedial Law/subtopics/Civil Procedure"
        private String subtopicDocumentPath; // Renamed for clarity to reflect it's a document path

        public Subtopic(String title, String subtopicDocumentPath) {
            this.title = title;
            this.subtopicDocumentPath = subtopicDocumentPath;
        }

        public String getTitle() { return title; }
        public String getSubtopicDocumentPath() { return subtopicDocumentPath; } // Renamed getter
    }


    // --- SubtopicAdapter for RecyclerView ---
    public static class SubtopicAdapter extends RecyclerView.Adapter<SubtopicAdapter.SubtopicViewHolder> {

        private List<Subtopic> subtopicList;
        private String adapterSelectedMainTopic; // This is the "Remedial Law" part

        public SubtopicAdapter(List<Subtopic> subtopicList, String selectedMainTopic) {
            this.subtopicList = subtopicList;
            this.adapterSelectedMainTopic = selectedMainTopic;
        }

        @NonNull
        @Override
        public SubtopicViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_subtopic_button, parent, false);
            return new SubtopicViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull SubtopicViewHolder holder, int position) {
            Subtopic subtopic = subtopicList.get(position);
            holder.subtopicButton.setText(subtopic.getTitle()); // e.g., "Civil Procedure"

            holder.subtopicButton.setOnClickListener(v -> {
                // **** CRITICAL CHANGE HERE ****
                // Launch ReviewerContentActivity, NOT ChapterViewerActivity
                Intent intent = new Intent(v.getContext(), ReviewerContentActivity.class);

                // Pass the necessary information for ReviewerContentActivity
                // MAIN_TOPIC is the "Remedial Law"
                intent.putExtra("MAIN_TOPIC", adapterSelectedMainTopic);

                // SUB_TOPIC is the document ID of the subtopic (e.g., "Civil Procedure")
                intent.putExtra("SUB_TOPIC", subtopic.getTitle());

                // No need to pass the subtopic document path to ReviewerContentActivity
                // as ReviewerContentActivity will construct the 'chapters' collection path itself
                // (e.g., "codals/Remedial Law/subtopics/Civil Procedure/chapters")

                Log.d("SubtopicAdapter", "Launching ReviewerContentActivity with:");
                Log.d("SubtopicAdapter", "  MAIN_TOPIC: " + adapterSelectedMainTopic);
                Log.d("SubtopicAdapter", "  SUB_TOPIC: " + subtopic.getTitle());

                v.getContext().startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return subtopicList.size();
        }

        public static class SubtopicViewHolder extends RecyclerView.ViewHolder {
            Button subtopicButton;

            public SubtopicViewHolder(@NonNull View itemView) {
                super(itemView);
                subtopicButton = itemView.findViewById(R.id.subtopic_button);
            }
        }
    }
}