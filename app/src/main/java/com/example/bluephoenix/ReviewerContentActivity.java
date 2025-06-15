package com.example.bluephoenix;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText; // Used for homeSearchField, if it exists in a common header
import android.widget.ImageButton;
import android.widget.ImageView; // For menu_icon, imageView (logo)
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout; // NEW
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.bluephoenix.adapters.ChapterContentAdapter;
import com.example.bluephoenix.models.ChapterContentItem;
import com.google.android.material.navigation.NavigationView; // NEW
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.l4digital.fastscroll.FastScrollRecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ReviewerContentActivity extends AppCompatActivity implements
        NavigationView.OnNavigationItemSelectedListener { // Implement the listener

    private static final String TAG = "ReviewerContentActivity";

    // Existing: Reviewer content specific views
    private TextInputLayout backBtn; // This is the arrow_back_ic in your XML
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

    // NEW: DrawerLayout and NavigationView for the main app sidebar
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;

    // NEW: Views from your specific AppBarLayout header within activity_reviewer_content.xml
    private ImageView menuIcon;
    private TextInputLayout logOutBtn; // This is the logOut_ic in your XML
    private ImageView logoImageView; // This is the imageView in your XML for the logo
    private TextView userDisplayNameTextView; // This is user_display_name in your XML
    // private TextView mainGreetingTextView; // Not directly present in your AppBarLayout in this XML, user_display_name takes its place
    // private EditText homeSearchField; // Not present in your AppBarLayout in this XML

    // NEW: Data for the current logged-in user (needed for the drawer header)
    private String loggedInUserId;
    private String loggedInUserName;

    // NEW: For the navigation header name (from nav_header_main.xml)
    private TextView nav_name;

    @SuppressLint({"MissingInflatedId", "SetTextI18n"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reviewer_content); // This XML now has DrawerLayout as root
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);

        // This listener should now be set on the DrawerLayout's main content view, which is your CoordinatorLayout
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.drawer_layout), (v, insets) -> { // Changed R.id.main to R.id.drawer_layout
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            // Apply padding to the main content area inside the drawer, not the drawer itself.
            // Your CoordinatorLayout (id/main) should handle this, or the direct child of it.
            // For simplicity, let's keep it on the root of the content being moved.
            findViewById(R.id.main).setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();
        chapterTitles = new ArrayList<>();
        chapterPositions = new ArrayList<>(); // Initialize here

        Intent incomingIntent = getIntent();
        if (incomingIntent != null) {
            currentMainTopic = incomingIntent.getStringExtra("MAIN_TOPIC");
            currentSubTopic = incomingIntent.getStringExtra("SUB_TOPIC");
            // NEW: Get user info for the main app navigation drawer
            loggedInUserId = incomingIntent.getStringExtra("USER_ID");
            loggedInUserName = incomingIntent.getStringExtra("USER_NAME");
        }

        Log.d(TAG, "Received MAIN_TOPIC: " + currentMainTopic);
        Log.d(TAG, "Received SUB_TOPIC: " + currentSubTopic);
        Log.d(TAG, "Logged in as: " + loggedInUserName + " (ID: " + loggedInUserId + ")");

        initializeViews();
        setupRecyclerView();
        setupFastScrollTocHandler(); // Keep existing TOC setup
        setupTableOfContentsButton(); // Keep existing TOC button listener

        // NEW: Setup the main app navigation drawer and its associated header elements
        setupDrawerNavigation();
        setupHeaderElementsForDrawer(); // Renamed for clarity, handles your AppBarLayout header

        // Set header titles (existing logic)
        if (currentSubTopic != null) {
            codalTitle1.setText(currentSubTopic);
        } else {
            codalTitle1.setText("Select Subtopic");
            Log.e(TAG, "currentSubTopic is null. Cannot fetch chapters.");
            Toast.makeText(this, "Error: Subtopic not provided.", Toast.LENGTH_LONG).show();
        }

        if (currentMainTopic != null) {
            codalTitle2.setText(currentMainTopic);
        } else {
            codalTitle2.setText("Select Main Topic");
            Log.e(TAG, "currentMainTopic is null. Cannot fetch chapters.");
            Toast.makeText(this, "Error: Main Topic not provided.", Toast.LENGTH_LONG).show();
        }

        fetchAllChaptersContentFromFirestore();
    }

    private void initializeViews() {
        // Existing views from your content layout
        backBtn = findViewById(R.id.arrow_back_ic);
        backBtn.setOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        });

        contentRecyclerView = findViewById(R.id.content_recycler_view_reviewer);
        codalTitle1 = findViewById(R.id.codal_title_1);
        codalTitle2 = findViewById(R.id.codal_title_2);

        // Existing TOC views
        tocButton = findViewById(R.id.toc_button);
        tocPopup = findViewById(R.id.toc_popup);
        tocContent = findViewById(R.id.toc_content);

        // NEW: Initialize DrawerLayout and NavigationView (root elements in modified XML)
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);

        // NEW: Initialize common header elements from your AppBarLayout
        menuIcon = findViewById(R.id.menu_icon);
        logOutBtn = findViewById(R.id.logOut_ic);
        logoImageView = findViewById(R.id.imageView); // Your logo ImageView in AppBarLayout
        userDisplayNameTextView = findViewById(R.id.user_display_name); // User display name in AppBarLayout

        // Note: mainGreetingTextView and homeSearchField are not present in the provided XML snippet's AppBarLayout
        // If you need them, ensure their IDs exist in your actual common header layout.
    }

    private void setupRecyclerView() {
        contentItems = new ArrayList<>();
        chapterContentAdapter = new ChapterContentAdapter(contentItems);
        contentRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        contentRecyclerView.setAdapter(chapterContentAdapter);
    }

    // Existing: This method is private and called only when the data is ready
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

    // Existing: Setup for the old TOC button
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

    // NEW: Setup the navigation drawer for the main app sidebar
    private void setupDrawerNavigation() {
        if (navigationView != null) {
            navigationView.setNavigationItemSelectedListener(this);
            View headerView = navigationView.getHeaderView(0); // Get the first header layout (nav_header_main.xml)
            if (headerView != null) {
                nav_name = headerView.findViewById(R.id.nav_header_title);
                if (nav_name != null && loggedInUserName != null) {
                    nav_name.setText(loggedInUserName); // Set navigation drawer header name
                } else if (nav_name != null) {
                    nav_name.setText("Guest"); // Default if username is null
                }
            } else {
                Log.e(TAG, "NavigationView header view is null. Check nav_header_main.xml.");
            }
        } else {
            Log.e(TAG, "NavigationView not found (R.id.nav_view). Check activity_reviewer_content.xml.");
        }
    }

    // NEW: Setup header elements in your AppBarLayout (menu icon, user greeting etc.)
    private void setupHeaderElementsForDrawer() {
        // Handle the main app menu icon click to open the drawer
        menuIcon.setOnClickListener(v -> {
            if (drawerLayout != null && navigationView != null) {
                if (drawerLayout.isDrawerOpen(navigationView)) {
                    drawerLayout.closeDrawer(navigationView);
                } else {
                    drawerLayout.openDrawer(navigationView);
                }
            }
        });
        // You already set src and tint in XML for menu_icon, no need to do it programmatically unless you want to override.
        // menuIcon.setImageResource(R.drawable.ic_side_bar);
        // menuIcon.setColorFilter(getColor(R.color.bp_name_light_blue));

        // Update user greeting/name in YOUR AppBarLayout header (user_display_name)
        if (loggedInUserName != null && !loggedInUserName.isEmpty()) {
            userDisplayNameTextView.setText("Hello, " + loggedInUserName + "!"); // Added "!" for consistency with AddNewPost
            // mainGreetingTextView is not directly in your provided XML, so removed this line:
            // mainGreetingTextView.setText(getGreetingBasedOnTime());
        } else {
            userDisplayNameTextView.setText("Hello!");
            // mainGreetingTextView.setText("Welcome!");
        }

        // Adjust visibility of other header elements as needed.
        // Based on your XML, logOut_ic is already `gone` by default, if you want to make it visible
        // logOutBtn.setVisibility(View.VISIBLE);
    }


    private void fetchAllChaptersContentFromFirestore() {
        if (currentMainTopic == null || currentSubTopic == null) {
            Log.e(TAG, "Cannot fetch chapters: Main Topic or Subtopic is null.");
            Toast.makeText(this, "Error: Missing topic information to load chapters.", Toast.LENGTH_LONG).show();
            return;
        }

        String chaptersCollectionPath = "codals/" + currentMainTopic + "/subtopics/" + currentSubTopic + "/chapters";

        Log.d(TAG, "Fetching all chapter content from: " + chaptersCollectionPath);

        db.collection(chaptersCollectionPath)
                .orderBy(com.google.firebase.firestore.FieldPath.documentId(), Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<ChapterContentItem> newContentItems = new ArrayList<>();
                    chapterTitles.clear(); // Keep this for the FastScrollTOCHandler
                    chapterPositions.clear(); // Keep this for the FastScrollTOCHandler

                    if (queryDocumentSnapshots.isEmpty()) {
                        Toast.makeText(this, "No content found for this topic.", Toast.LENGTH_SHORT).show();
                    } else {
                        Log.d(TAG, "--- Populating Chapter Data for TOC ---");
                        int currentAdapterPosition = 0; // Track adapter position manually for FastScrollTOCHandler

                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            String chapterDocumentId = document.getId();

                            // Store chapter title and its adapter position *before* adding the chapter title item
                            chapterTitles.add(chapterDocumentId); // Needed for FastScrollTOCHandler
                            chapterPositions.add(currentAdapterPosition); // Needed for FastScrollTOCHandler
                            Log.d(TAG, "Adding chapter '" + chapterDocumentId + "' at proposed adapter position: " + currentAdapterPosition);

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
                                        Log.w(TAG, "Section map in " + chapterDocumentId + " has no 'content' field or it's not a String. Map: " + sectionMap);
                                        newContentItems.add(new ChapterContentItem(ChapterContentItem.VIEW_TYPE_TEXT, "Error: Malformed content section."));
                                        currentAdapterPosition++; // Increment for error text item too
                                    }
                                }
                            } else if (sectionsObject == null) {
                                Log.w(TAG, "Document " + chapterDocumentId + " has no 'sections' field.");
                                newContentItems.add(new ChapterContentItem(ChapterContentItem.VIEW_TYPE_TEXT, "No content sections defined for " + chapterDocumentId + "."));
                                currentAdapterPosition++;
                            } else {
                                Log.e(TAG, "Document " + chapterDocumentId + " 'sections' field is not a List. Actual type: " + sectionsObject.getClass().getSimpleName());
                                newContentItems.add(new ChapterContentItem(ChapterContentItem.VIEW_TYPE_TEXT, "Error: Content for " + chapterDocumentId + " is in an unexpected format."));
                                currentAdapterPosition++;
                            }
                        }
                        Log.d(TAG, "Successfully loaded content for " + queryDocumentSnapshots.size() + " chapters.");
                        Log.d("TOC_DEBUG_ACTIVITY", "Final chapterTitles: " + chapterTitles.toString());
                        Log.d("TOC_DEBUG_ACTIVITY", "Final chapterPositions: " + chapterPositions.toString());
                        Log.d("TOC_DEBUG_ACTIVITY", "Total content items in RecyclerView: " + newContentItems.size() + " (should equal currentAdapterPosition: " + currentAdapterPosition + ")");
                    }
                    chapterContentAdapter.updateData(newContentItems);

                    // Update existing TOC handler after data changes
                    if (tocHandler == null) {
                        setupFastScrollTocHandler();
                    } else {
                        tocHandler.updateChapterData(chapterTitles, chapterPositions);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching all chapters content from Firestore: " + chaptersCollectionPath, e);
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

    /**
     * Generates a greeting based on the current time of day.
     * Copied from AddNewPostActivity.
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

    /**
     * Handles clicks on items in the main app navigation drawer.
     * Copied from AddNewPostActivity.
     */
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        Intent navIntent = null;

        if (id == R.id.nav_home) {
            Toast.makeText(this, "Home selected", Toast.LENGTH_SHORT).show();
            navIntent = new Intent(this, HomeActivity.class);
            navIntent.putExtra("USER_ID", loggedInUserId);
            navIntent.putExtra("USER_NAME", loggedInUserName);
        } else if (id == R.id.nav_gallery) { // Assuming nav_gallery is your calendar item
            Toast.makeText(this, "Calendar selected", Toast.LENGTH_SHORT).show();
            // Uncomment and set your CalendarActivity if applicable:
            // navIntent = new Intent(this, CalendarActivity.class);
        } else if (id == R.id.nav_slideshow) { // Assuming nav_slideshow is your forum item
            Toast.makeText(this, "Peer Review and Discussion selected", Toast.LENGTH_SHORT).show();
            navIntent = new Intent(this, ForumActivity.class);
            navIntent.putExtra("USER_ID", loggedInUserId);
            navIntent.putExtra("USER_NAME", loggedInUserName);
        }
        // Add more navigation items as needed from your navigation menu XML

        if (navIntent != null) {
            startActivity(navIntent);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish(); // Finish current activity to prevent back stack issues
        }

        // Close the main app navigation drawer
        if (drawerLayout != null) {
            drawerLayout.closeDrawer(navigationView);
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        // Prioritize closing the main app navigation drawer if it's open
        if (drawerLayout != null && drawerLayout.isDrawerOpen(navigationView)) {
            drawerLayout.closeDrawer(navigationView);
        }
        // Then, check and close the existing TOC popup if it's open
        else if (tocPopup.getVisibility() == View.VISIBLE) {
            tocHandler.forceHideToc(); // Use handler to hide
        }
        // Otherwise, perform the default back action
        else {
            super.onBackPressed();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right); // Or fade_in, fade_out
        }
    }
}