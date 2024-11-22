package com.example.retroreads;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
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

public class ForSale extends AppCompatActivity {

    private RecyclerView for_sale_RecyclerView;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private BookAdapter bookAdapter;
    private List<Book> bookList;
    private ImageView accountIcon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_for_sale);

        //setting up user interface
        getWindow().setStatusBarColor(getResources().getColor(android.R.color.white));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        finishProgressBar();

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_for_sale);

        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                if (item.getItemId() == R.id.nav_catalog) {
                    Intent intent = new Intent(ForSale.this, Catalog.class);
                    startActivity(intent);
                    overridePendingTransition(0, 0);
                    return true;
                } else if (item.getItemId() == R.id.nav_shelf) {
                    Intent intent = new Intent(ForSale.this, Shelf.class);
                    startActivity(intent);
                    overridePendingTransition(0, 0);
                    return true;
                } else if (item.getItemId() == R.id.nav_interests) {
                    Intent intent = new Intent(ForSale.this, Interests.class);
                    startActivity(intent);
                    overridePendingTransition(0, 0);
                    return true;
                } else if (item.getItemId() == R.id.nav_finances) {
                    Intent intent = new Intent(ForSale.this, Finances.class);
                    startActivity(intent);
                    overridePendingTransition(0, 0);
                    return true;
                }
                return false;
            }
        });

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        for_sale_RecyclerView = findViewById(R.id.for_sale_recycler_view);
        for_sale_RecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        // Passando o listener de clique para o adaptador
        bookAdapter = new BookAdapter(new ArrayList<>(), 3, this::openEditBook, this::deleteBook);
        for_sale_RecyclerView.setAdapter(bookAdapter);

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

                                        Glide.with(ForSale.this)
                                                .load(imageUrl)
                                                .transform(
                                                        new CircleCrop(),
                                                        new CircularBorderTransformation(ForSale.this, 1, Color.parseColor("#91918E"))
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
        db.collection("books")
                .whereEqualTo("userId", mAuth.getCurrentUser().getUid())
                .whereEqualTo("readingStatus", "want_to_sell")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Book> bookList = new ArrayList<>();
                    for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                        Book book = documentSnapshot.toObject(Book.class);
                        if (book != null) {
                            book.setId(documentSnapshot.getId()); // Define o ID do livro
                            bookList.add(book);
                        }
                    }

                    // Verificar se a lista foi preenchida corretamente
                    if (bookList.isEmpty()) {
                        Log.d("Shelf", "No books found for this user.");
                    } else {
                        Log.d("Shelf", "Loaded " + bookList.size() + " books.");
                    }

                    // Atualiza a lista de livros no adaptador existente
                    bookAdapter.updateBookList(bookList);
                    finishProgressBar();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ForSale.this, "Error loading books", Toast.LENGTH_SHORT).show();
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

    public void openEditBook(int position) {
        if (bookList == null) {
            Log.e("Shelf", "bookList é nula");
        } else if (bookList.isEmpty()) {
            Log.e("Shelf", "bookList está vazia");
        } else {
            Log.d("Shelf", "bookList contém " + bookList.size() + " livros");
        }

        if (bookList != null && !bookList.isEmpty()) {
            // Obter o livro pela posição
            Book book = bookList.get(position);

            // Pegue o ID do livro para passá-lo para a EditBook
            String bookId = book.getId(); // Suponha que o livro tenha o ID

            // Passar o ID do livro para a próxima Activity (EditBook)
            Intent myIntent = new Intent(getApplicationContext(), EditBook.class);
            myIntent.putExtra("BOOK_ID", bookId); // Passando o bookId para EditBook
            startActivity(myIntent);
        } else {
            Log.e("Shelf", "Não foi possível acessar o livro na posição: " + position);
        }
    }


    // Método para excluir o livro
    private void deleteBook(String bookId) {
        if (bookId != null && !bookId.isEmpty()) {
            db.collection("books").document(bookId)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        // Remover a imagem do Firebase Storage, se necessário
                        deleteBookImageFromStorage(bookId);

                        Toast.makeText(ForSale.this, "Book deleted successfully", Toast.LENGTH_SHORT).show();
                        // Atualize a lista de livros após exclusão
                        loadBooks();  // Recarrega os livros após a exclusão
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(ForSale.this, "Error deleting book", Toast.LENGTH_SHORT).show();
                    });
        } else {
            Toast.makeText(this, "Invalid book ID", Toast.LENGTH_SHORT).show();
        }
    }

    // Método para excluir a imagem do livro do Firebase Storage (caso tenha sido carregada)
    private void deleteBookImageFromStorage(String bookId) {
        StorageReference storageRef = FirebaseStorage.getInstance().getReference();
        StorageReference imageRef = storageRef.child("book_images/" + bookId + ".jpg");

        imageRef.delete()
                .addOnSuccessListener(aVoid -> Log.d("DeleteBook", "Image deleted successfully"))
                .addOnFailureListener(e -> Log.e("DeleteBook", "Error deleting image", e));
    }
}