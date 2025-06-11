package com.example.bluephoenix;

import static android.content.ContentValues.TAG;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.gridlayout.widget.GridLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bluephoenix.adapters.FirestoreSearchAdapter;
import com.example.bluephoenix.fragments.GetUserNameDialogFragment;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class HomeActivity extends AppCompatActivity implements GetUserNameDialogFragment.NameInputDialogListener, NavigationView.OnNavigationItemSelectedListener {
    private TextView welcomeTextView;
    private FirebaseFirestore db;
    private CardView cardViewCivil;
    private CardView cardViewCommercial;
    private CardView cardViewRem;
    private CardView cardViewConstitutional;
    private CardView cardViewCriminal;
    private CardView cardViewTaxation;

    private TextInputLayout logOutBtn;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser; // Keep a reference to the current user
    private boolean isNewUser = false;
    private String currentFirstName;
    private String currentUserId;

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ImageView menuIcon;
    private ImageView homeIcon;

    private TextView nav_username_display;

    private TextView no_results_text;

    private EditText home_search_field;

    // Search functionality
    private RecyclerView searchResultsRecyclerView;
    private FirestoreSearchAdapter searchAdapter;
    private List<SearchItem> searchResults;
    private LinearLayout contentLayout;

    private GridLayout gridLayout;

    public static final int SEARCH_REQUEST_CODE = 101;

    // Define your subcollections here - you'll need to update this based on your actual Firestore structure
    private static final Map<String, List<String>> MAIN_TOPIC_SUBCOLLECTIONS = new HashMap<>();
    static {
        // Example structure - update these based on your actual Firestore subcollections
        MAIN_TOPIC_SUBCOLLECTIONS.put("Civil Law", Arrays.asList(
                "Persons and Family Relations", "Property", "Obligations and Contracts",
                "Torts and Damages", "Land Titles and Deeds", "Muslim Personal Laws",
                "Property", "Special Contracts", "Succession"
        ));
        MAIN_TOPIC_SUBCOLLECTIONS.put("Criminal Law", Arrays.asList(
                "Revised Penal Code", "Special Penal Laws", "Criminal Procedure"
        ));
        MAIN_TOPIC_SUBCOLLECTIONS.put("Constitutional Law", Arrays.asList(
                "Bill of Rights", "Separation of Powers", "Local Government"
        ));
        MAIN_TOPIC_SUBCOLLECTIONS.put("Remedial Law", Arrays.asList(
                "Civil Procedure", "Criminal Procedure", "Evidence", "Special Proceedings",
                "Laws on Jurisdiction"
        ));
        MAIN_TOPIC_SUBCOLLECTIONS.put("Commercial Law", Arrays.asList(
                "Anti-Trust", "Banking", "Business Organizations", "Credit Transactions",
                "Data Privacy", "Financiaal Rehabilitation", "Insurance", "Intellectual Property",
                "Negotiable Instrument", "Securities", "Transportation", "Other Special Laws"
        ));
        MAIN_TOPIC_SUBCOLLECTIONS.put("Taxation Law", Arrays.asList(
                "National Internal Revenue Code", "Local Government Code", "Customs and Tariff Code"
        ));
    }

    // Search item class to hold search results
    public static class SearchItem {
        private String title;
        private String mainTopic;
        private String subTopic;
        private String originalChapterTitle; // This field is the culprit
        private String documentId;
        private String path;

        public SearchItem(String title, String mainTopic, String subTopic,
                          String originalChapterTitle, String documentId, String path) {
            // Use Objects.requireNonNullElse for defensive programming
            this.title = Objects.requireNonNullElse(title, "");
            this.mainTopic = Objects.requireNonNullElse(mainTopic, "");
            this.subTopic = Objects.requireNonNullElse(subTopic, "");
            this.originalChapterTitle = Objects.requireNonNullElse(originalChapterTitle, ""); // Defensive check here!
            this.documentId = Objects.requireNonNullElse(documentId, "");
            this.path = Objects.requireNonNullElse(path, "");
        }

        // Getters
        public String getTitle() { return title; }
        public String getMainTopic() { return mainTopic; }
        public String getSubTopic() { return subTopic; }
        public String getOriginalChapterTitle() { return originalChapterTitle; } // Null check not needed here if constructor is robust
        public String getDocumentId() { return documentId; }
        public String getPath() { return path; }

        // Equals and HashCode (Important for List.contains and duplicates)
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SearchItem that = (SearchItem) o;
            // Compare based on unique identifier, e.g., documentId or path
            return Objects.equals(documentId, that.documentId) &&
                    Objects.equals(title, that.title); // Or just documentId if that's unique enough for your purpose
        }

        @Override
        public int hashCode() {
            return Objects.hash(documentId, title); // Ensure consistent with equals
        }
    }

    @SuppressLint({"SetTextI1d", "MissingInflatedId"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance(); // Initialize mAuth first
        db = FirebaseFirestore.getInstance(); // Initialize db first

        gridLayout = findViewById(R.id.home_category_grid);
        no_results_text = findViewById(R.id.no_results_text);

        // --- IMPORTANT: Check authentication early ---
        currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            // User is not authenticated, redirect to LoginActivity
            Log.d("HomeActivity", "User not authenticated. Redirecting to LoginActivity.");
            redirectToLogin();
            return; // Stop further execution of onCreate
        } else {
            currentUserId = currentUser.getUid(); // Set currentUserId from authenticated user
            Log.d("HomeActivity", "User authenticated: " + currentUserId);
        }
        // --- End of authentication check ---

        initializeViews();
        setupSearch();
        setupCardClickListeners();
        setupUserData(); // Now setupUserData will always have an authenticated user

        // This line was previously outside the onCreate method but inside initializeViews.
        // Ensuring it's here after views are initialized for proper setup of nav_username_display.
        navigationView.setNavigationItemSelectedListener(this);
        View headerView = navigationView.getHeaderView(0);
        nav_username_display = headerView.findViewById(R.id.nav_header_title);
        navigationView.setCheckedItem(R.id.nav_home);

        // Update nav_username_display immediately if firstName is known from previous state or intent
        if (currentFirstName != null && !currentFirstName.isEmpty()) {
            nav_username_display.setText(currentFirstName);
        }
    }

    private void initializeViews() {
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        menuIcon = findViewById(R.id.menu_icon);
        homeIcon = findViewById(R.id.home_icon);

        home_search_field = findViewById(R.id.home_search_field);

        welcomeTextView = findViewById(R.id.user_display_name);
        cardViewCivil = findViewById(R.id.home_civ);
        cardViewCommercial = findViewById(R.id.home_comm);
        cardViewRem = findViewById(R.id.home_rem);
        cardViewCriminal = findViewById(R.id.home_crim);
        cardViewConstitutional = findViewById(R.id.home_consti);
        cardViewTaxation = findViewById(R.id.home_tax);
        logOutBtn = findViewById(R.id.logOut_ic);

        // mAuth and db are now initialized in onCreate before this
        // db = FirebaseFirestore.getInstance();
        // mAuth = FirebaseAuth.getInstance();

        menuIcon.setOnClickListener(v -> {
            if (drawerLayout.isDrawerOpen(navigationView)) {
                drawerLayout.closeDrawer(navigationView);
            } else {
                drawerLayout.openDrawer(navigationView);
            }
        });

        homeIcon.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, HomeActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish(); // Finish current activity to prevent building up activity stack
        });

        logOutBtn.setOnClickListener(v -> showLogoutConfirmationDialog());
    }

    // Enhanced search implementation - Add these methods to your HomeActivity class

    private Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;
    private static final int SEARCH_DELAY_MS = 300; // Debounce delay

    private void setupSearch() {
        // Initialize RecyclerView and Adapter
        searchResultsRecyclerView = findViewById(R.id.searchResultsRecyclerView);
        searchResultsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        searchResults = new ArrayList<>();
        searchAdapter = new FirestoreSearchAdapter(searchResults, item -> {
            // Handle click on a search result item
            navigateToContent(item);
            home_search_field.setText(""); // Clear search field after selection
            searchResultsRecyclerView.setVisibility(View.GONE); // Hide search results
            contentLayout.setVisibility(View.VISIBLE); // Show main content

            // Hide keyboard
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(home_search_field.getWindowToken(), 0);
        });
        searchResultsRecyclerView.setAdapter(searchAdapter);

        // Get the main content layout
        contentLayout = findViewById(R.id.i_codals).getParent().getParent().getParent() instanceof LinearLayout ?
                (LinearLayout) findViewById(R.id.i_codals).getParent().getParent().getParent() : findViewById(R.id.main);

        // Enhanced TextWatcher with debouncing for better performance
        home_search_field.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Cancel previous search if user is still typing
                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }

                String currentInput = s.toString().trim();

                if (currentInput.isEmpty()) {
                    // If the field is now empty (e.g., due to backspacing all text)
                    Log.d("HomeActivity", "Search field is now empty. Triggering UI update.");
                    // Clear any previous search results from the adapter and list
                    searchResults.clear();
                    searchAdapter.notifyDataSetChanged(); // Notifying here ensures RecyclerView is empty
                    updateSearchResultsUI(); // <-- This is the crucial call
                    return; // Stop here, no need to perform search or debounce
                }

                // For very short queries, clear results and hide search views.
                // Do NOT call updateSearchResultsUI here, as it would show the main content.
                // The main content should remain hidden until the query is truly empty.
                if (currentInput.length() <= 1) {
                    searchResults.clear();
                    searchAdapter.notifyDataSetChanged();
                    searchResultsRecyclerView.setVisibility(View.GONE);
                    no_results_text.setVisibility(View.GONE);
                    // The mainContentScrollView and gridLayout would have been hidden by
                    // the previous call to updateSearchResultsUI when the query was not empty.
                    return;
                }

                // Create new search runnable with delay (debouncing)
                searchRunnable = () -> searchFirestore(currentInput);
                searchHandler.postDelayed(searchRunnable, SEARCH_DELAY_MS);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Handle search action from keyboard
        home_search_field.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    actionId == EditorInfo.IME_ACTION_DONE ||
                    (event != null && event.getAction() == KeyEvent.ACTION_DOWN &&
                            event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                performSearch();

                // Hide keyboard
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(home_search_field.getWindowToken(), 0);
                return true;
            }
            return false;
        });

        // Handle focus changes
        home_search_field.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && !home_search_field.getText().toString().trim().isEmpty()) {
                String query = home_search_field.getText().toString().trim();
                if (query.length() > 1) {
                    searchFirestore(query);
                }
            } else if (!hasFocus && searchResults.isEmpty()) {
                searchResultsRecyclerView.setVisibility(View.GONE);
                contentLayout.setVisibility(View.VISIBLE);
            }
        });
        home_search_field.setOnFocusChangeListener((v, hasFocus) -> {
            String currentQuery = home_search_field.getText().toString().trim();
            if (hasFocus) {
                // If gaining focus with an existing query, initiate a search (if long enough)
                if (!currentQuery.isEmpty() && currentQuery.length() > 1) {
                    searchFirestore(currentQuery);
                }
            } else {
                // If losing focus AND the search field is empty, revert to main content
                if (currentQuery.isEmpty()) {
                    updateSearchResultsUI(); // Crucial: Reverts to main content if field empty on focus loss
                }
                // If focus is lost but there's still a query, the search results/no results state should persist.
            }
        });
    }


    // Optional: Add this method to improve search experience
    private void showSearchState(boolean isSearching) {
        runOnUiThread(() -> {
            if (isSearching) {
                // You could add a progress indicator here if desired
                // progressBar.setVisibility(View.VISIBLE);
            } else {
                // progressBar.setVisibility(View.GONE);
            }
        });
    }

    private void searchFirestore(String query) {
        // Ensure user is authenticated before performing search
        if (currentUser == null) {
            Log.e("HomeActivity", "searchFirestore: User not authenticated.");
            redirectToLogin();
            return;
        }

        if (query.trim().isEmpty()) {
            searchResultsRecyclerView.setVisibility(View.GONE);
            contentLayout.setVisibility(View.VISIBLE);
            searchResults.clear();
            searchAdapter.notifyDataSetChanged();
            return;
        }

        // Show that we're searching (optional loading state)
        showSearchState(true);

        // Clear existing results before starting a new search
        searchResults.clear();
        String lowerQuery = query.toLowerCase(Locale.getDefault()).trim();

        // Use AtomicInteger to track completed searches
        AtomicInteger totalSearches = new AtomicInteger(0);
        AtomicInteger completedSearches = new AtomicInteger(0);

        // Calculate total number of searches
        for (String mainTopic : MAIN_TOPIC_SUBCOLLECTIONS.keySet()) {
            List<String> subTopics = MAIN_TOPIC_SUBCOLLECTIONS.get(mainTopic);
            if (subTopics != null) {
                totalSearches.addAndGet(subTopics.size());
            }
        }

        Log.d("HomeActivity", "Starting search for: '" + query + "' with total searches: " + totalSearches.get());

        // If no searches to perform, update UI immediately
        if (totalSearches.get() == 0) {
            showSearchState(false);
            updateSearchResultsUI();
            return;
        }

        // Search through each main topic and its subcollections
        for (String mainTopic : MAIN_TOPIC_SUBCOLLECTIONS.keySet()) {
            List<String> subTopics = MAIN_TOPIC_SUBCOLLECTIONS.get(mainTopic);
            if (subTopics != null) {
                for (String subTopic : subTopics) {
                    searchInChapters(mainTopic, subTopic, lowerQuery, () -> {
                        int completed = completedSearches.incrementAndGet();
                        Log.d("HomeActivity", "Completed search " + completed + " of " + totalSearches.get());
                        if (completed >= totalSearches.get()) {
                            showSearchState(false);
                            updateSearchResultsUI();
                        }
                    });
                }
            }
        }
    }

    private void searchInChapters(String mainTopic, String subTopic, String query, Runnable onComplete) {
        // Ensure user is authenticated before accessing Firestore
        if (currentUser == null) {
            Log.e("HomeActivity", "searchInChapters: User not authenticated. Cannot search Firestore.");
            if (onComplete != null) {
                onComplete.run(); // Ensure onComplete is called to prevent hang if authentication fails
            }
            return;
        }

        Log.d("HomeActivity", "Searching in: " + mainTopic + "/" + subTopic + " for: " + query);

        db.collection("codals").document(mainTopic)
                .collection("subtopics") // <--- ADD THIS COLLECTION NAME
                .document(subTopic)      // <--- AND MAKE subTopic A DOCUMENT, NOT A COLLECTION
                .collection("chapters")  // <--- This remains correct as a subcollection of the subTopic document
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d("HomeActivity", "Found " + queryDocumentSnapshots.size() + " documents in " + mainTopic + "/subtopics/" + subTopic + "/chapters");

                    // ... (rest of your existing logic for matching and adding to searchResults)

                    String lowercasedQuery = query.toLowerCase(Locale.ROOT);

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String chapterTitle = document.getId();

                        Log.d("HomeActivity", "Processing document ID: '" + chapterTitle + "' from " + mainTopic + "/subtopics/" + subTopic + "/chapters");

                        boolean titleMatch = false;

                        // Check if chapter title matches search query
                        if (chapterTitle != null && chapterTitle.toLowerCase(Locale.ROOT).contains(lowercasedQuery)) {
                            titleMatch = true;
                            // Corrected path for SearchItem as well
                            String path = "codals/" + mainTopic + "/subtopics/" + subTopic + "/chapters/" + chapterTitle;
                            SearchItem item = new SearchItem(
                                    chapterTitle,
                                    mainTopic,
                                    subTopic,
                                    chapterTitle,
                                    document.getId(),
                                    path
                            );
                            synchronized (searchResults) {
                                if (!searchResults.contains(item)) {
                                    searchResults.add(item);
                                    Log.d("HomeActivity", "Added title match: " + chapterTitle);
                                }
                            }
                        }

                        // Also search within the document's sections content
                        // *** --- CRITICAL CHANGE FOR "sections" FIELD --- ***
                        List<Map<String, Object>> sectionsList = (List<Map<String, Object>>) document.get("sections");

                        if (sectionsList != null) {
                            for (Map<String, Object> sectionMap : sectionsList) {
                                if (sectionMap.containsKey("content")) {
                                    Object contentObject = sectionMap.get("content");
                                    if (contentObject instanceof String) {
                                        String sectionContent = (String) contentObject;
                                        Log.d("HomeActivity", "Processing section content: '" + sectionContent + "'");

                                        if (sectionContent.toLowerCase(Locale.ROOT).contains(lowercasedQuery)) {
                                            String path = "codals/" + mainTopic + "/subtopics/" + subTopic + "/chapters/" + chapterTitle;
                                            SearchItem item = new SearchItem(
                                                    chapterTitle + " (Content match)", // Indicate it's a content match
                                                    mainTopic,
                                                    subTopic,
                                                    chapterTitle,
                                                    document.getId(),
                                                    path
                                            );
                                            synchronized (searchResults) {
                                                if (!searchResults.contains(item)) {
                                                    searchResults.add(item);
                                                    Log.d("HomeActivity", "Added content match for chapter: " + chapterTitle);
                                                }
                                            }
                                            // Once a match is found in content, no need to check other sections of this chapter
                                            break;
                                        }
                                    }
                                }
                            }
                        }

                        // Call searchInAdditionalFields for other fields if needed,
                        // but ensure it also handles non-String types gracefully.
                        // If 'sections' is ALSO passed to this, remove it from the list of fields to check here.
                        // searchInAdditionalFields(document, lowercasedQuery, mainTopic, subTopic, chapterTitle);
                    }

                    if (onComplete != null) {
                        onComplete.run();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("HomeActivity", "Error searching chapters in " + mainTopic + "/" + subTopic, e);
                    // Check if the error is due to permission denied
                    if (e.getMessage() != null && e.getMessage().contains("PERMISSION_DENIED")) {
                        Toast.makeText(HomeActivity.this, "Authentication required to access content.", Toast.LENGTH_LONG).show();
                        redirectToLogin(); // Redirect if permission denied
                    }
                    if (onComplete != null) {
                        onComplete.run();
                    }
                });
    }

    private void searchInAdditionalFields(QueryDocumentSnapshot document, String query,
                                          String mainTopic, String subTopic, String chapterTitle) {
        // Ensure user is authenticated before accessing Firestore
        if (currentUser == null) {
            Log.e("HomeActivity", "searchInAdditionalFields: User not authenticated.");
            return;
        }

        // Search in additional fields like description, keywords, etc.
        Map<String, Object> data = document.getData();
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String fieldName = entry.getKey();
            Object fieldValue = entry.getValue();

            if ("sections".equals(fieldName)) continue;

            if (fieldValue instanceof String) {
                String stringValue = (String) fieldValue;
                if (stringValue.toLowerCase(Locale.ROOT).contains(query)) {
                    String path = "codals/" + mainTopic + "/subtopics/" + subTopic + "/chapters/" + chapterTitle; // <--- Corrected path here too
                    SearchItem item = new SearchItem(
                            chapterTitle + " (" + fieldName + " match)",
                            mainTopic,
                            subTopic,
                            chapterTitle,
                            document.getId(),
                            path
                    );
                    synchronized (searchResults) {
                        if (!searchResults.contains(item)) {
                            searchResults.add(item);
                            Log.d("HomeActivity", "Added " + fieldName + " match: " + chapterTitle);
                        }
                    }
                    break;
                }
            }
        }
    }

    private void updateSearchResultsUI() {
        runOnUiThread(() -> {
            Log.d("HomeActivity", "Updating UI with " + searchResults.size() + " results");

            String currentQuery = home_search_field.getText().toString().trim();

            // Ensure no_results_text is correctly initialized (from onCreate, not here)
            // no_results_text = findViewById(R.id.no_results_text); // REMOVE THIS LINE IF ALREADY IN ONCREATE

            // Always start by hiding all search-related UI components
            searchResultsRecyclerView.setVisibility(View.GONE);
            no_results_text.setVisibility(View.GONE);

            if (currentQuery.isEmpty()) {
                // SCENARIO 1: Search field is EMPTY
                Log.d("HomeActivity", "Search query is empty. Showing main content (ScrollView) and GridLayout.");


                // Explicitly show the GridLayout
                gridLayout.setVisibility(View.VISIBLE);

                // Clear any previous search results from the adapter and list (important when clearing text)
                searchResults.clear();
                searchAdapter.notifyDataSetChanged();

            } else {
                // SCENARIO 2: Search field has a QUERY (not empty)
                Log.d("HomeActivity", "Search query active: '" + currentQuery + "'. Hiding main content and GridLayout.");


                // Explicitly hide the GridLayout
                gridLayout.setVisibility(View.GONE);

                if (!searchResults.isEmpty()) {
                    // SUB-SCENARIO 2a: Results found for the query
                    Log.d("HomeActivity", "Showing search results RecyclerView.");
                    searchResultsRecyclerView.setVisibility(View.VISIBLE);

                    // Sort results
                    searchResults.sort((item1, item2) -> {
                        boolean item1IsContentMatch = item1.getTitle().contains("match)");
                        boolean item2IsContentMatch = item2.getTitle().contains("match)");

                        if (item1IsContentMatch && !item2IsContentMatch) return 1;
                        if (!item1IsContentMatch && item2IsContentMatch) return -1;
                        return item1.getTitle().compareToIgnoreCase(item2.getTitle());
                    });

                    searchAdapter.setSearchQuery(currentQuery);
                    searchAdapter.notifyDataSetChanged();
                    Log.d("HomeActivity", "Search results adapter notified.");

                } else {
                    // SUB-SCENARIO 2b: No results found for the current query
                    Log.d("HomeActivity", "No results found for query '" + currentQuery + "'. Showing no_results_text.");
                    no_results_text.setVisibility(View.VISIBLE); // Show "No results found" message
                }
            }
        });
    }

    private void navigateToContent(SearchItem item) {
        // Ensure user is authenticated before navigating to content
        if (currentUser == null) {
            Log.e("HomeActivity", "navigateToContent: User not authenticated.");
            redirectToLogin();
            return;
        }

        Intent intent = new Intent(HomeActivity.this, ReviewerActivity.class);
        intent.putExtra(ReviewerActivity.EXTRA_SELECTED_MAIN_TOPIC, item.getMainTopic());
        intent.putExtra("SUB_TOPIC", item.getSubTopic());
        intent.putExtra("CHAPTER_TITLE", item.getOriginalChapterTitle());
        intent.putExtra("DOCUMENT_ID", item.getDocumentId());
        intent.putExtra("FIRESTORE_PATH", item.getPath());
        intent.putExtra("SEARCH_QUERY", home_search_field.getText().toString()); // Pass search query if needed

        Log.d("HomeActivity", "Navigating to: " + item.getPath());
        startActivityForResult(intent, SEARCH_REQUEST_CODE);

        Toast.makeText(this, "Opening: " + item.getTitle().replace(" (Content match)", "").replace(" (Content match)", ""), Toast.LENGTH_SHORT).show();
    }

    private void performSearch() {
        // Ensure user is authenticated before performing search
        if (currentUser == null) {
            Log.e("HomeActivity", "performSearch: User not authenticated.");
            redirectToLogin();
            return;
        }

        String query = home_search_field.getText().toString().trim();
        if (query.isEmpty()) {
            Toast.makeText(this, "Please enter a search query", Toast.LENGTH_SHORT).show();
            return;
        }

        // Cancel any pending search
        if (searchRunnable != null) {
            searchHandler.removeCallbacks(searchRunnable);
        }

        searchFirestore(query);
    }

    // Add this method to handle cleanup
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (searchHandler != null && searchRunnable != null) {
            searchHandler.removeCallbacks(searchRunnable);
        }
    }

    // Enhanced onBackPressed to handle search state
    @Override
    public void onBackPressed() {
        if (searchResultsRecyclerView.getVisibility() == View.VISIBLE && !home_search_field.getText().toString().isEmpty()) {
            home_search_field.setText("");
            home_search_field.clearFocus();

            // Hide keyboard
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(home_search_field.getWindowToken(), 0);

        } else if (drawerLayout.isDrawerOpen(navigationView)) {
            drawerLayout.closeDrawer(navigationView);
        } else {
            super.onBackPressed();
        }
    }

    private void setupCardClickListeners() {
        // All card clicks also need authentication check
        // You can add a check inside each listener or wrap the Intent launch.
        // For simplicity and to ensure all content access is authenticated,
        // the `checkAuthentication()` method will handle the main redirect if needed.
        // However, if the user was authenticated on onCreate, these should proceed.

        cardViewCivil.setOnClickListener(v -> {
            if (currentUser == null) { redirectToLogin(); return; }
            Intent intent = new Intent(HomeActivity.this, ReviewerActivity.class);
            intent.putExtra(ReviewerActivity.EXTRA_SELECTED_MAIN_TOPIC, "Civil Law");
            Log.d("HomeActivity", "Starting ReviewerActivity for: Civil Law");
            startActivity(intent);
        });

        cardViewCommercial.setOnClickListener(v -> {
            if (currentUser == null) { redirectToLogin(); return; }
            Intent intent = new Intent(HomeActivity.this, ReviewerActivity.class);
            intent.putExtra(ReviewerActivity.EXTRA_SELECTED_MAIN_TOPIC, "Commercial Law");
            Log.d("HomeActivity", "Starting ReviewerActivity for: Commercial Law");
            startActivity(intent);
        });

        cardViewRem.setOnClickListener(v -> {
            if (currentUser == null) { redirectToLogin(); return; }
            Intent intent = new Intent(HomeActivity.this, ReviewerActivity.class);
            intent.putExtra(ReviewerActivity.EXTRA_SELECTED_MAIN_TOPIC, "Remedial Law");
            Log.d("HomeActivity", "Starting ReviewerActivity for: Remedial Law");
            startActivity(intent);
        });

        cardViewCriminal.setOnClickListener(v -> {
            if (currentUser == null) { redirectToLogin(); return; }
            Intent intent = new Intent(HomeActivity.this, ReviewerActivity.class);
            intent.putExtra(ReviewerActivity.EXTRA_SELECTED_MAIN_TOPIC, "Criminal Law");
            Log.d("HomeActivity", "Starting ReviewerActivity for: Criminal Law");
            startActivity(intent);
        });

        cardViewConstitutional.setOnClickListener(v -> {
            if (currentUser == null) { redirectToLogin(); return; }
            Intent intent = new Intent(HomeActivity.this, ReviewerActivity.class);
            intent.putExtra(ReviewerActivity.EXTRA_SELECTED_MAIN_TOPIC, "Constitutional Law");
            Log.d("HomeActivity", "Starting ReviewerActivity for: Constitutional Law");
            startActivity(intent);
        });

        cardViewTaxation.setOnClickListener(v -> {
            if (currentUser == null) { redirectToLogin(); return; }
            Intent intent = new Intent(HomeActivity.this, ReviewerActivity.class);
            intent.putExtra(ReviewerActivity.EXTRA_SELECTED_MAIN_TOPIC, "Taxation Law");
            Log.d("HomeActivity", "Starting ReviewerActivity for: Taxation Law");
            startActivity(intent);
        });
    }

    private void setupUserData() {
        // In onCreate, we now ensure currentUser is not null.
        // So, currentUserId will always be available here.
        Log.d("HomeActivity", "setupUserData() - currentUserId: " + currentUserId);

        // Check if firstName is passed from intent (e.g., from a new user registration)
        String firstNameFromIntent = getIntent().getStringExtra("FIRST_NAME");
        isNewUser = getIntent().getBooleanExtra("IS_NEW_USER", false);

        if (isNewUser) {
            showGetUserNameDialog();
        } else if (firstNameFromIntent != null && !firstNameFromIntent.isEmpty()) {
            currentFirstName = firstNameFromIntent;
            welcomeTextView.setText("Hello, " + currentFirstName);
            if (nav_username_display != null) {
                nav_username_display.setText(currentFirstName);
            }
        } else {
            // If not a new user and no first name from intent, fetch from Firestore
            fetchUserData(currentUserId);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SEARCH_REQUEST_CODE && resultCode == RESULT_OK) {
            home_search_field.setText("");
            searchResultsRecyclerView.setVisibility(View.GONE);
            contentLayout.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("FIRST_NAME", currentFirstName);
        outState.putString("USER_ID", currentUserId);
        outState.putBoolean("IS_NEW_USER", isNewUser);
        Log.d("HomeActivity", "onSaveInstanceState() - Saving currentFirstName: " + currentFirstName + ", currentUserId: " + currentUserId + ", isNewUser: " + isNewUser);
    }

    @Override
    protected void onResume() {
        super.onResume();
        currentUser = mAuth.getCurrentUser(); // Re-check user status on resume
        if (currentUser == null) {
            Log.d("HomeActivity", "onResume() - User not authenticated. Redirecting.");
            redirectToLogin();
            return;
        }
        currentUserId = currentUser.getUid(); // Update currentUserId in case it changed

        Log.d("HomeActivity", "onResume() - userId: " + currentUserId + ", currentFirstName: " + currentFirstName + ", welcomeText: " + welcomeTextView.getText().toString());

        // Only fetch user data if currentFirstName is not set or if there was an error/default welcome text
        if (currentUserId != null && (currentFirstName == null || currentFirstName.isEmpty() || welcomeTextView.getText().toString().startsWith("Welcome") || welcomeTextView.getText().toString().contains("Error"))) {
            Log.d("HomeActivity", "onResume() - Calling fetchUserData() because name is missing or generic.");
            fetchUserData(currentUserId);
        }
        Log.d("HomeActivity", "onResume() - Finished");
        home_search_field.setText("");
        searchResultsRecyclerView.setVisibility(View.GONE);
        contentLayout.setVisibility(View.VISIBLE);
    }

    private void showGetUserNameDialog() {
        GetUserNameDialogFragment dialog = new GetUserNameDialogFragment();
        dialog.show(getSupportFragmentManager(), "get_user_name_dialog");
        Log.d("HomeActivity", "showGetUserNameDialog() - Shown");
    }

    @Override
    public void onNameEntered(String name) {
        if (!name.isEmpty()) {
            currentFirstName = name;
            welcomeTextView.setText("Hello, " + currentFirstName);
            if (nav_username_display != null) {
                nav_username_display.setText(currentFirstName);
            }
            currentUser = mAuth.getCurrentUser(); // Ensure currentUser is up-to-date
            if (currentUser != null) {
                updateUserNameInFirestore(currentUser.getUid(), name);
                currentUserId = currentUser.getUid(); // Ensure currentUserId is consistent
                Log.d("HomeActivity", "onNameEntered() - Name entered: " + name + ", updated UI and Firestore, currentUserId: " + currentUserId);
            } else {
                Log.e("HomeActivity", "onNameEntered: User is null after name entry, cannot update Firestore.");
                redirectToLogin();
            }
        } else {
            welcomeTextView.setText("Hello!");
            Log.d("HomeActivity", "onNameEntered() - Empty name entered");
        }
    }

    private void updateUserNameInFirestore(String userId, String name) {
        // Ensure user is authenticated for this write operation
        if (currentUser == null || !currentUser.getUid().equals(userId)) {
            Log.e("HomeActivity", "updateUserNameInFirestore: User not authenticated or mismatched UID. Cannot update.");
            redirectToLogin();
            return;
        }

        DocumentReference userRef = db.collection("users").document(userId);
        userRef.update("name", name)
                .addOnSuccessListener(aVoid -> Log.d("HomeActivity", "updateUserNameInFirestore() - Success"))
                .addOnFailureListener(e -> {
                    Log.e("HomeActivity", "updateUserNameInFirestore() - Error: " + e.getMessage(), e);
                    // Handle specific errors like PERMISSION_DENIED
                    if (e.getMessage() != null && e.getMessage().contains("PERMISSION_DENIED")) {
                        Toast.makeText(HomeActivity.this, "Permission denied to update user data. Please re-authenticate.", Toast.LENGTH_LONG).show();
                        redirectToLogin();
                    }
                });
    }

    private void showLogoutConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Log Out")
                .setMessage("Are you sure you want to log out from your account?")
                .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                    mAuth.signOut();
                    Log.d("HomeActivity", "showLogoutConfirmationDialog() - Logged out");
                    redirectToLogin(); // Redirect to login after logout
                })
                .setNegativeButton(android.R.string.no, null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
        Log.d("HomeActivity", "showLogoutConfirmationDialog() - Shown");
    }

    private void fetchUserData(String userId) {
        // Ensure user is authenticated for this read operation
        if (currentUser == null || !currentUser.getUid().equals(userId)) {
            Log.e("HomeActivity", "fetchUserData: User not authenticated or mismatched UID. Cannot fetch.");
            redirectToLogin();
            return;
        }

        DocumentReference userRef = db.collection("users").document(userId);
        Log.d("HomeActivity", "fetchUserData() - Fetching for userId: " + userId);
        userRef.get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            String name = document.getString("name");
                            currentFirstName = name;
                            welcomeTextView.setText("Hello, " + name);
                            Log.d("HomeActivity", "fetchUserData() - Success, name: " + name);
                            if (nav_username_display != null) {
                                nav_username_display.setText(currentFirstName);
                            }
                        } else {
                            welcomeTextView.setText("Welcome!"); // Default if no name found
                            Log.w("HomeActivity", "fetchUserData() - No user data found in Firestore for this UID: " + userId + ". Showing GetUserNameDialog.");
                            // If user document doesn't exist for an authenticated user, prompt for name
                            showGetUserNameDialog();
                        }
                    } else {
                        // Handle failure, especially permission denied errors
                        Log.e("HomeActivity", "fetchUserData() - Error getting document: " + task.getException().getMessage(), task.getException());
                        welcomeTextView.setText("Welcome!"); // Default in case of error
                        if (task.getException() != null && task.getException().getMessage() != null && task.getException().getMessage().contains("PERMISSION_DENIED")) {
                            Toast.makeText(HomeActivity.this, "Permission denied to read user data. Please re-authenticate.", Toast.LENGTH_LONG).show();
                            redirectToLogin(); // Redirect if permission denied
                        }
                    }
                });
    }

    // New helper method to redirect to LoginActivity
    private void redirectToLogin() {
        Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish(); // Finish HomeActivity so user can't go back
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        Log.d(TAG, "onNavigationItemSelected: Item selected with ID: " + getResources().getResourceEntryName(id));
        Intent navIntent = null; // Declare navIntent here

        if (id == R.id.nav_home) {

        } else if (id == R.id.nav_gallery) { // Assuming nav_gallery is your calendar item
            // IMPORTANT: Only uncomment this if CalendarActivity.java exists and is declared in AndroidManifest.xml
            // If you still have this item in your menu XML, this block WILL be entered.
            // If CalendarActivity doesn't exist, uncommenting the line below WILL cause ActivityNotFoundException.
            Toast.makeText(this, "Calendar selected", Toast.LENGTH_SHORT).show();
            Log.w(TAG, "onNavigationItemSelected: Calendar item selected, but CalendarActivity is not implemented.");
             navIntent = new Intent(HomeActivity.this, CalendarActivity.class); // Keep commented for now
             if (currentUserId != null && currentFirstName != null) {
                 navIntent.putExtra("USER_ID", currentUserId);
                 navIntent.putExtra("USER_NAME", currentFirstName);
             }
        } else if (id == R.id.nav_slideshow) { // Assuming nav_slideshow is your forum item
            Toast.makeText(this, "Forum Selected", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "onNavigationItemSelected: Forum item selected, already in ForumActivity. No navigation.");
            Toast.makeText(this, "Navigating to Home.", Toast.LENGTH_SHORT).show();
            navIntent = new Intent(HomeActivity.this, ForumActivity.class);
            if (currentUserId != null && currentFirstName != null) { // Always pass user data if possible
                navIntent.putExtra("USER_ID", currentUserId);
                navIntent.putExtra("USER_NAME", currentFirstName);
                Log.d(TAG, "onNavigationItemSelected: Passing USER_ID: " + currentUserId + ", USER_NAME: " + currentFirstName + " to HomeActivity.");
            } else {
                Log.w(TAG, "onNavigationItemSelected: User ID or Name is null, not passing to HomeActivity.");
            }
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
}