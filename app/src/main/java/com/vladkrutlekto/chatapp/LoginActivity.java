package com.vladkrutlekto.chatapp;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.transition.Fade;
import android.support.transition.Scene;
import android.support.transition.TransitionManager;
import android.support.transition.TransitionSet;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity
        implements View.OnClickListener, GoogleApiClient.OnConnectionFailedListener {

    // Tags, RequestCodes, Keys (constants)
    private static final String TAG = "SignInActivity";
    private static final int RC_SIGN_IN = 125;

    // Objects
    private User user;

    // Views and Buttons
    private RelativeLayout loginFirst, loginSecond;
    private Button signInButton, buttonToChat;
    private LinearLayout userProfile;
    private ImageView userPic;
    private TextView userName, userEmail;
    private EditText userChatNameET;
    private Button signOutButton;
    private FrameLayout sceneRoot;

    // Firebase and GoogleSignIn APIs
    private GoogleApiClient mGoogleApiClient;
    private FirebaseAuth mAuth;
    private CollectionReference colRef = FirebaseFirestore.getInstance().collection("userList");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Views for scenes
        sceneRoot = findViewById(R.id.scene_root);
        loginFirst = sceneRoot.findViewById(R.id.login_first);
        loginSecond = sceneRoot.findViewById(R.id.login_second);

        // User views
        userProfile = loginSecond.findViewById(R.id.user_profile);
        userName = loginSecond.findViewById(R.id.user_acc_name);
        userEmail = loginSecond.findViewById(R.id.user_acc_email);
        userPic = loginSecond.findViewById(R.id.user_acc_pic);
        userChatNameET = loginSecond.findViewById(R.id.user_chat_name);

        // Buttons and listeners
        buttonToChat = loginSecond.findViewById(R.id.button_to_chat);
        signInButton = loginFirst.findViewById(R.id.sign_in_button);
        signOutButton = loginSecond.findViewById(R.id.sign_out_button);
        signInButton.setOnClickListener(this);
        signOutButton.setOnClickListener(this);
        buttonToChat.setOnClickListener(this);

        // Firebase
        mAuth = FirebaseAuth.getInstance();

        // Google
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

        // If there is authenticated firebase user, load 2nd state (user info)
        FirebaseUser firebaseUser = mAuth.getCurrentUser();
        if (firebaseUser != null) {
            user = new User(firebaseUser.getDisplayName(),
                   firebaseUser.getEmail(),
                    firebaseUser.getPhotoUrl().toString(),
                    firebaseUser.getUid());
            userName.setText(user.getUserName());
            userEmail.setText(user.getUserEmail());
            Glide.with(LoginActivity.this).load(user.getUserPicUriString()).into(userPic);

            // Get nickname from Firestore by current userId
            DocumentReference userRef = colRef.document(firebaseUser.getUid());
            userRef.get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    DocumentSnapshot snapshot = task.getResult();
                    Log.d(TAG, "snapshot (" + userRef.getPath() + ") exists? " + Boolean.toString(snapshot.exists()));
                    if (snapshot.exists()) {
                        // Get nickname and fill the TextView
                        String userNickName = (String) snapshot.get("userNickName");
                        userChatNameET.setText(userNickName);
                    }
                }
            });

            updateUI(true);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            handleSignInResult(result);
        }
    }

    private void handleSignInResult(GoogleSignInResult result) {
        Log.d(TAG, "handleSignInResult:" + result.isSuccess());
        if (result.isSuccess()) {
            // Getting Google+ profile parameters
            final GoogleSignInAccount acct = result.getSignInAccount();

            // Getting credential and sign in Firebase
            AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);

            mAuth.signInWithCredential(credential).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    final FirebaseUser firebaseUser = task.getResult().getUser();
                    user = new User(firebaseUser.getDisplayName(),
                           firebaseUser.getEmail(),
                           firebaseUser.getPhotoUrl().toString(),
                           firebaseUser.getUid());

                    //Set info into views
                    userName.setText(user.getUserName());
                    userEmail.setText(user.getUserEmail());
                    Glide.with(LoginActivity.this).load(user.getUserPicUriString()).into(userPic);

                    // If user directory with current id exists, get nickname. O/w set user to DB
                    DocumentReference userRef = colRef.document(user.getUserId());
                    userRef.get().addOnCompleteListener(task1 -> {
                        if (task1.isSuccessful()) {
                            DocumentSnapshot snapshot = task1.getResult();

                            Log.d(TAG, String.format("snapshot (%s) exists? %s",
                                    userRef.getPath(), Boolean.toString(snapshot.exists())));

                            if (snapshot.exists()) {
                                // Get nickname and fill the TextView
                                String userNickName = (String) snapshot.get("userNickName");
                                userChatNameET.setText(userNickName);
                            } else {
                                // Set user object to Firestore
                                userRef.set(user);
                            }
                        }
                    });

                    // Animate motion
                    final Scene scene2 = new Scene(sceneRoot, loginSecond);
                    TransitionSet set = new TransitionSet();
                    set.addTransition(new Fade());
                    set.setDuration(1000);
                    TransitionManager.go(scene2, set);
                    loginSecond.setVisibility(View.VISIBLE);
                }
            });
        } else {
            updateUI(false);
        }
    }

    // Start signing in with Google+
    private void signIn() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    // Exit from Google and Firebase accounts
    private void signOut() {
        Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(
                status -> {
                    mAuth.signOut();

                    // Animate motion
                    final Scene scene1 = new Scene(sceneRoot, loginFirst);
                    TransitionSet set = new TransitionSet();
                    set.addTransition(new Fade());
                    set.setDuration(1000);
                    TransitionManager.go(scene1, set);
                    loginFirst.setVisibility(View.VISIBLE);
                });
    }

    // Go to ChatActivity with written nickname
    private void goToChat() {
        Intent intent = new Intent(this, ChatActivity.class);
        String userNickName = userChatNameET.getText().toString();
        user.setUserNickName(userNickName);
        colRef.document(user.getUserId()).update("userNickName", user.getUserNickName());
        intent.putExtra("usernick", user.getUserNickName());
        overridePendingTransition(R.anim.fui_slide_out_left, R.anim.fui_slide_in_right);
        startActivity(intent);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed:" + connectionResult);
    }

    // Update UI that depends on user state
    private void updateUI(boolean signedIn) {
        if (signedIn) {
            loginFirst.setVisibility(View.GONE);
            loginSecond.setVisibility(View.VISIBLE);
        } else {
            loginFirst.setVisibility(View.VISIBLE);
            loginSecond.setVisibility(View.GONE);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.sign_in_button:
                signIn();
                break;
            case R.id.sign_out_button:
                signOut();
                break;
            case R.id.button_to_chat:
                if (userChatNameET.getText().toString().length() != 0) goToChat();
                else Toast.makeText(this, getString(R.string.enter_nickname), Toast.LENGTH_SHORT).show();
                break;
        }
    }
}
