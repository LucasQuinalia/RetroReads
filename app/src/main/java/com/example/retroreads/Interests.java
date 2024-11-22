package com.example.retroreads;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;

public class Interests extends AppCompatActivity {

    //declaring variables
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private RecyclerView interestedBooksRecyclerView;
    private BookAdapter bookAdapter;
    private List<Book> interestedBooksList;
    private ImageView accountIcon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_interests);

        //setting up user interface
        getWindow().setStatusBarColor(getResources().getColor(android.R.color.white));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        startProgressBar();

        //initializing bottom nav
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_interests);
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                if (item.getItemId() == R.id.nav_catalog) {
                    Intent intent = new Intent(Interests.this, Catalog.class);
                    startActivity(intent);
                    overridePendingTransition(0, 0);
                    return true;
                } else if (item.getItemId() == R.id.nav_for_sale) {
                    Intent intent = new Intent(Interests.this, ForSale.class);
                    startActivity(intent);
                    overridePendingTransition(0, 0);
                    return true;
                } else if (item.getItemId() == R.id.nav_shelf) {
                    Intent intent = new Intent(Interests.this, Shelf.class);
                    startActivity(intent);
                    overridePendingTransition(0, 0);
                    return true;
                } else if (item.getItemId() == R.id.nav_finances) {
                    Intent intent = new Intent(Interests.this, Finances.class);
                    startActivity(intent);
                    overridePendingTransition(0, 0);
                    return true;
                }
                return false;
            }
        });

        //setting up firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        //setting up specific recycler view
        interestedBooksRecyclerView = findViewById(R.id.interests_recycler_view);
        interestedBooksList = new ArrayList<>();

        //initializing the edit and delete book click listeners
        bookAdapter = new BookAdapter(new ArrayList<>(), 2, this::openEditBook, this::deleteBook);

        //assigning the book adapter to the recycler view
        interestedBooksRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        interestedBooksRecyclerView.setAdapter(bookAdapter);

        loadInterests();

        //verifying if the current user is logged in
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();

            //retrieving data from firebase collection
            db.collection("users").document(userId).get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                String imageUrl = document.getString("imageUrl");
                                if (imageUrl != null) {
                                    if (!isFinishing() && !isDestroyed()) {
                                        accountIcon = findViewById(R.id.account);
                                        resizeImageView(accountIcon, 48, 48);
                                        setImageViewMargin(accountIcon, 14);

                                        Glide.with(Interests.this)
                                                .load(imageUrl)
                                                .transform(
                                                        new CircleCrop(),
                                                        new CircularBorderTransformation(Interests.this, 1, Color.parseColor("#91918E"))
                                                )
                                                .into(accountIcon);
                                    }
                                }
                            } else {
                                Toast.makeText(this, "User not found.", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(this, "Error retrieving data: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void resizeImageView(ImageView imageView, int widthDp, int heightDp) {
        // Converter dp para pixels
        int widthPx = dpToPx(widthDp);
        int heightPx = dpToPx(heightDp);

        // Definir novos parâmetros de layout
        ViewGroup.LayoutParams params = imageView.getLayoutParams();
        params.width = widthPx;
        params.height = heightPx;
        imageView.setLayoutParams(params);
    }

    // Método para converter dp para pixels
    private int dpToPx(int dp) {
        return Math.round(getResources().getDisplayMetrics().density * dp);
    }

    private void setImageViewMargin(ImageView imageView, int marginDp) {
        int marginPx = dpToPx(marginDp);

        if (imageView.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams marginParams = (ViewGroup.MarginLayoutParams) imageView.getLayoutParams();
            marginParams.setMargins(marginPx, marginPx, marginPx, marginPx);
            imageView.setLayoutParams(marginParams);
        }
    }

    private void loadInterests() {
        String currentUserId = mAuth.getCurrentUser().getUid();

        if (currentUserId != null) {
            bookAdapter.loadInterestedBooks(currentUserId);

            //waits 1 second to guarantee the recycler view update
            interestedBooksRecyclerView.postDelayed(this::finishProgressBar, 250);
        }
    }

    private void startProgressBar() {
        View progressBackground = findViewById(R.id.progress_background);
        ProgressBar progressBar = findViewById(R.id.progress_bar);

        progressBackground.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.VISIBLE);
    }

    private void finishProgressBar() {
        View progressBackground = findViewById(R.id.progress_background);
        ProgressBar progressBar = findViewById(R.id.progress_bar);

        progressBackground.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
    }

    public void openAccount(View view) {
        Intent myIntent = new Intent(getApplicationContext(), Account.class);
        startActivity(myIntent);
    }

    public void openCatalog(View view) {
        Intent myIntent = new Intent(getApplicationContext(), Catalog.class);
        startActivity(myIntent);
    }

    public void openEditBook(int position) {

    }

    private void deleteBook(String bookId) {

    }
}