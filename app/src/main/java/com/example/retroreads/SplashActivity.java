package com.example.retroreads;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        //initializes user data
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        //verifies if the user is logged in
        new Handler().postDelayed(() -> {
            if (currentUser != null) {
                Intent intent = new Intent(SplashActivity.this, Catalog.class);
                startActivity(intent);
            } else {
                Intent intent = new Intent(SplashActivity.this, Login.class);
                startActivity(intent);
            }
            finish();
        }, 750); //delays redirect by 750ms to show the splash screen
    }
}