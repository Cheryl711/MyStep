package com.step.pedometer.mystep.login;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.step.pedometer.mystep.R;
import com.step.pedometer.mystep.utils.FirebaseMethods;

public class RegisterActivity extends AppCompatActivity{
    private static final String TAG = "RegisterActivity";

    private Context mContext;
    private String email, username, password;
    private EditText mEmail, mPassword, mUsername;
    private TextView loadingPleaseWait;
    private Button btnRegister;

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private FirebaseMethods firebaseMethods;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        mContext = RegisterActivity.this;
        firebaseMethods = new FirebaseMethods(mContext);
        Log.d(TAG, "onCreate: start");

        initWidgets();
        setupFirebaseAuth();
        init();
    }

    private void init(){
        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                email = mEmail.getText().toString();
                username = mUsername.getText().toString();
                password = mPassword.getText().toString();

                if (checkInputs(email,username,password)){
                    loadingPleaseWait.setVisibility(View.VISIBLE);

                    firebaseMethods.registerNewEmail(email, password, username);
                }
            }
        });
    }

    private boolean checkInputs (String email, String username, String password){
        Log.d(TAG, "checkInputs: checking inputs for null values.");
        if (email.equals("") || username.equals("") || password.equals("")){
            Toast.makeText(mContext, "Please fill all fields.", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    //initialise the activity widgets
    private void initWidgets(){
        Log.d(TAG, "intWidgets: Initializing Widgets.");
        loadingPleaseWait = (TextView) findViewById(R.id.loadingPleaseWait);
        mUsername = (EditText) findViewById(R.id.input_username);
        btnRegister = (Button) findViewById(R.id.btn_register);
        mEmail = (EditText) findViewById(R.id.input_email);
        mPassword = (EditText) findViewById(R.id.input_password);
        mContext = RegisterActivity.this;
        loadingPleaseWait.setVisibility(View.GONE);
    }

    private boolean isStringNull(String string){
        Log.d(TAG, "isStringNull: checking string if null.");

        if (string.equals("")){
            return true;
        } else {
            return false;
        }
    }
    /*
    -----------Firebase-----------------
     */
    //set up the firebase auth object
    private void setupFirebaseAuth(){
        Log.d(TAG, "setupFirebaseAuth: setting up firebase auth");

        mAuth = FirebaseAuth.getInstance();

        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();

                if (user != null){
                    // user is signed in
                    Log.d(TAG, "onAuthStateChanged: sined_in: " + user.getUid());
                } else {
                    // user is signed out
                    Log.d(TAG, "onAuthStateChanged: signed_out");
                }
            }
        };
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        mAuth.addAuthStateListener(mAuthListener);
    }

    public void onStop(){
        super.onStop();
        if(mAuthListener != null){
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }
    /*
    -----------Firebase-----------------
     */
}

