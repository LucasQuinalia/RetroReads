package com.example.retroreads;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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

public class SearchBook extends AppCompatActivity {

    private RecyclerView bookRecyclerView;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private BookAdapter bookAdapter;
    private ImageView accountIcon;
    private FirebaseStorage storage;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        FirebaseApp.initializeApp(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_book);

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
                    Intent intent = new Intent(SearchBook.this, ForSale.class);
                    startActivity(intent);
                    overridePendingTransition(0, 0);
                    return true;
                } else if (item.getItemId() == R.id.nav_shelf) {
                    Intent intent = new Intent(SearchBook.this, Shelf.class);
                    startActivity(intent);
                    overridePendingTransition(0, 0);
                    return true;
                } else if (item.getItemId() == R.id.nav_interests) {
                    Intent intent = new Intent(SearchBook.this, Interests.class);
                    startActivity(intent);
                    overridePendingTransition(0, 0);
                    return true;
                } else if (item.getItemId() == R.id.nav_finances) {
                    Intent intent = new Intent(SearchBook.this, Finances.class);
                    startActivity(intent);
                    overridePendingTransition(0, 0);
                    return true;
                }
                return false;
            }
        });

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        bookRecyclerView = findViewById(R.id.book_recycler_view);

        bookRecyclerView.setLayoutManager(new LinearLayoutManager(this));

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

                                        Glide.with(SearchBook.this)
                                                .load(imageUrl)
                                                .transform(
                                                        new CircleCrop(),
                                                        new CircularBorderTransformation(SearchBook.this, 1, Color.parseColor("#91918E"))
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

            EditText searchBar = findViewById(R.id.search_bar);
            searchBar.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    filterBooks(s.toString());
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });
        }

        bookAdapter = new BookAdapter(new ArrayList<>(), 0, null, null);
        bookRecyclerView.setAdapter(bookAdapter);

        // Obtendo o texto do Intent
        Intent intent = getIntent();
        String searchQuery = intent.getStringExtra("search_query");

        if (searchQuery != null && !searchQuery.isEmpty()) {
            // Use o texto para realizar a busca inicial
            filterBooks(searchQuery);

            // Opcional: Exibir o texto na barra de busca
            EditText searchBar = findViewById(R.id.search_bar);
            searchBar.setText(searchQuery);
        }
    }

    private void filterBooks(String query) {
        String currentUser = mAuth.getCurrentUser().getUid();

        // Evita crash se o adaptador não estiver inicializado
        if (bookAdapter == null) {
            Toast.makeText(this, "Adapter is not initialized", Toast.LENGTH_SHORT).show();
            return;
        }

        // Busca no Firestore
        db.collection("books")
                .whereNotEqualTo("userId", currentUser)
                .whereEqualTo("readingStatus", "want_to_sell")
                .whereGreaterThanOrEqualTo("title", query) // Filtrar títulos maiores ou iguais à consulta
                .whereLessThanOrEqualTo("title", query + "\uf8ff") // Filtrar títulos menores ou iguais ao intervalo
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Book> filteredBooks = new ArrayList<>();
                    for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                        Book book = documentSnapshot.toObject(Book.class);
                        book.setId(documentSnapshot.getId());
                        filteredBooks.add(book);
                    }

                    if (!filteredBooks.isEmpty()) {
                        updateRecyclerView(filteredBooks);
                    } else {
                        Toast.makeText(SearchBook.this, "No books found for the query.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(SearchBook.this, "Error filtering books: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("Catalog", "Error filtering books", e);
                });
    }

    private void updateRecyclerView(List<Book> books) {
        bookAdapter.updateBookList(books);
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

        db.collection("books")
                .whereNotEqualTo("userId", currentUser)
                .whereEqualTo("readingStatus", "want_to_sell")
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
                    bookRecyclerView.setLayoutManager(layoutManager);
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
                    bookRecyclerView.setAdapter(bookAdapter);
                    bookRecyclerView.postDelayed(this::finishProgressBar, 250);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(SearchBook.this, "Error loading books", Toast.LENGTH_SHORT).show();
                    Log.e("Shelf", "Error loading books: ", e);
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
}