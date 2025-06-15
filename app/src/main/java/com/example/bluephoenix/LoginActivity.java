package com.example.bluephoenix;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private Button logInBtn;
    private Button registerBtn;
    private EditText emailEditText;
    private EditText passwordEditText;
    private DatabaseReference usersRef;
    private static final int RC_SIGN_IN = 9001;
    private GoogleSignInClient mGoogleSignInClient;
    private ImageView googleSignInButton; // Assuming you have an ImageView for Google Sign-in
    private FirebaseFirestore db;
    private FragmentManager fm = getSupportFragmentManager();
    private LoadingDialogFragment loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        auth = FirebaseAuth.getInstance();
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        db = FirebaseFirestore.getInstance();
        usersRef = database.getReference("users");

        logInBtn = findViewById(R.id.loginUserBtn);
        registerBtn = findViewById(R.id.registerBtn);
        emailEditText = findViewById(R.id.email_field);
        passwordEditText = findViewById(R.id.password_field);
        googleSignInButton = findViewById(R.id.gmail_ic);

        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id)) // Replace with your Web client ID from Firebase Console
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        logInBtn.setOnClickListener(v -> handleEmailPasswordLogin());
        registerBtn.setOnClickListener(v -> navigateToRegister());
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

    private void handleEmailPasswordLogin() {
        String email = emailEditText.getText().toString();
        String password = passwordEditText.getText().toString();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(LoginActivity.this, "Please enter email and password.", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoadingDialog("Logging in...");
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(LoginActivity.this, task -> {
                    hideLoadingDialog();
                    if (task.isSuccessful()) {
                        Log.d(TAG, "signInWithEmail:success");
                        FirebaseUser firebaseAuthUser = auth.getCurrentUser();
                        if (firebaseAuthUser != null) {
                            navigateToHome(firebaseAuthUser);
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
    }

    private void navigateToRegister() {
        Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        finish();
    }

    private void signInWithGoogle() {
        showLoadingDialog("Signing in with Google...");
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
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
                    if (task.isSuccessful()) {
                        Log.d(TAG, "firebaseAuthWithGoogle:success");
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            // Check if the user's document exists in Firestore
                            db.collection("users").document(user.getUid())
                                    .get()
                                    .addOnCompleteListener(documentTask -> {
                                        hideLoadingDialog();
                                        if (documentTask.isSuccessful()) {
                                            DocumentSnapshot document = documentTask.getResult();
                                            if (document.exists()) {
                                                navigateToHome(user);
                                            } else {
                                                // This case shouldn't ideally happen for Google sign-in after registration
                                                Toast.makeText(LoginActivity.this, "User data not found. Please register.", Toast.LENGTH_SHORT).show();
                                                // Optionally, redirect to RegisterActivity
                                            }
                                        } else {
                                            hideLoadingDialog();
                                            Log.w(TAG, "Error checking Firestore user document", documentTask.getException());
                                            Toast.makeText(LoginActivity.this, "Error checking user data.", Toast.LENGTH_SHORT).show();
                                            updateUI(null); // Optionally handle failure
                                        }
                                    });
                        } else {
                            hideLoadingDialog();
                            updateUI(null);
                        }
                    } else {
                        hideLoadingDialog();
                        Log.w(TAG, "firebaseAuthWithGoogle:failure", task.getException());
                        Toast.makeText(this, "Firebase authentication with Google failed.", Toast.LENGTH_SHORT).show();
                        updateUI(null);
                    }
                });
    }

    private void updateUI(FirebaseUser user) {
        // This method might not be strictly necessary for navigation in this flow
        // as navigation is handled directly in the auth success listeners.
    }

    private void navigateToHome(FirebaseUser user) {
        String firstName = "";
        boolean isNewUser = false; // Existing user, so not a new registration

        if (user.getDisplayName() != null && !user.getDisplayName().isEmpty()) {
            String[] nameParts = user.getDisplayName().split(" ");
            if (nameParts.length > 0) {
                firstName = nameParts[0];
            }
        } else {
            // If the user logged in with email/password, get the name from Firestore
            db.collection("users").document(user.getUid())
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            String firestoreFirstName = "";
                            String firestoreRole = "";
                            if (document.exists()) {
                                firestoreFirstName = document.getString("name");
                                if (firestoreFirstName == null) {
                                    firestoreFirstName = "";
                                }
                                firestoreRole = document.getString("role");


                            } else {
                                Log.w(TAG, "User document not found in Firestore.");
                            }

                            Intent intent;
                            if (firestoreRole.equals("admin")) {
                                intent = new Intent(LoginActivity.this, AdminHomeActivity.class);;
                            }
                            else {
                                intent = new Intent(LoginActivity.this, HomeActivity.class);;
                            }
                            intent.putExtra("USER_ID", user.getUid());
                            intent.putExtra("FIRST_NAME", firestoreFirstName);
                            intent.putExtra("IS_NEW_USER", isNewUser); // Pass the flag
                            startActivity(intent);
                            finish();
                        } else {
                            Log.w(TAG, "Error getting user document from Firestore.", task.getException());
                            Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                            intent.putExtra("USER_ID", user.getUid());
                            intent.putExtra("FIRST_NAME", "");
                            intent.putExtra("IS_NEW_USER", isNewUser); // Pass the flag (even on error)
                            startActivity(intent);
                            finish();
                        }
                    });
            return;
        }
        Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
        intent.putExtra("USER_ID", user.getUid());
        intent.putExtra("FIRST_NAME", firstName);
        intent.putExtra("IS_NEW_USER", isNewUser); // Pass the flag
        startActivity(intent);
        finish();
    }
}