package com.example.retroreads;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
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
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.util.ArrayList;
import java.util.List;

public class Shelf extends AppCompatActivity {

    //declaring variables
    private RecyclerView shelfRecyclerView;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private BookAdapter bookAdapter;
    private List<Book> bookList;
    private ImageView accountIcon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shelf);

        //setting up user interface
        getWindow().setStatusBarColor(getResources().getColor(android.R.color.white));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        finishProgressBar();

        //initializing bottom nav
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_shelf);
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                if (item.getItemId() == R.id.nav_catalog) {
                    Intent intent = new Intent(Shelf.this, Catalog.class);
                    startActivity(intent);
                    overridePendingTransition(0, 0);
                    return true;
                } else if (item.getItemId() == R.id.nav_for_sale) {
                    Intent intent = new Intent(Shelf.this, ForSale.class);
                    startActivity(intent);
                    overridePendingTransition(0, 0);
                    return true;
                } else if (item.getItemId() == R.id.nav_interests) {
                    Intent intent = new Intent(Shelf.this, Interests.class);
                    startActivity(intent);
                    overridePendingTransition(0, 0);
                    return true;
                } else if (item.getItemId() == R.id.nav_finances) {
                    Intent intent = new Intent(Shelf.this, Finances.class);
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
        shelfRecyclerView = findViewById(R.id.shelf_recycler_view);
        shelfRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        //initializing the edit and delete book click listeners
        bookAdapter = new BookAdapter(new ArrayList<>(), 1, this::openEditBook, this::deleteBook);

        //assigning the book adapter to the recycler view
        shelfRecyclerView.setAdapter(bookAdapter);

        loadBooks();

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

                                        Glide.with(Shelf.this)
                                                .load(imageUrl)
                                                .transform(
                                                        new CircleCrop(),
                                                        new CircularBorderTransformation(Shelf.this, 1, Color.parseColor("#91918E"))
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

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK) {
            loadBooks();
        }
        if (requestCode == 2 && resultCode == RESULT_OK) {
            loadBooks();
        }
    }

    private void loadBooks() {
        startProgressBar();

        if (bookList == null) {
            bookList = new ArrayList<>();
        }

        //retrieving user's books from firebase
        db.collection("books")
                .whereEqualTo("userId", mAuth.getCurrentUser().getUid())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    bookList.clear();  //clears any previous content
                    for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                        Book book = documentSnapshot.toObject(Book.class);
                        if (book != null) {
                            book.setId(documentSnapshot.getId()); //defines the book id
                            bookList.add(book);
                        }
                    }

                    //updates the book list
                    bookAdapter.updateBookList(bookList);
                    shelfRecyclerView.postDelayed(this::finishProgressBar, 250);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(Shelf.this, "Error loading books.", Toast.LENGTH_SHORT).show();
                    shelfRecyclerView.postDelayed(this::finishProgressBar, 250);
                });
    }

    public void openAccount(View view) {
        Intent myIntent = new Intent(getApplicationContext(), Account.class);
        startActivity(myIntent);
    }

    public void openCatalog(View view) {
        Intent myIntent = new Intent(getApplicationContext(), Catalog.class);
        startActivity(myIntent);
    }

    public void openAddBook(View view) {
        Intent myIntent = new Intent(getApplicationContext(), AddBook.class);
        startActivityForResult(myIntent, 1);
    }

    public void openEditBook(int position) {
        if (bookList != null && !bookList.isEmpty()) {
            //get book from position
            Book book = bookList.get(position);

            String bookId = book.getId();

            //starts the edit book activity with the book id
            Intent myIntent = new Intent(getApplicationContext(), EditBook.class);
            myIntent.putExtra("BOOK_ID", bookId);
            startActivityForResult(myIntent, 2);
        }
    }

    private void deleteBook(String bookId) {
        if (bookId != null && !bookId.isEmpty()) {
            db.collection("books").document(bookId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String bookName = documentSnapshot.getString("title");

                            LayoutInflater inflater = LayoutInflater.from(Shelf.this);
                            View dialogView = inflater.inflate(R.layout.book_delete_custom_dialog, null);

                            dialogView.setBackgroundResource(R.drawable.input_field);

                            TextView dialogBookName = dialogView.findViewById(R.id.dialog_book_name);
                            dialogBookName.setText(bookName);

                            AlertDialog.Builder builder = new AlertDialog.Builder(Shelf.this);
                            builder.setView(dialogView);

                            Button dialogYesButton = dialogView.findViewById(R.id.dialog_yes_button);
                            Button dialogNoButton = dialogView.findViewById(R.id.dialog_no_button);
                            AlertDialog alertDialog = builder.create();

                            alertDialog.getWindow().setBackgroundDrawableResource(R.drawable.input_field);

                            dialogYesButton.setOnClickListener(btn -> {
                                db.collection("books").document(bookId)
                                        .delete()
                                        .addOnSuccessListener(aVoid -> {
                                            deleteBookImageFromStorage(bookId);
                                            Toast.makeText(Shelf.this, "Book deleted successfully!", Toast.LENGTH_SHORT).show();
                                            loadBooks();
                                        })
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(Shelf.this, "Error deleting book.", Toast.LENGTH_SHORT).show();
                                        });
                                alertDialog.dismiss();
                            });

                            dialogNoButton.setOnClickListener(btn -> alertDialog.dismiss());

                            alertDialog.show();
                        }
                    });
        }
    }

    private void deleteBookImageFromStorage(String bookId) {
        StorageReference storageRef = FirebaseStorage.getInstance().getReference();
        StorageReference imageRef = storageRef.child("book_images/" + bookId + ".jpg");

        imageRef.delete();
    }
}