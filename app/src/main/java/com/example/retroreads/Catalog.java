package com.example.retroreads;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
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
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;

import java.util.ArrayList;
import java.util.List;

public class Catalog  extends AppCompatActivity {

    private RecyclerView fictionRecyclerView;
    private RecyclerView fantasyRecyclerView;
    private RecyclerView philosophyRecyclerView;
    private RecyclerView economicsRecyclerView;
    private RecyclerView businessRecyclerView;
    private RecyclerView personalDevelopmentRecyclerView;
    private RecyclerView historicalRecyclerView;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private BookAdapter bookAdapter;
    private ImageView accountIcon;
    private FirebaseStorage storage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        FirebaseApp.initializeApp(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_catalog);

        //setting up user interface
        getWindow().setStatusBarColor(getResources().getColor(android.R.color.white));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        finishProgressBar();

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_catalog);

        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                if (item.getItemId() == R.id.nav_for_sale) {
                    Intent intent = new Intent(Catalog.this, ForSale.class);
                    startActivity(intent);
                    overridePendingTransition(0, 0);
                    return true;
                } else if (item.getItemId() == R.id.nav_shelf) {
                    Intent intent = new Intent(Catalog.this, Shelf.class);
                    startActivity(intent);
                    overridePendingTransition(0, 0);
                    return true;
                } else if (item.getItemId() == R.id.nav_interests) {
                    Intent intent = new Intent(Catalog.this, Interests.class);
                    startActivity(intent);
                    overridePendingTransition(0, 0);
                    return true;
                } else if (item.getItemId() == R.id.nav_finances) {
                    Intent intent = new Intent(Catalog.this, Finances.class);
                    startActivity(intent);
                    overridePendingTransition(0, 0);
                    return true;
                }
                return false;
            }
        });

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        fictionRecyclerView = findViewById(R.id.fiction_recycler_view);
        fantasyRecyclerView = findViewById(R.id.fantasy_recycler_view);
        philosophyRecyclerView = findViewById(R.id.philosophy_recycler_view);
        economicsRecyclerView = findViewById(R.id.economics_recycler_view);
        businessRecyclerView = findViewById(R.id.business_recycler_view);
        personalDevelopmentRecyclerView = findViewById(R.id.personal_development_recycler_view);
        historicalRecyclerView = findViewById(R.id.historical_recycler_view);

        fictionRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        fantasyRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        philosophyRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        economicsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        businessRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        personalDevelopmentRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        historicalRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        loadBooks();

        FirebaseStorage storage = FirebaseStorage.getInstance();
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

                                        Glide.with(Catalog.this)
                                                .load(imageUrl)
                                                .transform(
                                                        new CircleCrop(),
                                                        new CircularBorderTransformation(Catalog.this, 1, Color.parseColor("#91918E"))
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

    private void loadBooks() {
        startProgressBar();
        String currentUser = mAuth.getCurrentUser().getUid();

        //fiction books
        db.collection("books")
                .whereNotEqualTo("userId", currentUser)
                .whereEqualTo("readingStatus", "want_to_sell")
                .whereEqualTo("genre", "Fiction")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // Converte os documentos em objetos Book e atribui o id manualmente
                    List<Book> bookList = new ArrayList<>();
                    for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                        Book book = documentSnapshot.toObject(Book.class);
                        book.setId(documentSnapshot.getId()); // Aqui estamos atribuindo o ID do documento
                        bookList.add(book);
                    }

                    // Configura o LayoutManager e o Adaptador
                    LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
                    fictionRecyclerView.setLayoutManager(layoutManager);
                    bookAdapter = new BookAdapter(bookList, 0, new BookAdapter.OnBookClickListener() {
                        @Override
                        public void onBookClick(int position) {

                        }

                        public void onBookClick(Book book) {
                            // Não faz nada, pode ser vazio
                        }
                    }, new BookAdapter.OnBookDeleteListener() {
                        @Override
                        public void onBookDelete(String bookId) {

                        }

                        public void onBookDelete(Book book) {
                            // Não faz nada, pode ser vazio
                        }
                    });
                    fictionRecyclerView.setAdapter(bookAdapter);
                    fictionRecyclerView.postDelayed(this::finishProgressBar, 250);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(Catalog.this, "Error loading books", Toast.LENGTH_SHORT).show();
                    Log.e("Shelf", "Error loading books: ", e);
                });

        //fantasy books
        db.collection("books")
                .whereNotEqualTo("userId", currentUser)
                .whereEqualTo("readingStatus", "want_to_sell")
                .whereEqualTo("genre", "Fantasy")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // Converte os documentos em objetos Book e atribui o id manualmente
                    List<Book> bookList = new ArrayList<>();
                    for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                        Book book = documentSnapshot.toObject(Book.class);
                        book.setId(documentSnapshot.getId()); // Aqui estamos atribuindo o ID do documento
                        bookList.add(book);
                    }

                    // Configura o LayoutManager e o Adaptador
                    LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
                    fantasyRecyclerView.setLayoutManager(layoutManager);
                    bookAdapter = new BookAdapter(bookList, 0, new BookAdapter.OnBookClickListener() {
                        @Override
                        public void onBookClick(int position) {

                        }

                        public void onBookClick(Book book) {
                            // Não faz nada, pode ser vazio
                        }
                    }, new BookAdapter.OnBookDeleteListener() {
                        @Override
                        public void onBookDelete(String bookId) {

                        }

                        public void onBookDelete(Book book) {
                            // Não faz nada, pode ser vazio
                        }
                    });
                    fantasyRecyclerView.setAdapter(bookAdapter);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(Catalog.this, "Error loading books", Toast.LENGTH_SHORT).show();
                    Log.e("Shelf", "Error loading books: ", e);
                });

        //philosophy books
        db.collection("books")
                .whereNotEqualTo("userId", currentUser)
                .whereEqualTo("readingStatus", "want_to_sell")
                .whereEqualTo("genre", "Philosophy")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // Converte os documentos em objetos Book e atribui o id manualmente
                    List<Book> bookList = new ArrayList<>();
                    for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                        Book book = documentSnapshot.toObject(Book.class);
                        book.setId(documentSnapshot.getId()); // Aqui estamos atribuindo o ID do documento
                        bookList.add(book);
                    }

                    // Configura o LayoutManager e o Adaptador
                    LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
                    philosophyRecyclerView.setLayoutManager(layoutManager);
                    bookAdapter = new BookAdapter(bookList, 0, new BookAdapter.OnBookClickListener() {
                        @Override
                        public void onBookClick(int position) {

                        }

                        public void onBookClick(Book book) {
                            // Não faz nada, pode ser vazio
                        }
                    }, new BookAdapter.OnBookDeleteListener() {
                        @Override
                        public void onBookDelete(String bookId) {

                        }

                        public void onBookDelete(Book book) {
                            // Não faz nada, pode ser vazio
                        }
                    });
                    philosophyRecyclerView.setAdapter(bookAdapter);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(Catalog.this, "Error loading books", Toast.LENGTH_SHORT).show();
                    Log.e("Shelf", "Error loading books: ", e);
                });

        //economics books
        db.collection("books")
                .whereNotEqualTo("userId", currentUser)
                .whereEqualTo("readingStatus", "want_to_sell")
                .whereEqualTo("genre", "Economics")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // Converte os documentos em objetos Book e atribui o id manualmente
                    List<Book> bookList = new ArrayList<>();
                    for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                        Book book = documentSnapshot.toObject(Book.class);
                        book.setId(documentSnapshot.getId()); // Aqui estamos atribuindo o ID do documento
                        bookList.add(book);
                    }

                    // Configura o LayoutManager e o Adaptador
                    LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
                    economicsRecyclerView.setLayoutManager(layoutManager);
                    bookAdapter = new BookAdapter(bookList, 0, new BookAdapter.OnBookClickListener() {
                        @Override
                        public void onBookClick(int position) {

                        }

                        public void onBookClick(Book book) {
                            // Não faz nada, pode ser vazio
                        }
                    }, new BookAdapter.OnBookDeleteListener() {
                        @Override
                        public void onBookDelete(String bookId) {

                        }

                        public void onBookDelete(Book book) {
                            // Não faz nada, pode ser vazio
                        }
                    });
                    economicsRecyclerView.setAdapter(bookAdapter);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(Catalog.this, "Error loading books", Toast.LENGTH_SHORT).show();
                    Log.e("Shelf", "Error loading books: ", e);
                });

        //business books
        db.collection("books")
                .whereNotEqualTo("userId", currentUser)
                .whereEqualTo("readingStatus", "want_to_sell")
                .whereEqualTo("genre", "Business")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // Converte os documentos em objetos Book e atribui o id manualmente
                    List<Book> bookList = new ArrayList<>();
                    for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                        Book book = documentSnapshot.toObject(Book.class);
                        book.setId(documentSnapshot.getId()); // Aqui estamos atribuindo o ID do documento
                        bookList.add(book);
                    }

                    // Configura o LayoutManager e o Adaptador
                    LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
                    businessRecyclerView.setLayoutManager(layoutManager);
                    bookAdapter = new BookAdapter(bookList, 0, new BookAdapter.OnBookClickListener() {
                        @Override
                        public void onBookClick(int position) {

                        }

                        public void onBookClick(Book book) {
                            // Não faz nada, pode ser vazio
                        }
                    }, new BookAdapter.OnBookDeleteListener() {
                        @Override
                        public void onBookDelete(String bookId) {

                        }

                        public void onBookDelete(Book book) {
                            // Não faz nada, pode ser vazio
                        }
                    });
                    businessRecyclerView.setAdapter(bookAdapter);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(Catalog.this, "Error loading books", Toast.LENGTH_SHORT).show();
                    Log.e("Shelf", "Error loading books: ", e);
                });

        //personal development books
        db.collection("books")
                .whereNotEqualTo("userId", currentUser)
                .whereEqualTo("readingStatus", "want_to_sell")
                .whereEqualTo("genre", "Personal Development")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // Converte os documentos em objetos Book e atribui o id manualmente
                    List<Book> bookList = new ArrayList<>();
                    for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                        Book book = documentSnapshot.toObject(Book.class);
                        book.setId(documentSnapshot.getId()); // Aqui estamos atribuindo o ID do documento
                        bookList.add(book);
                    }

                    // Configura o LayoutManager e o Adaptador
                    LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
                    personalDevelopmentRecyclerView.setLayoutManager(layoutManager);
                    bookAdapter = new BookAdapter(bookList, 0, new BookAdapter.OnBookClickListener() {
                        @Override
                        public void onBookClick(int position) {

                        }

                        public void onBookClick(Book book) {
                            // Não faz nada, pode ser vazio
                        }
                    }, new BookAdapter.OnBookDeleteListener() {
                        @Override
                        public void onBookDelete(String bookId) {

                        }

                        public void onBookDelete(Book book) {
                            // Não faz nada, pode ser vazio
                        }
                    });
                    personalDevelopmentRecyclerView.setAdapter(bookAdapter);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(Catalog.this, "Error loading books", Toast.LENGTH_SHORT).show();
                    Log.e("Shelf", "Error loading books: ", e);
                });

        //historical books
        db.collection("books")
                .whereNotEqualTo("userId", currentUser)
                .whereEqualTo("readingStatus", "want_to_sell")
                .whereEqualTo("genre", "Historical")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // Converte os documentos em objetos Book e atribui o id manualmente
                    List<Book> bookList = new ArrayList<>();
                    for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                        Book book = documentSnapshot.toObject(Book.class);
                        book.setId(documentSnapshot.getId()); // Aqui estamos atribuindo o ID do documento
                        bookList.add(book);
                    }

                    // Configura o LayoutManager e o Adaptador
                    LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
                    historicalRecyclerView.setLayoutManager(layoutManager);
                    bookAdapter = new BookAdapter(bookList, 0, new BookAdapter.OnBookClickListener() {
                        @Override
                        public void onBookClick(int position) {

                        }

                        public void onBookClick(Book book) {
                            // Não faz nada, pode ser vazio
                        }
                    }, new BookAdapter.OnBookDeleteListener() {
                        @Override
                        public void onBookDelete(String bookId) {

                        }

                        public void onBookDelete(Book book) {
                            // Não faz nada, pode ser vazio
                        }
                    });
                    historicalRecyclerView.setAdapter(bookAdapter);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(Catalog.this, "Error loading books", Toast.LENGTH_SHORT).show();
                    Log.e("Shelf", "Error loading books: ", e);
                });
    }

    public void openAccount(View view) {
        Intent myIntent = new Intent(getApplicationContext(), Account.class);
        startActivity(myIntent);
    }
}
