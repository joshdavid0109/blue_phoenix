package com.example.bluephoenix;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.example.bluephoenix.fragments.LoadingDialogFragment;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private EditText emailEditText;
    private EditText passwordEditText;
    private EditText confirmPasswordEditText;
    private Button registerButton;
    private Button loginButton;
    private ImageView googleSignInButton;
    private GoogleSignInClient mGoogleSignInClient;
    private static final int RC_SIGN_IN = 9002; // Different request code for Register
    private FragmentManager fm = getSupportFragmentManager();
    private LoadingDialogFragment loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        emailEditText = findViewById(R.id.editTextTextEmailAddress);
        passwordEditText = findViewById(R.id.password_field);
        confirmPasswordEditText = findViewById(R.id.confirm_pass);
        registerButton = findViewById(R.id.reg_Btn); // Make sure this ID matches the Register button
        loginButton = findViewById(R.id.loginBtn); // Make sure this ID matches the Login button on the Register screen
        googleSignInButton = findViewById(R.id.gmail_ic); // Assuming you have a Google sign-in button

        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id)) // Replace with your Web client ID
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        registerButton.setOnClickListener(v -> handleEmailPasswordRegistration());
        loginButton.setOnClickListener(v -> navigateToLogin());
        googleSignInButton.setOnClickListener(v -> signInWithGoogle());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void showLoadingDialog(String message) {
        loadingDialog = LoadingDialogFragment.newInstance(message);
        loadingDialog.show(fm, "loading_dialog");
    }

    private void hideLoadingDialog() {
        if (loadingDialog != null) {
            loadingDialog.dismissAllowingStateLoss();
            loadingDialog = null;
        }
    }

    private void handleEmailPasswordRegistration() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String confirmPassword = confirmPasswordEditText.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(RegisterActivity.this, "Please fill in all fields.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(RegisterActivity.this, "Passwords do not match.", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoadingDialog("Registering...");
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(RegisterActivity.this, task -> {
                    hideLoadingDialog();
                    if (task.isSuccessful()) {
                        Log.d(TAG, "createUserWithEmail:success");
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            createUserDocument(user, email, ""); // Name is empty for email/password registration
                        }
                    } else {
                        Log.w(TAG, "createUserWithEmail:failure", task.getException());
                        Toast.makeText(RegisterActivity.this, "Registration failed: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void signInWithGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            com.google.android.gms.tasks.Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                showLoadingDialog("Registering with Google...");
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                hideLoadingDialog();
                Log.w(TAG, "Google sign in failed", e);
                Toast.makeText(this, "Google sign in failed.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    showLoadingDialog("Registering...");
                    if (task.isSuccessful()) {
                        Log.d(TAG, "firebaseAuthWithGoogle:success");
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            createUserDocument(user, user.getEmail(), user.getDisplayName());
                        }
                    } else {
                        hideLoadingDialog();
                        Log.w(TAG, "firebaseAuthWithGoogle:failure", task.getException());
                        Toast.makeText(this, "Firebase authentication with Google failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void createUserDocument(FirebaseUser user, String email, String displayName) {
        if (user != null) {
            db.collection("users").document(user.getUid())
                    .get()
                    .addOnCompleteListener(documentTask -> {
                        if (documentTask.isSuccessful()) {
                            DocumentSnapshot document = documentTask.getResult();
                            if (!document.exists()) {
                                Map<String, Object> userData = new HashMap<>();
                                userData.put("email", email);
                                userData.put("name", displayName != null ? displayName : "");
                                userData.put("role", "user");
                                userData.put("providerId", user.getProviderData().get(0).getProviderId()); // Get auth provider
                                userData.put("creationTime", FieldValue.serverTimestamp());

                                db.collection("users").document(user.getUid())
                                        .set(userData)
                                        .addOnSuccessListener(aVoid -> {
                                            hideLoadingDialog();
                                            Toast.makeText(RegisterActivity.this, "Registration successful.", Toast.LENGTH_SHORT).show();
                                            navigateToHome(user.getUid(), displayName, true); // Indicate new user
                                        })
                                        .addOnFailureListener(e -> {
                                            hideLoadingDialog();
                                            Log.w(TAG, "Error creating Firestore user document", e);
                                            Toast.makeText(RegisterActivity.this, "Registration successful, but failed to save user data.", Toast.LENGTH_SHORT).show();
                                            navigateToHome(user.getUid(), displayName, true); // Still navigate
                                        });
                            } else {
                                hideLoadingDialog();
                                Toast.makeText(RegisterActivity.this, "User already registered.", Toast.LENGTH_SHORT).show();
                                navigateToHome(user.getUid(), displayName, false); // Indicate not a new user
                            }
                        } else {
                            hideLoadingDialog();
                            Log.w(TAG, "Error checking user document", documentTask.getException());
                            Toast.makeText(RegisterActivity.this, "Error checking user data.", Toast.LENGTH_SHORT).show();
                            navigateToHome(user.getUid(), displayName, false); // Still navigate
                        }
                    });
        }
    }

    private void navigateToHome(String userId, String displayName, boolean isNewUser) {
        Intent intent = new Intent(RegisterActivity.this, HomeActivity.class);
        intent.putExtra("USER_ID", userId);
        intent.putExtra("FIRST_NAME", displayName != null && !displayName.isEmpty() ? displayName.split(" ")[0] : "");
        intent.putExtra("IS_NEW_USER", isNewUser); // Pass the flag
        startActivity(intent);
        finish();
    }

    private void navigateToLogin() {
        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        finish();
    }
}