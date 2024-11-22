package com.example.retroreads;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.EditText;
import android.widget.ImageView;
import androidx.annotation.NonNull;
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

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddBook extends AppCompatActivity {

    //declaring variables
    private EditText bookTitleField, bookAuthorField, bookPublisherField, bookReleaseDateField,
            bookPageNumberField, bookIsbnField, bookDescriptionField, bookStarRateField,
            bookPriceField, bookQuantityField;
    private Spinner bookGenreSpinner;
    private RadioGroup radioReadingStatus;
    private ImageView addBookPlaceholder, accountIcon;
    private Uri imageUri;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_book);

        //setting up user interface
        getWindow().setStatusBarColor(getResources().getColor(android.R.color.white));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        finishProgressBar();

        //initializing bottom nav
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        //selecting the shelf icon as selected
        bottomNavigationView.setSelectedItemId(R.id.nav_shelf);
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                if (item.getItemId() == R.id.nav_catalog) {
                    Intent intent = new Intent(AddBook.this, Catalog.class);
                    startActivity(intent);
                    overridePendingTransition(0, 0);
                    return true;
                } else if (item.getItemId() == R.id.nav_for_sale) {
                    Intent intent = new Intent(AddBook.this, ForSale.class);
                    startActivity(intent);
                    overridePendingTransition(0, 0);
                    return true;
                } else if (item.getItemId() == R.id.nav_interests) {
                    Intent intent = new Intent(AddBook.this, Interests.class);
                    startActivity(intent);
                    overridePendingTransition(0, 0);
                    return true;
                } else if (item.getItemId() == R.id.nav_finances) {
                    Intent intent = new Intent(AddBook.this, Finances.class);
                    startActivity(intent);
                    overridePendingTransition(0, 0);
                    return true;
                }
                return false;
            }
        });

        //initializing genre spinner
        Spinner genreSpinner = findViewById(R.id.book_genre_spinner);

        //setting up arrayadapter using the spinner options from strings.xml
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.book_genre_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        genreSpinner.setAdapter(adapter);

        //listener to get the user selection
        genreSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedGenre = parent.getItemAtPosition(position).toString();
                //action if some genre is selected
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                //action if nothing is selected
            }
        });

        //initializing firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        //setting up fields
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
        bookGenreSpinner = findViewById(R.id.book_genre_spinner);
        radioReadingStatus = findViewById(R.id.radio_reading_status);
        addBookPlaceholder = findViewById(R.id.add_book_placeholder);

        //listener for the book image
        addBookPlaceholder.setOnClickListener(v -> openImagePicker());

        //listener for the save book button
        findViewById(R.id.save_button).setOnClickListener(v -> saveBook());

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
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Evita loops infinitos ao formatar
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

                                        Glide.with(AddBook.this)
                                                .load(imageUrl)
                                                .transform(
                                                        new CircleCrop(),
                                                        new CircularBorderTransformation(AddBook.this, 1, Color.parseColor("#91918E"))
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

    private void openImagePicker() {
        //open gallery
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, 1);
    }

    //using the selected image
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == RESULT_OK && data != null) {
            imageUri = data.getData(); //assigning uri to the class variable
            if (imageUri != null) {
                addBookPlaceholder.setImageURI(imageUri); //replaces placeholder with the selected image
                //using glide to round corners
                Glide.with(this)
                        .load(imageUri)
                        .transform(new RoundedCorners(50))
                        .into(addBookPlaceholder);
            } else {
                Toast.makeText(this, "No image selected.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void saveBook() {
        startProgressBar();

        //getting book data
        String title = bookTitleField.getText().toString().trim();
        String author = bookAuthorField.getText().toString().trim();
        String publisher = bookPublisherField.getText().toString().trim();
        String releaseDate = bookReleaseDateField.getText().toString().trim();
        String isbn = bookIsbnField.getText().toString().trim();
        String description = bookDescriptionField.getText().toString().trim();
        String genre = bookGenreSpinner.getSelectedItem().toString();
        String readingStatus = getReadingStatus();
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
                // Remove "R$" e substitui vírgulas por pontos antes de converter
                String cleanPriceString = priceString.replace("R$", "").replace(",", ".").trim();

                // Verifica se o preço limpo não está vazio antes de tentar a conversão
                if (cleanPriceString.isEmpty()) {
                    throw new NumberFormatException("Price is empty or invalid.");
                }

                price = Double.parseDouble(cleanPriceString); // Converte a string limpa para double

                // Formata o preço para garantir que sempre tenha duas casas decimais
                price = Math.round(price * 100.0) / 100.0;  // Arredonda para duas casas decimais

                // Formatação para garantir duas casas decimais
                DecimalFormat decimalFormat = new DecimalFormat("0.00");
                price = Double.parseDouble(decimalFormat.format(price));  // Formata com 2 casas decimais

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

        //verify required fields
        if (title.isEmpty() || author.isEmpty()) {
            Toast.makeText(this, "Please fill in all required fields.", Toast.LENGTH_SHORT).show();
            finishProgressBar();
            return;
        }

        //create a data map with the book info
        Map<String, Object> bookMap = new HashMap<>();
        bookMap.put("title", title);
        bookMap.put("author", author);
        bookMap.put("publisher", publisher);
        bookMap.put("releaseDate", releaseDate);
        bookMap.put("isbn", isbn);
        bookMap.put("description", description);
        bookMap.put("genre", genre);
        bookMap.put("readingStatus", readingStatus);
        bookMap.put("userId", mAuth.getCurrentUser().getUid());
        bookMap.put("price", price);
        bookMap.put("pageNumber", pageNumber);
        bookMap.put("starRate", starRate);
        bookMap.put("quantity", quantity);
        //initialize interestedusers empty
        bookMap.put("interestedUsers", new ArrayList<String>());

        //verifies if an image was selected
        if (imageUri != null) {
            uploadImageToFirebase(imageUri, bookMap); //call for image upload
        } else {
            saveBookToFirestore(bookMap); //if there is no image, upload directly
        }
    }

    private String getReadingStatus() {
        //get reading status from radiogroup
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
        return readingStatus;
    }

    private void uploadImageToFirebase(Uri imageUri, Map<String, Object> bookMap) {
        startProgressBar();

        StorageReference storageRef = storage.getReference();
        String fileName = System.currentTimeMillis() + ".jpg"; //unique name for the image
        StorageReference imageRef = storageRef.child("book_images/" + fileName);

        imageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        String imageUrl = uri.toString();
                        bookMap.put("imageUrl", imageUrl);
                        saveBookToFirestore(bookMap); //call firestore upload method
                        finishProgressBar();
                    });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(AddBook.this, "Error uploading image", Toast.LENGTH_SHORT).show();
                    finishProgressBar();
                });
    }

    private void saveBookToFirestore(Map<String, Object> bookMap) {
        startProgressBar();

        db.collection("books")
                .add(bookMap)  //create an unique id automatically
                .addOnSuccessListener(documentReference -> {
                    String generatedId = documentReference.getId();
                    bookMap.put("id", generatedId);
                    Book book = new Book();
                    book.setId(generatedId);
                    finishProgressBar();

                    Toast.makeText(this, "Book added successfully!", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error adding book.", Toast.LENGTH_SHORT).show();
                    finishProgressBar();
                });
    }

    //if user clicks user logo, it opens the account page
    public void openAccount(View view) {
        Intent myIntent = new Intent(getApplicationContext(), Account.class);
        startActivity(myIntent);
    }

    //if user clicks retroreads logo, it opens the catalog page
    public void openCatalog(View view) {
        Intent myIntent = new Intent(getApplicationContext(), Catalog.class);
        startActivity(myIntent);
    }
}