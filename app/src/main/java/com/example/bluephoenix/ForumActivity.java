package com.example.bluephoenix;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.navigation.NavigationView;

import com.example.bluephoenix.adapters.ForumPostAdapter;
import com.example.bluephoenix.models.ForumPost;
import com.google.firebase.auth.FirebaseAuth; // For real user authentication
import com.google.firebase.auth.FirebaseUser; // For real user authentication
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration; // To manage snapshot listeners
// import com.google.firebase.firestore.QueryDocumentSnapshot; // Not strictly needed for this file, but good for comments

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch; // Import CountDownLatch
import java.text.SimpleDateFormat; // For formatting date/time
import java.util.Date; // For Date object
import java.util.Locale; // For Locale

public class ForumActivity extends AppCompatActivity implements
        NavigationView.OnNavigationItemSelectedListener,
        ForumPostAdapter.OnItemClickListener {

    private static final String TAG = "ForumActivity"; // Tag for Logcat messages

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ImageView menuIcon;
    private EditText homeSearchField; // Will likely remain hidden for this activity

    private RecyclerView forumRecyclerView;
    private ForumPostAdapter forumPostAdapter;
    private List<ForumPost> forumPosts;

    private String loggedInUserId;
    private String loggedInUserName;

    // Firebase
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ListenerRegistration forumPostsListener; // To manage snapshot listeners

    private TextView welcomeTextView;

    private Button postBtn;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forum);
        Log.d(TAG, "onCreate: Activity started.");

        // 1. Initialize Firebase instances
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        Log.d(TAG, "onCreate: Firebase Firestore and Auth initialized.");

        welcomeTextView = findViewById(R.id.user_display_name);

        // 2. Retrieve User Data from Intent (or get from Firebase Auth if already logged in)
        // Check for passed intent data first
        Intent intent = getIntent();
        if (intent != null) {
            loggedInUserId = intent.getStringExtra("USER_ID");
            loggedInUserName = intent.getStringExtra("USER_NAME");
            Log.d(TAG, "onCreate: Received USER_ID from Intent: " + loggedInUserId + ", USER_NAME: " + loggedInUserName);
            welcomeTextView.setText("Hello, " + loggedInUserName);
        } else {
            Log.d(TAG, "onCreate: No Intent data received for user.");
        }

        // If not received via intent (e.g., direct launch, or returning from another activity without passing it back),
        // try to get from Firebase Auth. This is the more robust approach.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // Prioritize Firebase Auth for user data if available
            loggedInUserId = currentUser.getUid();
            // Get display name, or a default/email if display name is not set
            loggedInUserName = currentUser.getDisplayName() != null ? currentUser.getDisplayName() : currentUser.getEmail();
            Log.d(TAG, "onCreate: Acquired USER_ID from FirebaseAuth: " + loggedInUserId + ", USER_NAME: " + loggedInUserName);
        } else {
            // Handle cases where no user is logged in (e.g., redirect to login)
            Toast.makeText(this, "No user logged in. Some features may be restricted.", Toast.LENGTH_LONG).show();
            Log.w(TAG, "onCreate: No Firebase user is currently logged in. Consider redirecting to login.");
            // Optionally, redirect to login screen:
            // Intent loginIntent = new Intent(this, LoginActivity.class);
            // startActivity(loginIntent);
            // finish();
            // return; // Prevent further execution if user must be logged in
        }


        // 3. Initialize UI elements
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        menuIcon = findViewById(R.id.menu_icon);
        homeSearchField = findViewById(R.id.home_search_field);
        Log.d(TAG, "onCreate: UI elements initialized.");

        // 4. Setup Navigation Drawer
        if (drawerLayout == null) {
            Log.e(TAG, "onCreate: DrawerLayout not found (R.id.drawer_layout). Check your XML layout.");
        }
        if (navigationView != null) {
            navigationView.setNavigationItemSelectedListener(this);
            Log.d(TAG, "onCreate: NavigationView listener set.");

            // Set header user info if you have a TextViews in nav_header_main.xml
            View headerView = navigationView.getHeaderView(0);
            TextView navHeaderTitle = headerView.findViewById(R.id.nav_header_title);
            TextView navHeaderSubtitle = headerView.findViewById(R.id.nav_header_subtitle);

            if (navHeaderTitle != null && loggedInUserName != null) {
                navHeaderTitle.setText(loggedInUserName);
                Log.d(TAG, "onCreate: Nav header title set to: " + loggedInUserName);
            } else if (navHeaderTitle != null) {
                navHeaderTitle.setText("Guest User"); // Default for missing name
                Log.d(TAG, "onCreate: Nav header title set to default (loggedInUserName is null).");
            }
            if (navHeaderSubtitle != null && currentUser != null && currentUser.getEmail() != null) {
                navHeaderSubtitle.setText(currentUser.getEmail());
                Log.d(TAG, "onCreate: Nav header subtitle set to: " + currentUser.getEmail());
            } else if (navHeaderSubtitle != null) {
                navHeaderSubtitle.setText("No Email Available"); // Default for missing email
                Log.d(TAG, "onCreate: Nav header subtitle set to default (currentUser/email is null).");
            }
        } else {
            Log.e(TAG, "onCreate: NavigationView not found (R.id.nav_view). Please ensure it's uncommented and has the correct ID in your XML.");
        }

        menuIcon.setOnClickListener(v -> {
            Log.d(TAG, "menuIcon clicked.");
            if (drawerLayout != null && navigationView != null) { // Added null checks for safety
                if (drawerLayout.isDrawerOpen(navigationView)) {
                    drawerLayout.closeDrawer(navigationView);
                    Log.d(TAG, "Drawer closed.");
                } else {
                    drawerLayout.openDrawer(navigationView);
                    Log.d(TAG, "Drawer opened.");
                }
            } else {
                Log.e(TAG, "menuIcon click: DrawerLayout or NavigationView is null, cannot open/close drawer.");
            }
        });

        // 5. Setup Forum Posts RecyclerView
        forumRecyclerView = findViewById(R.id.forum_recycler_view);
        if (forumRecyclerView == null) {
            Log.e(TAG, "onCreate: forum_recycler_view not found. Check XML.");
        }
        forumRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        forumPosts = new ArrayList<>();
        forumPostAdapter = new ForumPostAdapter(forumPosts);
        forumPostAdapter.setOnItemClickListener(this); // Set the listener
        forumRecyclerView.setAdapter(forumPostAdapter);
        Log.d(TAG, "onCreate: RecyclerView and adapter initialized.");

        // 6. Load Forum Posts from Firestore (Real-time listener)
        loadForumPosts();

        postBtn = findViewById(R.id.post_btn);

        if (postBtn != null) { // Always good to null-check views
            postBtn.setOnClickListener(v -> {
                Intent i = new Intent(this, AddNewPostActivity.class);

                if (loggedInUserId != null && loggedInUserName != null) {
                    i.putExtra("USER_ID", loggedInUserId);
                    i.putExtra("USER_NAME", loggedInUserName);
                    startActivity(i);
                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                    finish();
                } else {
                    Log.e("ForumActivity", "Cannot create new post: User ID or Name is missing in ForumActivity.");
                    Toast.makeText(this, "User data not available. Please try again or log in.", Toast.LENGTH_LONG).show();
                }
            });
        }
        // 7. Search Field setup
        if (homeSearchField == null) {
            Log.w(TAG, "onCreate: home_search_field not found. Skipping search listener setup.");
        } else {
            homeSearchField.setOnEditorActionListener((v, actionId, event) -> {
                Log.d(TAG, "Search field action: actionId=" + actionId + ", event=" + event);
                if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH ||
                        (event != null && event.getKeyCode() == android.view.KeyEvent.KEYCODE_ENTER)) {
                    String query = v.getText().toString().trim();
                    if (!query.isEmpty()) {
                        Toast.makeText(ForumActivity.this, "Searching for: " + query, Toast.LENGTH_SHORT).show();
                        Log.i(TAG, "Search initiated for query: '" + query + "'");
                        // Implement search logic here (e.g., filter forumPosts, re-query Firestore)
                    } else {
                        Toast.makeText(ForumActivity.this, "Search field is empty.", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "Search field is empty.");
                    }
                    return true;
                }
                return false;
            });
            // The search field from the HomeActivity layout is likely not relevant here.
            // It's probably better to hide it for this activity if it's not used.
            // homeSearchField.setVisibility(View.GONE); // Uncomment if you want to hide it
            Log.d(TAG, "onCreate: Search field listener set.");
        }

        Log.d(TAG, "onCreate: ForumActivity finished.");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Detach the Firestore listener to prevent memory leaks
        if (forumPostsListener != null) {
            forumPostsListener.remove();
            Log.d(TAG, "onDestroy: Firestore listener removed.");
        }
        Log.d(TAG, "onDestroy: Activity destroyed.");
    }

    @Override
    public void onItemClick(ForumPost post) {
        Log.d(TAG, "onItemClick: Post clicked: " + post.getTitle() + " (ID: " + post.getId() + ")");

        // Check if user info is available before navigating to detail
        if (loggedInUserId == null || loggedInUserName == null) {
            Toast.makeText(this, "User not authenticated. Please log in.", Toast.LENGTH_SHORT).show();
            Log.w(TAG, "onItemClick: User ID or Name is null, preventing navigation to ForumPostDetailActivity.");
            // Optionally, redirect to login activity here
            // Intent loginIntent = new Intent(this, LoginActivity.class);
            // startActivity(loginIntent);
            return;
        }

        Intent detailIntent = new Intent(ForumActivity.this, ForumPostDetailActivity.class);
        detailIntent.putExtra("FORUM_POST", post); // Pass the entire Parcelable object
        detailIntent.putExtra("USER_ID", loggedInUserId); // Pass current user's ID
        detailIntent.putExtra("USER_NAME", loggedInUserName); // Pass current user's name
        Log.d(TAG, "onItemClick: Starting ForumPostDetailActivity with Post ID: " + post.getId() +
                ", User ID: " + loggedInUserId + ", User Name: " + loggedInUserName);
        startActivity(detailIntent);
        // Apply appropriate transition for navigating to a detail view
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right); // Slide in from right, current slides out left
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        Log.d(TAG, "onNavigationItemSelected: Item selected with ID: " + getResources().getResourceEntryName(id));
        Intent navIntent = null; // Declare navIntent here

        if (id == R.id.nav_home) {
            Toast.makeText(this, "Navigating to Home.", Toast.LENGTH_SHORT).show();
            navIntent = new Intent(ForumActivity.this, HomeActivity.class);
            if (loggedInUserId != null && loggedInUserName != null) { // Always pass user data if possible
                navIntent.putExtra("USER_ID", loggedInUserId);
                navIntent.putExtra("USER_NAME", loggedInUserName);
                Log.d(TAG, "onNavigationItemSelected: Passing USER_ID: " + loggedInUserId + ", USER_NAME: " + loggedInUserName + " to HomeActivity.");
            } else {
                Log.w(TAG, "onNavigationItemSelected: User ID or Name is null, not passing to HomeActivity.");
            }
        } else if (id == R.id.nav_gallery) { // Assuming nav_gallery is your calendar item
            // IMPORTANT: Only uncomment this if CalendarActivity.java exists and is declared in AndroidManifest.xml
            // If you still have this item in your menu XML, this block WILL be entered.
            // If CalendarActivity doesn't exist, uncommenting the line below WILL cause ActivityNotFoundException.
            Toast.makeText(this, "Calendar selected (Feature not yet implemented).", Toast.LENGTH_SHORT).show();
            Log.w(TAG, "onNavigationItemSelected: Calendar item selected, but CalendarActivity is not implemented.");
            // navIntent = new Intent(ForumActivity.this, CalendarActivity.class); // Keep commented for now
            // if (loggedInUserId != null && loggedInUserName != null) {
            //     navIntent.putExtra("USER_ID", loggedInUserId);
            //     navIntent.putExtra("USER_NAME", loggedInUserName);
            // }
        } else if (id == R.id.nav_slideshow) { // Assuming nav_slideshow is your forum item
            Toast.makeText(this, "You are already viewing Forum posts.", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "onNavigationItemSelected: Forum item selected, already in ForumActivity. No navigation.");
            // Do not set navIntent if you're staying on the same activity, so the 'if (navIntent != null)' block is skipped.
            // Or set it if you *do* want to restart the activity (e.g., for refresh).
            // Example to restart:
            // navIntent = new Intent(ForumActivity.this, ForumActivity.class);
            // if (loggedInUserId != null && loggedInUserName != null) {
            //     navIntent.putExtra("USER_ID", loggedInUserId);
            //     navIntent.putExtra("USER_NAME", loggedInUserName);
            // }
        } else if (id == R.id.nav_share) { // About Us (Assuming this was the correct ID for About Us)
            Toast.makeText(this, "About Us selected (Feature not yet implemented).", Toast.LENGTH_SHORT).show();
            Log.w(TAG, "onNavigationItemSelected: About Us item selected, but AboutUsActivity is not implemented.");
            // navIntent = new Intent(ForumActivity.this, AboutUsActivity.class); // Keep commented for now
            // if (loggedInUserId != null && loggedInUserName != null) {
            //     navIntent.putExtra("USER_ID", loggedInUserId);
            //     navIntent.putExtra("USER_NAME", loggedInUserName);
            // }
        } else if (id == R.id.nav_send) { // FAQs (Assuming this was the correct ID for FAQs)
            Toast.makeText(this, "FAQs selected (Feature not yet implemented).", Toast.LENGTH_SHORT).show();
            Log.w(TAG, "onNavigationItemSelected: FAQs item selected, but FAQsActivity is not implemented.");
            // navIntent = new Intent(ForumActivity.this, FAQsActivity.class); // Keep commented for now
            // if (loggedInUserId != null && loggedInUserName != null) {
            //     navIntent.putExtra("USER_ID", loggedInUserId);
            //     navIntent.putExtra("USER_NAME", loggedInUserName);
            // }
        } else {
            Log.w(TAG, "onNavigationItemSelected: Unhandled menu item ID: " + item.getItemId() + " (Resource Name: " + getResources().getResourceEntryName(id) + "). No navigation performed.");
            Toast.makeText(this, "Selected feature not available yet.", Toast.LENGTH_SHORT).show();
        }

        if (navIntent != null) {
            Log.d(TAG, "onNavigationItemSelected: Starting new activity: " + navIntent.getComponent().getClassName());
            startActivity(navIntent);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish(); // Finish current activity to prevent building up activity stack
        } else {
            Log.d(TAG, "onNavigationItemSelected: navIntent is null. No new activity started.");
        }

        if (drawerLayout != null && navigationView != null) { // Null check for safety
            drawerLayout.closeDrawer(navigationView);
            Log.d(TAG, "onNavigationItemSelected: Drawer closed.");
        } else {
            Log.e(TAG, "onNavigationItemSelected: DrawerLayout or NavigationView is null, cannot close drawer.");
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "onBackPressed: Back button pressed.");
        if (drawerLayout != null && navigationView != null && drawerLayout.isDrawerOpen(navigationView)) {
            drawerLayout.closeDrawer(navigationView);
            Log.d(TAG, "onBackPressed: Drawer was open, closed it.");
        } else {
            super.onBackPressed(); // Perform default back button action
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right); // Apply exit transition for this activity
            Log.d(TAG, "onBackPressed: Drawer was closed, performing default back action.");
        }
    }

    /**
     * Loads forum posts from Firestore in real-time, including comment counts.
     */
    private void loadForumPosts() {
        Log.d(TAG, "loadForumPosts: Attempting to load forum posts from collection 'posts'...");
        forumPostsListener = db.collection("posts")
                .orderBy("timestamp", Query.Direction.DESCENDING) // Assuming 'timestamp' field exists for ordering
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.e(TAG, "loadForumPosts: Listen failed for forum posts. Error: " + e.getMessage(), e);
                        Toast.makeText(ForumActivity.this, "Error loading forum posts: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        forumPosts.clear(); // Clear list on error
                        forumPostAdapter.notifyDataSetChanged();
                        return;
                    }

                    if (snapshots != null) {
                        Log.d(TAG, "loadForumPosts: Received Firestore snapshot. Number of documents: " + snapshots.size());

                        if (!snapshots.isEmpty()) {
                            forumPosts.clear(); // Clear existing data
                            Log.d(TAG, "loadForumPosts: Cleared existing forumPosts list.");

                            final CountDownLatch latch = new CountDownLatch(snapshots.size());
                            Log.d(TAG, "loadForumPosts: CountDownLatch initialized with " + snapshots.size() + " posts for comment count fetching.");

                            for (DocumentSnapshot doc : snapshots) {
                                ForumPost post = new ForumPost();
                                post.setId(doc.getId()); // Set the Firestore document ID

                                // IMPORTANT: Ensure these field names EXACTLY match your Firestore document fields.
                                post.setTitle(doc.getString("title"));
                                post.setAuthor(doc.getString("author"));
                                post.setMessage(doc.getString("message")); // Map 'message' to 'content'

                                String dateTimeString = doc.getString("date_time");
                                if (dateTimeString != null) {
                                    post.setDate(dateTimeString);
                                    Log.d(TAG, "loadForumPosts: Post " + post.getId() + " date_time: " + dateTimeString);
                                } else {
                                    com.google.firebase.Timestamp firestoreTimestamp = doc.getTimestamp("timestamp");
                                    if (firestoreTimestamp != null) {
                                        Date date = firestoreTimestamp.toDate();
                                        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault());
                                        post.setDate(sdf.format(date));
                                        Log.d(TAG, "loadForumPosts: Post " + post.getId() + " timestamp converted to date: " + post.getDate());
                                    } else {
                                        post.setDate("No Date Available");
                                        Log.w(TAG, "loadForumPosts: Post " + doc.getId() + " has no 'date_time' or 'timestamp' field. Set to 'No Date Available'.");
                                    }
                                }

                                Log.d(TAG, "loadForumPosts: Processing post: " + post.getTitle() + " (ID: " + post.getId() + ")");

                                // Get the number of comments for this post
                                db.collection("posts").document(post.getId()).collection("comments")
                                        .get()
                                        .addOnSuccessListener(commentSnapshots -> {
                                            post.setNumberOfComments(commentSnapshots.size());
                                            Log.d(TAG, "loadForumPosts: Post ID: " + post.getId() + ", Comments count fetched: " + commentSnapshots.size());
                                            forumPosts.add(post);
                                            latch.countDown(); // Decrement the latch count
                                            Log.d(TAG, "loadForumPosts: Latch count remaining: " + latch.getCount());

                                            // Check if all comment counts are fetched
                                            if (latch.getCount() == 0) {
                                                Log.d(TAG, "loadForumPosts: All comment counts fetched for posts. Notifying adapter.");
                                                forumPostAdapter.notifyDataSetChanged();
                                                Log.d(TAG, "loadForumPosts: Total posts in adapter after all fetches: " + forumPosts.size());
                                            }
                                        })
                                        .addOnFailureListener(exception -> {
                                            Log.e(TAG, "loadForumPosts: Error getting comments for post " + post.getId() + ": " + exception.getMessage(), exception);
                                            post.setNumberOfComments(0); // Set to 0 on error
                                            forumPosts.add(post); // Still add the post even if comments failed
                                            latch.countDown(); // Decrement latch even on failure
                                            Log.d(TAG, "loadForumPosts: Latch count remaining after comment fetch failure: " + latch.getCount());
                                            if (latch.getCount() == 0) {
                                                Log.d(TAG, "loadForumPosts: All comment counts processed (some failures). Notifying adapter.");
                                                forumPostAdapter.notifyDataSetChanged();
                                                Log.d(TAG, "loadForumPosts: Total posts in adapter after processing: " + forumPosts.size());
                                            }
                                        });
                            }
                        } else {
                            // Initial snapshot is empty (no posts found)
                            forumPosts.clear();
                            forumPostAdapter.notifyDataSetChanged();
                            Log.d(TAG, "loadForumPosts: Firestore snapshot received, but no forum posts found in the 'posts' collection.");
                            Toast.makeText(ForumActivity.this, "No forum posts found.", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        // This case generally shouldn't happen for addSnapshotListener unless there's a serious issue.
                        Log.e(TAG, "loadForumPosts: Firestore snapshot was null for posts collection.");
                        forumPosts.clear();
                        forumPostAdapter.notifyDataSetChanged();
                    }
                });
    }
}