package com.example.bluephoenix;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager; // Import for RecyclerView layout manager
import androidx.recyclerview.widget.RecyclerView; // Import RecyclerView

import com.example.bluephoenix.models.ForumPost;
import com.google.android.material.navigation.NavigationView;

import com.example.bluephoenix.adapters.ForumPostAdapter; // Import your custom adapter

import java.util.ArrayList;
import java.util.List;

public class ForumActivity extends AppCompatActivity implements
        NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView; // Keep if you use it in XML
    private ImageView menuIcon;
    private EditText homeSearchField; // For the search bar

    // RecyclerView components
    private RecyclerView forumRecyclerView;
    private ForumPostAdapter forumPostAdapter;
    private List<ForumPost> forumPosts; // Data list for the RecyclerView

    @SuppressLint("MissingInflatedId") // You can often remove this after fixing all IDs
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forum); // Correct layout, as updated

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.drawer_layout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize top bar and drawer layout components
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view); // Uncomment if NavigationView is in use and ID is correct
        menuIcon = findViewById(R.id.menu_icon);
        homeSearchField = findViewById(R.id.home_search_field);

        // Set up the navigation drawer listener
        if (navigationView != null) {
            navigationView.setNavigationItemSelectedListener(this);
        } else {
            Log.e("ForumActivity", "NavigationView not found (R.id.nav_view). Please ensure it's uncommented and has the correct ID in your XML.");
        }

        // Set up menu icon click listener to open/close drawer
        menuIcon.setOnClickListener(v -> {
            if (drawerLayout.isDrawerOpen(navigationView != null ? navigationView : findViewById(R.id.nav_view))) {
                drawerLayout.closeDrawer(navigationView != null ? navigationView : findViewById(R.id.nav_view));
            } else {
                drawerLayout.openDrawer(navigationView != null ? navigationView : findViewById(R.id.nav_view));
            }
        });

        // --- RecyclerView Setup ---
        forumRecyclerView = findViewById(R.id.forum_recycler_view);
        forumRecyclerView.setLayoutManager(new LinearLayoutManager(this)); // Use LinearLayoutManager for vertical scrolling

        // Prepare your data. In a real app, you'd fetch this from a database or API.
        forumPosts = new ArrayList<>();
        forumPosts.add(new ForumPost("Can someone explain the Parole Evidence Rule", "John Doe", "May 24, 2025", 5));
        forumPosts.add(new ForumPost("Latest Updates on Commercial Law Amendments", "Jane Smith", "May 22, 2025", 8));
        forumPosts.add(new ForumPost("Question on Stare Decisis Application", "Alice Brown", "May 20, 2025", 3));
        forumPosts.add(new ForumPost("Tips for Bar Exam - Criminal Law", "Bob White", "May 18, 2025", 15));
        forumPosts.add(new ForumPost("Dissecting the New Family Code Amendments", "Charlie Green", "May 15, 2025", 7));
        forumPosts.add(new ForumPost("Understanding Tax Law Principles", "Alex P.", "June 1, 2025", 12));
        forumPosts.add(new ForumPost("Insights on Intellectual Property Rights", "Maria C.", "May 30, 2025", 10));
        // Add more ForumPost objects as needed

        // Initialize and set the adapter for the RecyclerView
        forumPostAdapter = new ForumPostAdapter(forumPosts);
        forumRecyclerView.setAdapter(forumPostAdapter);
        // --- End RecyclerView Setup ---

        // Handle search field input (optional, just an example)
        homeSearchField.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH ||
                    (event != null && event.getKeyCode() == android.view.KeyEvent.KEYCODE_ENTER)) {
                Toast.makeText(ForumActivity.this, "Searching for: " + v.getText(), Toast.LENGTH_SHORT).show();
                // Implement your search/filtering logic here
                // For example, filter forumPosts and then call forumPostAdapter.updateData(filteredList);
                return true;
            }
            return false;
        });

        Log.d("ForumActivity", "onCreate() finished.");
    }

    // --- Navigation Drawer Callbacks ---
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_home) {
            Toast.makeText(this, "Home selected", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_gallery) {
            Toast.makeText(this, "Calendar selected", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_slideshow) {
            Toast.makeText(this, "Peer Review and Discussion selected", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_share) {
            Toast.makeText(this, "About Us selected", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_send) {
            Toast.makeText(this, "FAQs selected", Toast.LENGTH_SHORT).show();
        }
//        else if (id == R.id.nav_logout) {
//            Toast.makeText(this, "Logout clicked", Toast.LENGTH_SHORT).show();
//        }

        // Use 'navigationView' reference if it's not null, otherwise fallback to finding by ID
        drawerLayout.closeDrawer(navigationView != null ? navigationView : findViewById(R.id.nav_view));
        return true;
    }

    @Override
    public void onBackPressed() {
        // Use 'navigationView' reference if it's not null, otherwise fallback to finding by ID
        if (drawerLayout.isDrawerOpen(navigationView != null ? navigationView : findViewById(R.id.nav_view))) {
            drawerLayout.closeDrawer(navigationView != null ? navigationView : findViewById(R.id.nav_view));
        } else {
            super.onBackPressed();
        }
    }
}