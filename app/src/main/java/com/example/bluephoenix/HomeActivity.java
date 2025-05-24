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
import androidx.cardview.widget.CardView;
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
    private CardView cardViewRem;
    private TextInputLayout logOutBtn;
    private FirebaseAuth mAuth;
    private boolean isNewUser = false;
    private String currentFirstName; // To store the fetched/entered name
    private String currentUserId; // Store the userId

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ImageView menuIcon;

    @SuppressLint("SetTextI18n")
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

        menuIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (drawerLayout.isDrawerOpen(navigationView)) {
                    drawerLayout.closeDrawer(navigationView);
                } else {
                    drawerLayout.openDrawer(navigationView);
                }
            }
        });

        welcomeTextView = findViewById(R.id.user_display_name);
        cardViewRem = findViewById(R.id.home_rem);
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
            } else {
                fetchUserData(currentUserId);
            }
        } else {
            welcomeTextView.setText("Error: Could not retrieve user information.");
        }

        cardViewRem.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, ReviewerActivity.class);
            Log.d("HomeActivity", "Starting ReviewerActivity");
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
                            // ... other data handling ...
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
        Intent intent = null; // Declare intent here

        if (id == R.id.nav_home) {
            // Already in HomeActivity, no navigation needed
            Toast.makeText(this, "You are already on Home screen.", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_gallery) { // Calendar
            // intent = new Intent(HomeActivity.this, CalendarActivity.class);
        } else if (id == R.id.nav_slideshow) { // Peer Review and Discussion (Forum)
            intent = new Intent(HomeActivity.this, ForumActivity.class);
            if (currentUserId != null) {
                intent.putExtra("USER_ID", currentUserId);
                Log.d("HomeActivity", "Passing USER_ID to ForumActivity: " + currentUserId);
            }
            if (currentFirstName != null) {
                intent.putExtra("USER_NAME", currentFirstName);
                Log.d("HomeActivity", "Passing USER_NAME to ForumActivity: " + currentFirstName);
            }
            // ------------------------------------
        } else if (id == R.id.nav_share) { // About Us
            // intent = new Intent(HomeActivity.this, AboutUsActivity.class);
        } else if (id == R.id.nav_send) { // FAQs
            // intent = new Intent(HomeActivity.this, FAQsActivity.class);
        }

        if (intent != null) {
            startActivity(intent);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish();
        }

        drawerLayout.closeDrawer(navigationView); // Use navigationView directly as it's initialized
        return true;
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(navigationView)) { // Use navigationView directly
            drawerLayout.closeDrawer(navigationView);
        } else {
            super.onBackPressed();
        }
    }
}