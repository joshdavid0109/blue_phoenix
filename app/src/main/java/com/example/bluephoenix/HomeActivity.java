package com.example.bluephoenix;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class HomeActivity extends AppCompatActivity {
    private TextView welcomeTextView;
    private FirebaseFirestore db; // Use FirebaseFirestore
    private CardView cardViewRem;

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

        welcomeTextView = findViewById(R.id.user_display_name);
        cardViewRem = findViewById(R.id.home_rem);

        String userId = getIntent().getStringExtra("USER_ID");

        db = FirebaseFirestore.getInstance(); // Initialize Firestore

        if (userId != null) {
            cardViewRem.setOnClickListener(v -> {
                Intent intent = new Intent(HomeActivity.this, ReviewerActivity.class);
                startActivity(intent);
                finish();
            });

            // Now use Firestore to fetch the user's data
            fetchUserData(userId);
        } else {
            // Handle the case where no user ID was passed
            welcomeTextView.setText("Error: Could not retrieve user information.");
        }
    }

    private void fetchUserData(String userId) {
        DocumentReference userRef = db.collection("users").document(userId);

        userRef.get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            String email = document.getString("email");
                            String name = document.getString("name");
                            String role = document.getString("role");

                            welcomeTextView.setText("Hello, " + name);
                            Log.d("HomeActivity", "User Data (Firestore): Email - " + email + ", Name - " + name + ", Role - " + role);
                            // You can now use this data to populate your UI
                        } else {
                            welcomeTextView.setText("Welcome!"); // Or handle the case where user data is missing
                            Log.w("HomeActivity", "No user data found in Firestore for this UID.");
                        }
                    } else {
                        welcomeTextView.setText("Welcome!"); // Or handle the error
                        Log.e("HomeActivity", "Error fetching user data from Firestore: " + task.getException());
                    }
                });
    }
}