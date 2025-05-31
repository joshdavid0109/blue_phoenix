package com.example.bluephoenix;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView; // Make sure this is imported
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.DialogFragment;

import com.example.bluephoenix.fragments.GetUserNameDialogFragment;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class HomeActivity extends AppCompatActivity implements GetUserNameDialogFragment.NameInputDialogListener, NavigationView.OnNavigationItemSelectedListener {
    private TextView welcomeTextView;
    private FirebaseFirestore db;
    // Declare all your CardViews for main topics here
    private CardView cardViewCivil; // Assuming you have one for Civil Law
    private CardView cardViewCommercial; // Assuming you have one for Commercial Law
    private CardView cardViewRem; // This is the one you already have

    private CardView cardViewConstitutional;

    private CardView cardViewCriminal;

    private CardView cardViewTaxation;

    private TextInputLayout logOutBtn;
    private FirebaseAuth mAuth;
    private boolean isNewUser = false;
    private String currentFirstName; // To store the fetched/entered name
    private String currentUserId; // Store the userId

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ImageView menuIcon;

    private TextView nav_username_display;

    @SuppressLint({"SetTextI18d", "MissingInflatedId"}) // Added @SuppressLint("MissingInflatedId") just in case, but it's good practice to ensure all IDs are found.
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

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        menuIcon = findViewById(R.id.menu_icon);

        navigationView.setNavigationItemSelectedListener(this);
        View headerView = navigationView.getHeaderView(0);
        nav_username_display = headerView.findViewById(R.id.nav_header_title);

        menuIcon.setOnClickListener(v -> {
            if (drawerLayout.isDrawerOpen(navigationView)) {
                drawerLayout.closeDrawer(navigationView);
            } else {
                drawerLayout.openDrawer(navigationView);
            }
        });

        welcomeTextView = findViewById(R.id.user_display_name);

        // Initialize all your CardViews for main topics
        cardViewCivil = findViewById(R.id.home_civ); // Assuming R.id.home_civil is the ID for Civil Law CardView
        cardViewCommercial = findViewById(R.id.home_comm); // Assuming R.id.home_commercial
        cardViewRem = findViewById(R.id.home_rem); // Your existing Remedial Law CardView
        cardViewCriminal = findViewById(R.id.home_crim);
        cardViewConstitutional = findViewById(R.id.home_consti);
        cardViewTaxation = findViewById(R.id.home_tax);

        logOutBtn = findViewById(R.id.logOut_ic);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        String userIdFromIntent = getIntent().getStringExtra("USER_ID");
        String firstNameFromIntent = getIntent().getStringExtra("FIRST_NAME");
        isNewUser = getIntent().getBooleanExtra("IS_NEW_USER", false);

        if (savedInstanceState != null) {
            currentFirstName = savedInstanceState.getString("FIRST_NAME");
            currentUserId = savedInstanceState.getString("USER_ID");
            isNewUser = savedInstanceState.getBoolean("IS_NEW_USER", false);
            Log.d("HomeActivity", "onCreate() - Restored currentFirstName: " + currentFirstName + ", currentUserId: " + currentUserId);
            if (currentFirstName != null && !currentFirstName.startsWith("Hello")) {
                welcomeTextView.setText("Hello, " + currentFirstName);
                if (nav_username_display != null) {
                    nav_username_display.setText(currentFirstName);
                }
            } else if (currentFirstName != null) {
                welcomeTextView.setText(currentFirstName);
            } else if (currentUserId != null) {
                fetchUserData(currentUserId);
            } else {
                welcomeTextView.setText("Error: Could not retrieve user information.");
            }
        } else if (userIdFromIntent != null) {
            currentUserId = userIdFromIntent;
            Log.d("HomeActivity", "onCreate() - userId from Intent: " + currentUserId + ", firstName: " + firstNameFromIntent + ", isNewUser: " + isNewUser);
            if (isNewUser) {
                showGetUserNameDialog();
            } else if (firstNameFromIntent != null && !firstNameFromIntent.isEmpty()) {
                currentFirstName = firstNameFromIntent;
                welcomeTextView.setText("Hello, " + currentFirstName);
                if (nav_username_display != null) {
                    nav_username_display.setText(currentFirstName);
                }
            } else {
                fetchUserData(currentUserId);
            }
        } else {
            welcomeTextView.setText("Error: Could not retrieve user information.");
        }

        // Set OnClickListener for Civil Law CardView
        cardViewCivil.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, ReviewerActivity.class);
            // Pass the specific main topic name "Civil Law" as an extra
            intent.putExtra(ReviewerActivity.EXTRA_SELECTED_MAIN_TOPIC, "Civil Law");
            Log.d("HomeActivity", "Starting ReviewerActivity for: Civil Law");
            startActivity(intent);
        });

        // Set OnClickListener for Commercial Law CardView
        cardViewCommercial.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, ReviewerActivity.class);
            // Pass the specific main topic name "Commercial Law" as an extra
            intent.putExtra(ReviewerActivity.EXTRA_SELECTED_MAIN_TOPIC, "Commercial Law");
            Log.d("HomeActivity", "Starting ReviewerActivity for: Commercial Law");
            startActivity(intent);
        });

        // Your existing Remedial Law CardView click listener, now with extra
        cardViewRem.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, ReviewerActivity.class);
            // Pass the specific main topic name "Remedial Law" as an extra
            intent.putExtra(ReviewerActivity.EXTRA_SELECTED_MAIN_TOPIC, "Remedial Law");
            Log.d("HomeActivity", "Starting ReviewerActivity for: Remedial Law");
            startActivity(intent);
        });

         cardViewCriminal.setOnClickListener(v -> {
             Intent intent = new Intent(HomeActivity.this, ReviewerActivity.class);
             intent.putExtra(ReviewerActivity.EXTRA_SELECTED_MAIN_TOPIC, "Criminal Law");
             Log.d("HomeActivity", "Starting ReviewerActivity for: Criminal Law");
             startActivity(intent);
         });

         cardViewConstitutional.setOnClickListener(v -> {
             Intent intent = new Intent(HomeActivity.this, ReviewerActivity.class);
             intent.putExtra(ReviewerActivity.EXTRA_SELECTED_MAIN_TOPIC, "Constitutional Law");
             Log.d("HomeActivity", "Starting ReviewerActivity for: Constitutional Law");
             startActivity(intent);
         });

         cardViewTaxation.setOnClickListener(v -> {
             Intent intent = new Intent(HomeActivity.this, ReviewerActivity.class);
             intent.putExtra(ReviewerActivity.EXTRA_SELECTED_MAIN_TOPIC, "Taxation Law");
             Log.d("HomeActivity", "Starting ReviewerActivity for: Taxation Law");
             startActivity(intent);
         });

        logOutBtn.setOnClickListener(v -> showLogoutConfirmationDialog());
        Log.d("HomeActivity", "onCreate() - Finished");
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
        Log.d("HomeActivity", "onResume() - userId: " + currentUserId + ", currentFirstName: " + currentFirstName + ", welcomeText: " + welcomeTextView.getText().toString());

        if (currentUserId != null && (currentFirstName == null || currentFirstName.isEmpty() || welcomeTextView.getText().toString().startsWith("Welcome") || welcomeTextView.getText().toString().contains("Error"))) {
            Log.d("HomeActivity", "onResume() - Calling fetchUserData()");
            fetchUserData(currentUserId);
        }
        Log.d("HomeActivity", "onResume() - Finished");
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
            FirebaseUser user = mAuth.getCurrentUser();
            if (user != null) {
                updateUserNameInFirestore(user.getUid(), name);
                currentUserId = user.getUid();
                Log.d("HomeActivity", "onNameEntered() - Name entered: " + name + ", updated UI and Firestore, currentUserId: " + currentUserId);
            }
        } else {
            welcomeTextView.setText("Hello!");
            Log.d("HomeActivity", "onNameEntered() - Empty name entered");
        }
    }

    private void updateUserNameInFirestore(String userId, String name) {
        DocumentReference userRef = db.collection("users").document(userId);
        userRef.update("name", name)
                .addOnSuccessListener(aVoid -> Log.d("HomeActivity", "updateUserNameInFirestore() - Success"))
                .addOnFailureListener(e -> Log.e("HomeActivity", "updateUserNameInFirestore() - Error: " + e));
    }

    private void showLogoutConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Log Out")
                .setMessage("Are you sure you want to log out from your account?")
                .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                    mAuth.signOut();
                    Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
                    startActivity(intent);
                    finish();
                    Log.d("HomeActivity", "showLogoutConfirmationDialog() - Logged out");
                })
                .setNegativeButton(android.R.string.no, null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
        Log.d("HomeActivity", "showLogoutConfirmationDialog() - Shown");
    }

    private void fetchUserData(String userId) {
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
                            welcomeTextView.setText("Welcome!");
                            Log.w("HomeActivity", "fetchUserData() - No user data found in Firestore for this UID.");
                        }
                    } else {
                        welcomeTextView.setText("Welcome!");
                        Log.e("HomeActivity", "fetchUserData() - Error: " + task.getException());
                    }
                });
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        Intent intent = null;

        if (id == R.id.nav_home) {
            Toast.makeText(this, "You are already on Home screen.", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_gallery) {
            // intent = new Intent(HomeActivity.this, CalendarActivity.class);
        } else if (id == R.id.nav_slideshow) {
            intent = new Intent(HomeActivity.this, ForumActivity.class);
            if (currentUserId != null) {
                intent.putExtra("USER_ID", currentUserId);
                Log.d("HomeActivity", "Passing USER_ID to ForumActivity: " + currentUserId);
            }
            if (currentFirstName != null) {
                intent.putExtra("USER_NAME", currentFirstName);
                Log.d("HomeActivity", "Passing USER_NAME to ForumActivity: " + currentFirstName);
            }
        } else if (id == R.id.nav_share) {
            // intent = new Intent(HomeActivity.this, AboutUsActivity.class);
        } else if (id == R.id.nav_send) {
            // intent = new Intent(HomeActivity.this, FAQsActivity.class);
        } else if (id == R.id.nav_logout) {
            intent = new Intent(this, MainActivity.class);
        }

        if (intent != null) {
            startActivity(intent);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish();
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
        }
    }
}