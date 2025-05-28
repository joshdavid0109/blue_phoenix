package com.example.bluephoenix;

import static android.content.ContentValues.TAG;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.navigation.NavigationView;
import com.google.android.material.textfield.TextInputLayout;

import com.example.bluephoenix.adapters.CommentAdapter;
import com.example.bluephoenix.models.Comment;
import com.example.bluephoenix.models.ForumPost;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ForumPostDetailActivity extends AppCompatActivity implements
        NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;

    // Views for displaying the forum post details
    private TextView detailPostTitle, detailAuthor, detailDate, detailContent, detailCommentsCount;

    // Views for the comment input section
    private EditText commentEditText;
    private Button postCommentButton;

    // Views from the shared header block
    private ImageView menuIcon;
    private TextInputLayout logOutBtn; // Assuming this is for a logout icon/button
    private ImageView logoImageView;
    private TextView userDisplayNameTextView;
    private TextView mainGreetingTextView;
    private EditText homeSearchField; // Hidden in this activity but initialized

    // RecyclerView for comments
    private RecyclerView commentsRecyclerView;
    private CommentAdapter commentAdapter;
    private List<Comment> commentsList;

    // Data for the current post and logged-in user
    private ForumPost currentPost;
    private String loggedInUserId;
    private String loggedInUserName;

    // Firebase Firestore instance and collection reference for comments
    private FirebaseFirestore db;
    private CollectionReference commentsRef;

    private EditText backButton;

    private TextView nav_name;

    @SuppressLint({"MissingInflatedId", "SetTextI18n"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forum_post_detail);

        // 1. Initialize DrawerLayout and NavigationView
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);

        // 2. Initialize Firebase Firestore
        db = FirebaseFirestore.getInstance();

        // ** THIS IS THE CRITICAL CHANGE **
        // Find nav_name from the NavigationView's header view, not the main activity layout
        if (navigationView != null) {
            navigationView.setNavigationItemSelectedListener(this);

            // Get the header view from the navigation view
            // Assuming your nav_header_main.xml is the first (and possibly only) header
            View headerView = navigationView.getHeaderView(0);
            if (headerView != null) {
                nav_name = headerView.findViewById(R.id.nav_header_title);
                if (nav_name == null) {
                    Log.e("ForumPostDetailActivity", "nav_header_title (TextView) not found in nav_header_main.xml.");
                } else {
                    Log.d("ForumPostDetailActivity", "nav_name found in NavigationView header.");
                }
            } else {
                Log.e("ForumPostDetailActivity", "NavigationView header view is null. Check app:headerLayout in your NavigationView XML.");
            }
        } else {
            Log.e("ForumPostDetailActivity", "NavigationView not found (R.id.nav_view). Check your XML layout.");
        }


        // 3. Retrieve ForumPost data and user info from the Intent
        if (getIntent() != null && getIntent().hasExtra("FORUM_POST")) {
            currentPost = getIntent().getParcelableExtra("FORUM_POST");
            loggedInUserId = getIntent().getStringExtra("USER_ID");
            loggedInUserName = getIntent().getStringExtra("USER_NAME");


            Log.d("ForumDetail", "Intent check: FORUM_POST extra received.");
            if (currentPost != null) {
                Log.d("ForumDetail", "Current Post Object acquired.");
                Log.d("ForumDetail", "Post Title (from object): " + currentPost.getTitle());
                Log.d("ForumDetail", "Post Author (from object): " + currentPost.getAuthor());
                Log.d("ForumDetail", "Post Content (from object): " + currentPost.getMessage());
                Log.d("ForumDetail", "Post ID (from object): " + currentPost.getId());
            } else {
                Log.e("ForumDetail", "currentPost object is NULL after retrieving from Intent!");
            }
            Log.d("ForumDetail", "User ID (from Intent): " + loggedInUserId);
            Log.d("ForumDetail", "User Name (from Intent): " + loggedInUserName);

            // Set the nav_name text ONLY AFTER you've tried to find it in the header
            if (nav_name != null && loggedInUserName != null) {
                nav_name.setText(loggedInUserName);
                Log.d(TAG, "Nav header title set to: " + loggedInUserName);
            } else {
                Log.w(TAG, "Cannot set nav_name text. nav_name is null or loggedInUserName is null.");
            }

        } else {
            Log.e("ForumDetail", "No ForumPost object received via Intent! Finishing activity.");
            Toast.makeText(this, "Error: Post details not found.", Toast.LENGTH_SHORT).show();
            finish(); // Close activity if no post data is received
            return;
        }

        // 4. Initialize UI elements from the XML layout
        // Post Details
        detailPostTitle = findViewById(R.id.detail_post_title);
        detailAuthor = findViewById(R.id.detail_author);
        detailDate = findViewById(R.id.detail_post_date);
        detailContent = findViewById(R.id.detail_post_content);
        detailCommentsCount = findViewById(R.id.detail_comments_count);

        if (detailPostTitle == null) Log.e("ForumDetail", "detailPostTitle is NULL. Check R.id.detail_post_title in XML.");
        if (detailAuthor == null) Log.e("ForumDetail", "detailAuthor is NULL. Check R.id.detail_author in XML.");
        if (detailDate == null) Log.e("ForumDetail", "detailDate is NULL. Check R.id.detail_post_date in XML.");
        if (detailContent == null) Log.e("ForumDetail", "detailContent is NULL. Check R.id.detail_post_content in XML.");


        // Comment Input
        commentEditText = findViewById(R.id.comment_edit_text);
        postCommentButton = findViewById(R.id.post_comment_button);

        // Header elements (from the shared layout)
        menuIcon = findViewById(R.id.menu_icon);
        logOutBtn = findViewById(R.id.logOut_ic);
        logoImageView = findViewById(R.id.imageView);
        userDisplayNameTextView = findViewById(R.id.user_display_name);
        mainGreetingTextView = findViewById(R.id.main_greeting);
        homeSearchField = findViewById(R.id.home_search_field);

        // 5. Setup Comments RecyclerView
        commentsRecyclerView = findViewById(R.id.comments_recycler_view);
        commentsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        commentsList = new ArrayList<>();
        commentAdapter = new CommentAdapter(commentsList);
        commentsRecyclerView.setAdapter(commentAdapter);

        backButton = findViewById(R.id.back_button);

        backButton.setOnClickListener(v -> {
            Intent loginIntent = new Intent(this, ForumActivity.class);
            startActivity(loginIntent);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        });

        // 6. Populate UI with current post details
        if (currentPost != null) {
            String title = currentPost.getTitle();
            String author = currentPost.getAuthor();
            String date = currentPost.getDate();
            String content = currentPost.getMessage();
            int commentsCount = currentPost.getNumberOfComments();

            Log.d("ForumDetail", "Attempting to set text for detailPostTitle with: " + title);
            Log.d("ForumDetail", "Attempting to set text for detailAuthor with: " + author);
            Log.d("ForumDetail", "Attempting to set text for detailDate with: " + date);
            Log.d("ForumDetail", "Attempting to set text for detailContent with: " + content);


            detailPostTitle.setText(title);
            detailAuthor.setText("By " + author);
            detailDate.setText(" • " + date); // Or format timestamp if you use it
            detailContent.setText(content);
            updateCommentsCount(commentsCount);
        } else {
            Log.e("ForumDetail", "currentPost is NULL when attempting to populate UI!");
        }


        // 7. Configure header elements and their actions
        menuIcon.setOnClickListener(v -> {
            Log.d("MENUICON", "onCreate: menu clicked");
            if (drawerLayout != null && navigationView != null) { // Added null checks for safety
                if (drawerLayout.isDrawerOpen(navigationView)) {
                    drawerLayout.closeDrawer(navigationView);
                } else {
                    drawerLayout.openDrawer(navigationView);
                }
            } else {
                Log.e(TAG, "DrawerLayout or NavigationView is null, cannot open/close drawer.");
            }
        });
        menuIcon.setImageResource(R.drawable.ic_side_bar);
        menuIcon.setColorFilter(getColor(R.color.bp_name_light_blue));

        if (loggedInUserName != null && !loggedInUserName.isEmpty()) {
            userDisplayNameTextView.setText("Hello, " + loggedInUserName);
            mainGreetingTextView.setText(getGreetingBasedOnTime());
        } else {
            userDisplayNameTextView.setText("Hello!");
            mainGreetingTextView.setText("Welcome!");
        }

        homeSearchField.setVisibility(View.GONE);
        logOutBtn.setVisibility(View.GONE);

        // 8. Set up Firestore reference for comments related to THIS post
        // Ensure currentPost is not null before accessing its ID
        if (currentPost != null && currentPost.getId() != null) {
            commentsRef = db.collection("posts").document(currentPost.getId()).collection("comments");
            // 9. Load existing comments for this post
            loadComments();
        } else {
            Log.e(TAG, "currentPost or its ID is null, cannot load comments.");
            // Handle this case, perhaps by showing an error to the user
        }


        // 10. Set up listener for the Post Comment Button
        postCommentButton.setOnClickListener(v -> postComment());
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        Intent navIntent = null;

        // Handle navigation clicks
        if (id == R.id.nav_home) {
            Toast.makeText(this, "Home selected", Toast.LENGTH_SHORT).show();
            // Example: navIntent = new Intent(this, HomeActivity.class);
            // You might want to pass user data if HomeActivity needs it
             navIntent.putExtra("USER_ID", loggedInUserId);
             navIntent.putExtra("USER_NAME", loggedInUserName);
        } else if (id == R.id.nav_gallery) { // Assuming nav_gallery is your calendar item
            Toast.makeText(this, "Calendar selected", Toast.LENGTH_SHORT).show();
            // Example: navIntent = new Intent(this, CalendarActivity.class);
        } else if (id == R.id.nav_slideshow) { // Assuming nav_slideshow is your forum item
            Toast.makeText(this, "Peer Review and Discussion selected", Toast.LENGTH_SHORT).show();
            // If already in ForumPostDetailActivity, navigate to ForumActivity main list
            navIntent = new Intent(this, ForumActivity.class); // Navigates to the main forum list
            navIntent.putExtra("USER_ID", loggedInUserId);
            navIntent.putExtra("USER_NAME", loggedInUserName);
        }
