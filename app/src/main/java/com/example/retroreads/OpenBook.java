package com.example.retroreads;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import java.util.ArrayList;
import java.util.List;

public class OpenBook extends AppCompatActivity {

    //declaring variables
    private EditText bookTitleField, bookAuthorField, bookPublisherField, bookReleaseDateField,
            bookPageNumberField, bookIsbnField, bookDescriptionField, bookStarRateField,
            bookPriceField, bookQuantityField;
    private Spinner bookGenreSpinner;
    private ImageView addBookPlaceholder, accountIcon;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private String bookId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open_book);

        //setting up user interface
        getWindow().setStatusBarColor(getResources().getColor(android.R.color.white));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        finishProgressBar();

        // Recupera os dados do Intent
        String bookId = getIntent().getStringExtra("BOOK_ID");
        String ownerId = getIntent().getStringExtra("BOOK_USER_ID");

        // Configura o clique do botão de interesse
        Button interestButton = findViewById(R.id.interest_button);
        interestButton.setOnClickListener(v -> handleInterest(bookId, ownerId));

        //initializing bottom nav
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_catalog);
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                if (item.getItemId() == R.id.nav_catalog) {
                    Intent intent = new Intent(OpenBook.this, Catalog.class);
                    startActivity(intent);
                    overridePendingTransition(0, 0);
                    return true;
                } else if (item.getItemId() == R.id.nav_for_sale) {
                    Intent intent = new Intent(OpenBook.this, ForSale.class);
                    startActivity(intent);
                    overridePendingTransition(0, 0);
                    return true;
                } else if (item.getItemId() == R.id.nav_interests) {
                    Intent intent = new Intent(OpenBook.this, Interests.class);
                    startActivity(intent);
                    overridePendingTransition(0, 0);
                    return true;
                } else if (item.getItemId() == R.id.nav_finances) {
                    Intent intent = new Intent(OpenBook.this, Finances.class);
                    startActivity(intent);
                    overridePendingTransition(0, 0);
                    return true;
                }
                return false;
            }

        });

        bookTitleField = findViewById(R.id.book_title_field);
        bookAuthorField = findViewById(R.id.book_author_field);
        bookPublisherField = findViewById(R.id.book_publisher_field);
        bookReleaseDateField = findViewById(R.id.book_release_date_field);
        bookPageNumberField = findViewById(R.id.book_page_number_field);
        bookIsbnField = findViewById(R.id.book_isbn_field);
        bookDescriptionField = findViewById(R.id.book_description_field);
        bookStarRateField = findViewById(R.id.book_star_rate_field);
        bookPriceField = findViewById(R.id.book_price_field);
        bookQuantityField = findViewById(R.id.book_quantity_field);
        addBookPlaceholder = findViewById(R.id.add_book_placeholder);
        bookGenreSpinner = findViewById(R.id.book_genre_spinner);

        bookTitleField.setEnabled(false);
        bookAuthorField.setEnabled(false);
        bookPublisherField.setEnabled(false);
        bookReleaseDateField.setEnabled(false);
        bookPageNumberField.setEnabled(false);
        bookIsbnField.setEnabled(false);
        bookDescriptionField.setEnabled(false);
        bookStarRateField.setEnabled(false);
        bookPriceField.setEnabled(false);
        bookQuantityField.setEnabled(false);
        addBookPlaceholder.setEnabled(false);
        bookGenreSpinner.setEnabled(false);

        // Inicializando Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        if (bookId != null) {
            db.collection("books").document(bookId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            bookTitleField.setText(documentSnapshot.getString("title"));
                            bookAuthorField.setText(documentSnapshot.getString("author"));
                            bookPublisherField.setText(documentSnapshot.getString("publisher"));
                            bookReleaseDateField.setText(documentSnapshot.getString("releaseDate"));
                            bookIsbnField.setText(documentSnapshot.getString("isbn"));
                            bookDescriptionField.setText(documentSnapshot.getString("description"));

                            // Formatar o preço para garantir 2 casas decimais
                            double price = documentSnapshot.getDouble("price");
                            bookPriceField.setText(String.format("%.2f", price));  // Exibir preço com 2 casas decimais

                            bookPageNumberField.setText(String.valueOf(documentSnapshot.getLong("pageNumber")));
                            bookStarRateField.setText(String.valueOf(documentSnapshot.getDouble("starRate")));
                            bookQuantityField.setText(String.valueOf(documentSnapshot.getLong("quantity")));

                            // Preencher o Spinner de Gênero
                            String genre = documentSnapshot.getString("genre");
                            ArrayAdapter<CharSequence> genreAdapter = ArrayAdapter.createFromResource(this,
                                    R.array.book_genre_options, android.R.layout.simple_spinner_item);
                            genreAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            bookGenreSpinner.setAdapter(genreAdapter);
                            int genrePosition = genreAdapter.getPosition(genre);
                            bookGenreSpinner.setSelection(genrePosition);

                            // Carregar imagem, se existir
                            String imageUrl = documentSnapshot.getString("imageUrl");
                            if (imageUrl != null) {
                                Glide.with(this).load(imageUrl).transform(new RoundedCorners(50)).into(addBookPlaceholder);
                            }
                        }
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Error loading book.", Toast.LENGTH_SHORT).show());
        }

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

                                        Glide.with(OpenBook.this)
                                                .load(imageUrl)
                                                .transform(
                                                        new CircleCrop(),
                                                        new CircularBorderTransformation(OpenBook.this, 1, Color.parseColor("#91918E"))
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

    public void openAccount(View view) {
        Intent myIntent = new Intent(getApplicationContext(), Account.class);
        startActivity(myIntent);
    }

    public void openCatalog(View view) {
        Intent myIntent = new Intent(getApplicationContext(), Catalog.class);
        startActivity(myIntent);
    }

    public void openSearch(View view) {
        // Obtendo o texto do EditText
        EditText searchBar = findViewById(R.id.search_bar);
        String searchText = searchBar.getText().toString().trim();

        // Criando o Intent e passando o texto como extra
        Intent myIntent = new Intent(getApplicationContext(), SearchBook.class);
        myIntent.putExtra("search_query", searchText); // Envia o texto para a próxima Activity
        startActivity(myIntent);
        overridePendingTransition(0, 0);
    }

    private void handleInterest(String bookId, String ownerId) {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        if (!ownerId.equals(mAuth.getCurrentUser().getUid())) {
            db.collection("users").document(ownerId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String ownerName = documentSnapshot.getString("name");
                            String ownerPhone = documentSnapshot.getString("phoneNumber");

                            // Inflate o layout customizado
                            LayoutInflater inflater = LayoutInflater.from(this);
                            View dialogView = inflater.inflate(R.layout.interest_register_custom_dialog, null);

                            // Define o fundo para o layout interno
                            dialogView.setBackgroundResource(R.drawable.input_field);

                            TextView dialogName = dialogView.findViewById(R.id.dialog_name);
                            TextView dialogPhone = dialogView.findViewById(R.id.dialog_phone);
                            dialogName.setText(ownerName);
                            dialogPhone.setText(ownerPhone);

                            AlertDialog.Builder builder = new AlertDialog.Builder(this);
                            builder.setView(dialogView);

                            Button dialogButton = dialogView.findViewById(R.id.dialog_button);
                            AlertDialog alertDialog = builder.create();

                            // Aplica o fundo com bordas arredondadas no próprio AlertDialog
                            alertDialog.getWindow().setBackgroundDrawableResource(R.drawable.input_field);

                            dialogButton.setOnClickListener(btn -> alertDialog.dismiss());

                            alertDialog.show();

                            // Adiciona o usuário à lista de interessados
                            registerInterest(bookId, mAuth.getCurrentUser().getUid());
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error retrieving user data.", Toast.LENGTH_SHORT).show();
                    });
        } else {
            Toast.makeText(this, "This book is yours!", Toast.LENGTH_SHORT).show();
        }
    }

    // Função para registrar o interesse
    private void registerInterest(String bookId, String userId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Adiciona o usuário à lista de interessados do livro
        db.collection("books").document(bookId)
                .update("interestedUsers", FieldValue.arrayUnion(userId));
    }
}