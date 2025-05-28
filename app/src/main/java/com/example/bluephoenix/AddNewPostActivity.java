package com.example.bluephoenix;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText; // For the back button (if it's an EditText)
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.google.android.material.textfield.TextInputEditText; // For TextInputLayout's EditText
import com.google.android.material.textfield.TextInputLayout; // For TextInputLayout itself
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

// Assuming you have this model class
import com.example.bluephoenix.models.ForumPost;

public class AddNewPostActivity extends AppCompatActivity implements
        NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "AddNewPostActivity";

    // DrawerLayout and NavigationView for the side drawer
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;

    // Views from the shared header block (top of the screen)
    private ImageView menuIcon;
    private TextInputLayout logOutBtn; // Assuming this is the logout icon in your header
    private ImageView logoImageView;
    private TextView userDisplayNameTextView;
    private TextView mainGreetingTextView;
    private EditText homeSearchField; // This will be hidden in this activity

    // Views specific to adding a new post
    private TextView backButton; // Assuming it's a TextView with drawableStart
    private TextInputEditText editTextTitle;
    private TextInputEditText editTextContent;
    private Button buttonSubmitPost;
    private Button buttonCancelPost;

    // Data for the current logged-in user
    private String loggedInUserId;
    private String loggedInUserName;

    // Firebase Firestore instance
    private FirebaseFirestore db;

    // For the navigation header name
    private TextView nav_name;

    @SuppressLint({"MissingInflatedId", "SetTextI18n"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_forum_post); // Ensure this points to your new XML

        // 1. Initialize DrawerLayout and NavigationView
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);

        // Set up the navigation listener for the drawer and get nav_name
        if (navigationView != null) {
            navigationView.setNavigationItemSelectedListener(this);
            View headerView = navigationView.getHeaderView(0); // Get the first header layout
            if (headerView != null) {
                nav_name = headerView.findViewById(R.id.nav_header_title);
            } else {
                Log.e(TAG, "NavigationView header view is null.");
            }
        } else {
            Log.e(TAG, "NavigationView not found (R.id.nav_view). Check your XML layout.");
        }

        // 2. Initialize Firebase Firestore
        db = FirebaseFirestore.getInstance();

        // 3. Get user info from Intent (passed from ForumActivity or HomeActivity)
        if (getIntent() != null) {
            loggedInUserId = getIntent().getStringExtra("USER_ID");
            loggedInUserName = getIntent().getStringExtra("USER_NAME");

            if (loggedInUserId == null || loggedInUserName == null || loggedInUserId.isEmpty() || loggedInUserName.isEmpty()) {
                Log.e(TAG, "User ID or Name is null/empty from Intent. Cannot add post.");
                Toast.makeText(this, "Error: User information missing. Please log in again.", Toast.LENGTH_LONG).show();
                finish(); // Close activity if user info is critical for posting
                return;
            }
            Log.d(TAG, "Logged in as: " + loggedInUserName + " (ID: " + loggedInUserId + ")");
        } else {
            Log.e(TAG, "No Intent received for AddNewPostActivity.");
            Toast.makeText(this, "Error: No user data received.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // 4. Initialize UI elements from activity_add_new_post.xml
        // Header elements (common across activities)
        menuIcon = findViewById(R.id.menu_icon);
        logOutBtn = findViewById(R.id.logOut_ic);
        logoImageView = findViewById(R.id.imageView);
        userDisplayNameTextView = findViewById(R.id.user_display_name);
        mainGreetingTextView = findViewById(R.id.main_greeting);
        homeSearchField = findViewById(R.id.home_search_field); // This will be hidden

        // Specific to Add New Post
        backButton = findViewById(R.id.add_post_back_button);
        editTextTitle = findViewById(R.id.edit_text_title);
        editTextContent = findViewById(R.id.edit_text_content);
        buttonSubmitPost = findViewById(R.id.button_submit_post);
        buttonCancelPost = findViewById(R.id.button_cancel_post);

        // 5. Configure common header elements and their actions
        menuIcon.setOnClickListener(v -> {
            if (drawerLayout != null && navigationView != null) {
                if (drawerLayout.isDrawerOpen(navigationView)) {
                    drawerLayout.closeDrawer(navigationView);
                } else {
                    drawerLayout.openDrawer(navigationView);
                }
            }
        });
        menuIcon.setImageResource(R.drawable.ic_side_bar);
        menuIcon.setColorFilter(getColor(R.color.bp_name_light_blue));

        // Update user greeting and name in header
        if (loggedInUserName != null && !loggedInUserName.isEmpty()) {
            userDisplayNameTextView.setText("Hello, " + loggedInUserName);
            mainGreetingTextView.setText(getGreetingBasedOnTime());
            if (nav_name != null) {
                nav_name.setText(loggedInUserName); // Set navigation drawer header name
            }
        } else {
            userDisplayNameTextView.setText("Hello!");
            mainGreetingTextView.setText("Welcome!");
            if (nav_name != null) {
                nav_name.setText("Guest"); // Default for nav header
            }
        }

        homeSearchField.setVisibility(View.GONE); // Hide search bar
        logOutBtn.setVisibility(View.GONE); // Hide logout icon if not relevant here

        // 6. Set up listeners for the new post buttons
        backButton.setOnClickListener(v -> navigateBackToForum());
        buttonCancelPost.setOnClickListener(v -> navigateBackToForum());
        buttonSubmitPost.setOnClickListener(v -> submitNewPost());
    }

    /**
     * Handles the submission of a new post to Firestore.
     */
    private void submitNewPost() {
        String title = editTextTitle.getText().toString().trim();
        String content = editTextContent.getText().toString().trim();

        if (title.isEmpty()) {
            editTextTitle.setError("Title cannot be empty.");
            editTextTitle.requestFocus();
            return;
        }
        if (content.isEmpty()) {
            editTextContent.setError("Content cannot be empty.");
            editTextContent.requestFocus();
            return;
        }

        // Create a new ForumPost object
        // The ID will be generated by Firestore automatically.
        // numberOfComments starts at 0 for a new post.
        // Date will be a formatted string for display, Timestamp for Firestore.
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault());
        String formattedDate = sdf.format(new Date());
        Timestamp currentTimestamp = new Timestamp(new Date());

        ForumPost newPost = new ForumPost(null, title, content, loggedInUserId, loggedInUserName, formattedDate, currentTimestamp, 0);

        // Add the post to the "posts" collection in Firestore
        db.collection("posts")
                .add(newPost)
                .addOnSuccessListener(documentReference -> {
                    // Update the local post object with the Firestore-generated ID
                    newPost.setId(documentReference.getId());
                    Log.d(TAG, "DocumentSnapshot added with ID: " + documentReference.getId());
                    Toast.makeText(AddNewPostActivity.this, "Post added successfully!", Toast.LENGTH_SHORT).show();

                    // Navigate back to ForumActivity, potentially refreshing the list
                    navigateBackToForum();
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error adding document", e);
                    Toast.makeText(AddNewPostActivity.this, "Error adding post: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Navigates back to the ForumActivity.
     */
    private void navigateBackToForum() {
        Intent intent = new Intent(AddNewPostActivity.this, ForumActivity.class);
        // Pass user info back to ForumActivity if needed for its header/context
        intent.putExtra("USER_ID", loggedInUserId);
        intent.putExtra("USER_NAME", loggedInUserName);
        startActivity(intent);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out); // Use appropriate transitions
        finish(); // Finish this activity so it's not on the back stack
    }

    /**
     * Generates a greeting based on the current time of day.
     * @return A string greeting (Good morning/afternoon/evening).
     */
    private String getGreetingBasedOnTime() {
        Date now = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("HH", Locale.getDefault());
        int hour = Integer.parseInt(sdf.format(now));

        if (hour >= 5 && hour < 12) {
            return "Good morning!";
        } else if (hour >= 12 && hour < 18) {
            return "Good afternoon!";
        } else {
            return "Good evening!";
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        Intent navIntent = null;

        if (id == R.id.nav_home) {
            Toast.makeText(this, "Home selected", Toast.LENGTH_SHORT).show();
            navIntent = new Intent(this, HomeActivity.class);
            navIntent.putExtra("USER_ID", loggedInUserId);
            navIntent.putExtra("USER_NAME", loggedInUserName); // Pass name to HomeActivity
        } else if (id == R.id.nav_gallery) { // Assuming nav_gallery is your calendar item
            Toast.makeText(this, "Calendar selected", Toast.LENGTH_SHORT).show();
            // navIntent = new Intent(this, CalendarActivity.class);
        } else if (id == R.id.nav_slideshow) { // Assuming nav_slideshow is your forum item
            Toast.makeText(this, "Peer Review and Discussion selected", Toast.LENGTH_SHORT).show();
            navIntent = new Intent(this, ForumActivity.class);
            navIntent.putExtra("USER_ID", loggedInUserId);
            navIntent.putExtra("USER_NAME", loggedInUserName);
        }
        // Add more navigation items as needed
        // else if (id == R.id.nav_about_us) { ... }
        // else if (id == R.id.nav_faqs) { ... }

        if (navIntent != null) {
            startActivity(navIntent);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish(); // Finish current activity to prevent back stack issues
        }

        drawerLayout.closeDrawer(navigationView);
        return true;
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(navigationView)) {
            drawerLayout.closeDrawer(navigationView);
        } else {
            super.onBackPressed();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        }
    }
}