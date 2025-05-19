package com.example.bluephoenix;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private Button logInBtn;

    private Button registerBtn;
    private EditText emailEditText;
    private EditText passwordEditText;
    private DatabaseReference usersRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        auth = FirebaseAuth.getInstance();
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        usersRef = database.getReference("users");

        logInBtn = findViewById(R.id.loginUserBtn);
        registerBtn = findViewById(R.id.registerBtn);
        emailEditText = findViewById(R.id.email_field);
        passwordEditText = findViewById(R.id.password_field);

        logInBtn.setOnClickListener(v -> {
            String email = emailEditText.getText().toString();
            String password = passwordEditText.getText().toString();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(LoginActivity.this, "Please enter email and password.", Toast.LENGTH_SHORT).show();
                return;
            }

            auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(LoginActivity.this, task -> {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "signInWithEmail:success");
                            System.out.println("sign in succesful");
                            FirebaseUser firebaseAuthUser = auth.getCurrentUser();
                            if (firebaseAuthUser != null) {
                                // Pass the UID to the next activity
                                Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                                intent.putExtra("USER_ID", firebaseAuthUser.getUid());
                                startActivity(intent);
                                finish();
                            } else {
                                updateUI(null);
                            }
                        } else {
                            Log.w(TAG, "signInWithEmail:failure", task.getException());
                            Toast.makeText(LoginActivity.this, "Log in failed.",
                                    Toast.LENGTH_SHORT).show();
                            updateUI(null);
                        }
                    });
        });

        registerBtn.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish();
        });



        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and navigate only if they ARE already logged in
//        FirebaseUser currentUser = auth.getCurrentUser();
//        if (currentUser != null) {
//            // User is already signed in, proceed directly to HomeActivity
//            Intent intent = new Intent(this, HomeActivity.class);
//            intent.putExtra("USER_ID", currentUser.getUid());
//            startActivity(intent);
//            finish(); // Close LoginActivity so the user can't go back without logging out
//        } else {
//            // User is not signed in, the login screen will be displayed
//            Log.d(TAG, "User not signed in. Displaying login screen.");
//            // No need to do anything here, the layout for LoginActivity is already set in onCreate()
//        }
    }

    private void updateUI(FirebaseUser user) {
        if (user == null) {
            Toast.makeText(this, "Not signed in.", Toast.LENGTH_SHORT).show();
        }
    }
}