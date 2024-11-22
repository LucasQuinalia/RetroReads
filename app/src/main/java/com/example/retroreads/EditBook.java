package com.example.retroreads;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EditBook extends AppCompatActivity {

    //declaring variables
    private EditText bookTitleField, bookAuthorField, bookPublisherField, bookReleaseDateField,
            bookPageNumberField, bookIsbnField, bookDescriptionField, bookStarRateField,
            bookPriceField, bookQuantityField;
    private Spinner bookGenreSpinner;
    private RadioGroup radioReadingStatus;
    private ImageView addBookPlaceholder, accountIcon;
    private Uri imageUri;  // Definindo imageUri aqui para que fique acessível
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private String bookId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_book);

        //setting up user interface
        getWindow().setStatusBarColor(getResources().getColor(android.R.color.white));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        finishProgressBar();

        //receives book id from intent
        bookId = getIntent().getStringExtra("BOOK_ID");

        //listener to save the book
        Button saveButton = findViewById(R.id.save_button);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveBook();
            }
        });

        //initializing bottom nav
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_shelf);
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                if (item.getItemId() == R.id.nav_catalog) {
                    Intent intent = new Intent(EditBook.this, Catalog.class);
                    startActivity(intent);
                    overridePendingTransition(0, 0);
                    return true;
                } else if (item.getItemId() == R.id.nav_for_sale) {
                    Intent intent = new Intent(EditBook.this, ForSale.class);
                    startActivity(intent);
                    overridePendingTransition(0, 0);
                    return true;
                } else if (item.getItemId() == R.id.nav_interests) {
                    Intent intent = new Intent(EditBook.this, Interests.class);
                    startActivity(intent);
                    overridePendingTransition(0, 0);
                    return true;
                } else if (item.getItemId() == R.id.nav_finances) {
                    Intent intent = new Intent(EditBook.this, Finances.class);
                    startActivity(intent);
                    overridePendingTransition(0, 0);
                    return true;
                }
                return false;
            }

        });

        ImageButton deleteButton = findViewById(R.id.delete_button);

        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (bookId != null && !bookId.isEmpty()) {
                    deleteBook(bookId);
                } else {
                    Toast.makeText(EditBook.this, "Book ID not found", Toast.LENGTH_SHORT).show();
                }
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
        radioReadingStatus = findViewById(R.id.radio_reading_status);
        addBookPlaceholder = findViewById(R.id.add_book_placeholder);

        bookGenreSpinner = findViewById(R.id.book_genre_spinner);

        Spinner spinner = findViewById(R.id.book_genre_spinner);  // Verifique se o Spinner é encontrado
        if (spinner != null) {
            // Configuração do Spinner (Adapter e Listener)
            List<String> genreList = new ArrayList<>();
            genreList.add("Fiction");
            genreList.add("Non-fiction");
            genreList.add("Fantasy");
            genreList.add("Science Fiction");
            genreList.add("Romance");

            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, genreList);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(adapter);

            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    String selectedGenre = parent.getItemAtPosition(position).toString();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    // Ação caso nada seja selecionado
                }
            });
        }

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

                            // Preencher o status de leitura
                            String readingStatus = documentSnapshot.getString("readingStatus");
                            if ("want_to_read".equals(readingStatus)) {
                                radioReadingStatus.check(R.id.want_to_read);
                            } else if ("currently_reading".equals(readingStatus)) {
                                radioReadingStatus.check(R.id.currently_reading);
                            } else if ("already_read".equals(readingStatus)) {
                                radioReadingStatus.check(R.id.already_read);
                            } else if ("want_to_sell".equals(readingStatus)) {
                                radioReadingStatus.check(R.id.want_to_sell);
                            }

                            // Carregar imagem, se existir
                            String imageUrl = documentSnapshot.getString("imageUrl");
                            if (imageUrl != null) {
                                Glide.with(this).load(imageUrl).transform(new RoundedCorners(50)).into(addBookPlaceholder);
                            }
                        }
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Error loading book", Toast.LENGTH_SHORT).show());
        }

        // Listener para o botão de imagem
        addBookPlaceholder.setOnClickListener(v -> openImagePicker());

        bookReleaseDateField.addTextChangedListener(new TextWatcher() {
            private String current = "";
            private final String ddmmyyyy = "________";
            private final Calendar calendar = Calendar.getInstance();

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!s.toString().equals(current)) {
                    String clean = s.toString().replaceAll("[^\\d]", "");
                    String cleanCurrent = current.replaceAll("[^\\d]", "");

                    int cleanLength = clean.length();
                    int selectionPosition = cleanLength;
                    for (int i = 2; i <= cleanLength && i < 6; i += 2) {
                        selectionPosition++;
                    }

                    if (clean.equals(cleanCurrent)) selectionPosition--;

                    if (cleanLength < 8) {
                        clean = clean + ddmmyyyy.substring(cleanLength);
                    } else {
                        int day = Integer.parseInt(clean.substring(0, 2));
                        int month = Integer.parseInt(clean.substring(2, 4));
                        int year = Integer.parseInt(clean.substring(4, 8));

                        month = Math.max(1, Math.min(12, month));
                        calendar.set(Calendar.MONTH, month - 1);

                        year = Math.max(1900, Math.min(2100, year));
                        calendar.set(Calendar.YEAR, year);

                        day = Math.max(1, Math.min(calendar.getActualMaximum(Calendar.DAY_OF_MONTH), day));
                        calendar.set(Calendar.DAY_OF_MONTH, day);

                        clean = String.format("%02d%02d%02d", day, month, year);
                    }

                    clean = String.format("%s/%s/%s", clean.substring(0, 2),
                            clean.substring(2, 4),
                            clean.substring(4, 8));

                    current = clean;
                    bookReleaseDateField.setText(current);
                    bookReleaseDateField.setSelection(Math.min(selectionPosition, current.length()));
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        bookIsbnField.addTextChangedListener(new TextWatcher() {
            private String current = "";
            private final int maxLength = 17; // Máximo de 17 dígitos

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Impede loops infinitos ao formatar
                if (!s.toString().equals(current)) {
                    String clean = s.toString().replaceAll("[^\\d]", ""); // Remove caracteres não numéricos

                    // Limita o número de dígitos
                    if (clean.length() > maxLength) {
                        clean = clean.substring(0, maxLength);
                    }

                    // Formata o texto
                    StringBuilder formatted = new StringBuilder();
                    for (int i = 0; i < clean.length(); i++) {
                        formatted.append(clean.charAt(i));
                        // Adiciona hífens nos pontos corretos
                        if ((i == 2 || i == 3 || i == 5 || i == 11) && i < clean.length() - 1) {
                            formatted.append("-");
                        }
                    }

                    // Atualiza o texto
                    current = formatted.toString();
                    bookIsbnField.removeTextChangedListener(this); // Remove temporariamente o listener
                    bookIsbnField.setText(current);
                    bookIsbnField.setSelection(current.length()); // Move o cursor para o final
                    bookIsbnField.addTextChangedListener(this); // Reanexa o listener
                }
            }
        });

        bookStarRateField.addTextChangedListener(new TextWatcher() {
            private String current = "";
            private final int maxLength = 2; // Máximo de 2 dígitos

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Impede loops infinitos ao formatar
                if (!s.toString().equals(current)) {
                    String clean = s.toString().replaceAll("[^\\d]", ""); // Remove caracteres não numéricos

                    // Limita o número de dígitos
                    if (clean.length() > maxLength) {
                        clean = clean.substring(0, maxLength);
                    }

                    // Formata o texto
                    StringBuilder formatted = new StringBuilder();
                    for (int i = 0; i < clean.length(); i++) {
                        formatted.append(clean.charAt(i));
                        // Adiciona uma vírgula após o primeiro dígito
                        if (i == 0 && clean.length() > 1) {
                            formatted.append(".");
                        }
                    }

                    // Atualiza o texto
                    current = formatted.toString();
                    bookStarRateField.removeTextChangedListener(this); // Remove temporariamente o listener
                    bookStarRateField.setText(current);
                    bookStarRateField.setSelection(current.length()); // Move o cursor para o final
                    bookStarRateField.addTextChangedListener(this); // Reanexa o listener
                }
            }
        });

        bookPriceField.addTextChangedListener(new TextWatcher() {
            private String current = "";

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (!s.toString().equals(current)) {
                    bookPriceField.removeTextChangedListener(this); // Remove temporariamente o listener

                    // Remove caracteres não numéricos
                    String clean = s.toString().replaceAll("[^\\d]", "");

                    // Adiciona zeros para garantir que tenha ao menos dois dígitos
                    if (clean.isEmpty()) {
                        clean = "0";
                    } else if (clean.length() == 1) {
                        clean = "0" + clean;
                    }

                    // Formata o valor para moeda
                    double parsed = Double.parseDouble(clean) / 100.0;
                    String formatted = String.format("R$ %.2f", parsed).replace('.', ',');

                    // Atualiza o texto no campo
                    current = formatted;
                    bookPriceField.setText(current);
                    bookPriceField.setSelection(current.length()); // Move o cursor para o final

                    bookPriceField.addTextChangedListener(this); // Reanexa o listener
                }
            }
        });

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

                                        Glide.with(EditBook.this)
                                                .load(imageUrl)
                                                .transform(
                                                        new CircleCrop(),
                                                        new CircularBorderTransformation(EditBook.this, 1, Color.parseColor("#91918E"))
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

    private void openEditBook(int i) {
        //Método vazio
    }

    private void openImagePicker() {
        // Abrir galeria de imagens
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, 1);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == RESULT_OK && data != null) {
            imageUri = data.getData(); // Atribuindo o URI à variável de classe
            if (imageUri != null) {
                addBookPlaceholder.setImageURI(imageUri); // Exibe a imagem na ImageView
                // Usa o Glide para carregar a imagem e aplicar bordas arredondadas
                Glide.with(this)
                        .load(imageUri) // Carrega a imagem selecionada
                        .transform(new RoundedCorners(50)) // Aplica bordas arredondadas
                        .into(addBookPlaceholder); // Coloca a imagem no ImageButton
            } else {
                Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show();
            }
        }

        // Listener para o botão de imagem
        addBookPlaceholder.setOnClickListener(v -> openImagePicker());
    }

    private void saveBook() {
        startProgressBar();

        String title = bookTitleField.getText().toString().trim();
        String author = bookAuthorField.getText().toString().trim();
        String publisher = bookPublisherField.getText().toString().trim();
        String releaseDate = bookReleaseDateField.getText().toString().trim();
        String isbn = bookIsbnField.getText().toString().trim();
        String description = bookDescriptionField.getText().toString().trim();
        String priceString = bookPriceField.getText().toString().trim();
        String pageNumberString = bookPageNumberField.getText().toString().trim();
        String starRateString = bookStarRateField.getText().toString().trim();
        String quantityString = bookQuantityField.getText().toString().trim();

        //conversion
        double price = 0.0;
        int pageNumber = 0;
        double starRate = 0.0;
        int quantity = 0;

        try {
            try {
                // Conversão de preço ao salvar
                String cleanPriceString = priceString.replace("R$", "").replace(",", ".").trim();
                if (cleanPriceString.isEmpty()) {
                    throw new NumberFormatException("Price is empty or invalid.");
                }

                // Converte para double
                price = Double.parseDouble(cleanPriceString);

                // Garantir duas casas decimais ao salvar no Firebase
                price = Math.round(price * 100.00) / 100.00; // Arredonda para 2 casas decimais

            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid price format. Please enter a valid price.", Toast.LENGTH_SHORT).show();
                finishProgressBar();
                return;
            }
            pageNumber = Integer.parseInt(pageNumberString);
            starRate = Double.parseDouble(starRateString);
            quantity = Integer.parseInt(quantityString);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please fill in all the required fields.", Toast.LENGTH_SHORT).show();
            finishProgressBar();
            return;
        }

        // Verificação básica de campos obrigatórios
        if (title.isEmpty() || author.isEmpty() || releaseDate.isEmpty()) {
            Toast.makeText(this, "Please fill in all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Criar o mapa de dados para salvar no Firestore
        Map<String, Object> bookMap = new HashMap<>();
        bookMap.put("title", title);
        bookMap.put("author", author);
        bookMap.put("publisher", publisher);
        bookMap.put("releaseDate", releaseDate);
        bookMap.put("isbn", isbn);
        bookMap.put("description", description);
        bookMap.put("price", price);
        bookMap.put("pageNumber", pageNumber);
        bookMap.put("starRate", starRate);
        bookMap.put("quantity", quantity);
        bookMap.put("genre", bookGenreSpinner.getSelectedItem().toString());

        // Verificar o status de leitura
        int selectedReadingStatusId = radioReadingStatus.getCheckedRadioButtonId();
        String readingStatus = "";
        if (selectedReadingStatusId == R.id.want_to_read) {
            readingStatus = "want_to_read";
        } else if (selectedReadingStatusId == R.id.currently_reading) {
            readingStatus = "currently_reading";
        } else if (selectedReadingStatusId == R.id.already_read) {
            readingStatus = "already_read";
        } else if (selectedReadingStatusId == R.id.want_to_sell) {
            readingStatus = "want_to_sell";
        }
        bookMap.put("readingStatus", readingStatus);

        if (imageUri != null) {
            uploadImageToFirebase(imageUri, bookMap); // Chama o método de upload de imagem
        } else {
            updateBookInFirestore(bookId, bookMap); // Caso não haja imagem, atualiza direto
        }
    }

    private void updateBookInFirestore(String bookId, Map<String, Object> bookMap) {
        startProgressBar();

        db.collection("books").document(bookId)
                .update(bookMap)  // Usa o update() para alterar um documento existente
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Book updated successfully!", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finishProgressBar();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error updating book", Toast.LENGTH_SHORT).show();
                    finishProgressBar();
                });
    }

    private void uploadImageToFirebase(Uri imageUri, Map<String, Object> bookMap) {
        startProgressBar();

        StorageReference storageRef = storage.getReference();
        String fileName = System.currentTimeMillis() + ".jpg";
        StorageReference imageRef = storageRef.child("book_images/" + fileName);

        imageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        String imageUrl = uri.toString();
                        bookMap.put("imageUrl", imageUrl);
                        updateBookInFirestore(bookId, bookMap);
                        finishProgressBar();
                    });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(EditBook.this, "Error uploading image", Toast.LENGTH_SHORT).show();
                    finishProgressBar();
                });
    }

    private void deleteBook(String bookId) {
        if (bookId != null && !bookId.isEmpty()) {
            db.collection("books").document(bookId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String bookName = documentSnapshot.getString("title");

                            LayoutInflater inflater = LayoutInflater.from(EditBook.this);
                            View dialogView = inflater.inflate(R.layout.book_delete_custom_dialog, null);

                            dialogView.setBackgroundResource(R.drawable.input_field);

                            TextView dialogBookName = dialogView.findViewById(R.id.dialog_book_name);
                            dialogBookName.setText(bookName);

                            AlertDialog.Builder builder = new AlertDialog.Builder(EditBook.this);
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
                                            Toast.makeText(EditBook.this, "Book deleted successfully!", Toast.LENGTH_SHORT).show();
                                            setResult(RESULT_OK);
                                            finish();
                                        })
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(EditBook.this, "Error deleting book.", Toast.LENGTH_SHORT).show();
                                        });
                                alertDialog.dismiss();
                            });

                            dialogNoButton.setOnClickListener(btn -> alertDialog.dismiss());

                            alertDialog.show();
                        }
                    });
        }
    }

    // Método para excluir a imagem do livro do Firebase Storage (caso tenha sido carregada)
    private void deleteBookImageFromStorage(String bookId) {
        startProgressBar();

        StorageReference storageRef = FirebaseStorage.getInstance().getReference();
        StorageReference imageRef = storageRef.child("book_images/" + bookId + ".jpg");

        imageRef.delete();

        finishProgressBar();
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
}