//        else if (id == R.id.nav_about_us) { // Assuming nav_about_us is About Us
//            Toast.makeText(this, "About Us selected", Toast.LENGTH_SHORT).show();
//            // Example: navIntent = new Intent(this, AboutUsActivity.class);
//        } else if (id == R.id.nav_faqs) { // Assuming nav_faqs is FAQs
//            Toast.makeText(this, "FAQs selected", Toast.LENGTH_SHORT).show();
//            // Example: navIntent = new Intent(this, FAQsActivity.class);
//        }

        // Start the new activity if an intent was created
        if (navIntent != null) {
            startActivity(navIntent);
            // Add transition animations for a smoother feel
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish(); // Finish current activity to prevent back stack issues and redundant instances
        }

        drawerLayout.closeDrawer(navigationView); // Close the drawer after an item is selected
        return true;
    }

    @Override
    public void onBackPressed() {
        // If the drawer is open, close it instead of going back
        if (drawerLayout.isDrawerOpen(navigationView)) {
            drawerLayout.closeDrawer(navigationView);
        } else {
            super.onBackPressed(); // Perform default back button action
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right); // Add exit transition
        }
    }

    /**
     * Updates the displayed count of comments.
     * @param count The current number of comments.
     */
    @SuppressLint("SetTextI18n")
    private void updateCommentsCount(int count) {
        detailCommentsCount.setText(count + (count == 1 ? " Comment" : " Comments"));
    }

    /**
     * Loads comments for the current post from Firestore and updates the RecyclerView.
     * Uses a snapshot listener for real-time updates.
     */
    @SuppressLint("NotifyDataSetChanged")
    private void loadComments() {
        commentsRef.orderBy("timestamp", Query.Direction.ASCENDING) // Order comments by timestamp
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.w("ForumDetail", "Listen failed for comments.", e);
                        Toast.makeText(ForumPostDetailActivity.this, "Error loading comments.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (snapshots != null && !snapshots.isEmpty()) {
                        commentsList.clear();
                        for (DocumentSnapshot doc : snapshots) {

                            Comment comment = doc.toObject(Comment.class);
                            Log.e("COMMENTS", comment.toString());
                            if (comment != null) {
                                commentsList.add(comment);
                            }
                        }
                        commentAdapter.notifyDataSetChanged(); // Notify adapter of data change
                        updateCommentsCount(commentsList.size()); // Update the comments count display
                        Log.d("ForumDetail", "Comments loaded: " + commentsList.size());
                    } else {
                        commentsList.clear(); // No comments
                        commentAdapter.notifyDataSetChanged();
                        updateCommentsCount(0);
                        Log.d("ForumDetail", "No comments yet for this post.");
                    }
                });
    }

    /**
     * Handles posting a new comment to Firestore.
     */
    private void postComment() {
        String commentText = commentEditText.getText().toString().trim();

        if (commentText.isEmpty()) {
            Toast.makeText(this, "Comment cannot be empty.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Ensure user ID and name are available before posting
        if (loggedInUserId == null || loggedInUserName == null || loggedInUserId.isEmpty() || loggedInUserName.isEmpty()) {
            Toast.makeText(this, "User information missing. Cannot post comment.", Toast.LENGTH_SHORT).show();
            Log.e("ForumDetail", "Attempted to post comment without valid user ID or name.");
            return;
        }

        // Create a new Comment object
        Comment newComment = new Comment(loggedInUserId, loggedInUserName, commentText, new Timestamp(new Date()));

        // Add the comment to the 'comments' subcollection in Firestore
        commentsRef.add(newComment)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(ForumPostDetailActivity.this, "Comment posted!", Toast.LENGTH_SHORT).show();
                    commentEditText.setText(""); // Clear the input field

                    // Update the total number of comments on the parent ForumPost document
                    db.collection("posts").document(currentPost.getId())
                            .update("numberOfComments", currentPost.getNumberOfComments() + 1)
                            .addOnSuccessListener(aVoid -> {
                                Log.d("ForumDetail", "Post comment count updated.");
                                // Also update the local currentPost object to reflect the new count
                                currentPost.setNumberOfComments(currentPost.getNumberOfComments() + 1);
                            })
                            .addOnFailureListener(e -> Log.e("ForumDetail", "Error updating post comment count", e));

                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ForumPostDetailActivity.this, "Failed to post comment: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("ForumDetail", "Error adding comment", e);
                });
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
}