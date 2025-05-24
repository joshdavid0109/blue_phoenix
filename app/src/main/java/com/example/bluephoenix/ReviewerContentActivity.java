package com.example.bluephoenix;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bluephoenix.adapters.ReviewerButtonAdapter;
import com.example.bluephoenix.models.ReviewerButton;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;

public class ReviewerContentActivity extends AppCompatActivity implements ReviewerButtonAdapter.OnButtonClickListener {

    private TextInputLayout backBtn;
    private RecyclerView recyclerView;
    private ReviewerButtonAdapter adapter;
    private TextView codalTitle1;
    private TextView codalTitle2;


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

        backBtn = findViewById(R.id.arrow_back_ic);

        backBtn.setOnClickListener(v -> {
            Intent intent = new Intent(ReviewerContentActivity.this, ReviewerActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        });
        recyclerView = findViewById(R.id.reviewer_buttons_recycler_view);
        codalTitle1 = findViewById(R.id.codal_title_1);
        codalTitle2 = findViewById(R.id.codal_title_2);

        // Set up RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Create some sample data
        List<ReviewerButton> buttons = new ArrayList<>();
        buttons.add(new ReviewerButton(getString(R.string.rem_content_rules_of_civil_procedure)));
        buttons.add(new ReviewerButton(getString(R.string.rem_content_provisional_remedies)));
        buttons.add(new ReviewerButton(getString(R.string.rem_content_special_civil_actions)));
        // Add more buttons as needed

        adapter = new ReviewerButtonAdapter(buttons, this);
        recyclerView.setAdapter(adapter);

        // Initial title setting (you might want to set this based on the section being viewed)
        codalTitle1.setText("Civil Procedure");
        codalTitle2.setText(getString(R.string.reviewer_title_remedial_law));

        // Handle the back arrow click
        findViewById(R.id.arrow_back_ic).setOnClickListener(v -> onBackPressed());
    }

    @Override
    public void onButtonClick(int position, ReviewerButton button) {
        // Handle button click here
        Toast.makeText(this, "Clicked: " + button.getButtonText(), Toast.LENGTH_SHORT).show();
        // You can use the 'position' or 'button' object to determine what action to take
    }

    @Override
    public void onHeaderTitleChange(String title1, String title2) {
        codalTitle1.setText(title1);
        codalTitle2.setText(title2);
    }

    // Example method to dynamically change the buttons and titles
    public void loadFamilyLawReviewer() {
        List<ReviewerButton> familyLawButtons = new ArrayList<>();
        familyLawButtons.add(new ReviewerButton("Family Code of the Philippines"));
        familyLawButtons.add(new ReviewerButton("Persons and Family Relations"));
        // ... add more buttons specific to Family Law

        // Update titles
        codalTitle1.setText("Family Law");
        codalTitle2.setText("Civil Law"); // Or whatever the secondary title should be

        // Update RecyclerView data
        adapter.updateData(familyLawButtons);
    }

    public void loadCriminalLawReviewer() {
        List<ReviewerButton> criminalLawButtons = new ArrayList<>();
        criminalLawButtons.add(new ReviewerButton("Revised Penal Code - Book 1"));
        criminalLawButtons.add(new ReviewerButton("Revised Penal Code - Book 2"));
        criminalLawButtons.add(new ReviewerButton("Special Penal Laws"));
        // ... add more buttons specific to Criminal Law

        // Update titles
        codalTitle1.setText("Criminal Law");
        codalTitle2.setText("Criminal Law Review");

        // Update RecyclerView data
        adapter.updateData(criminalLawButtons);
    }
}