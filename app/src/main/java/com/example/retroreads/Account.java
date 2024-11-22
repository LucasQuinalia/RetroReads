package com.example.retroreads;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

public class Account extends AppCompatActivity {
    //declaring firebase variables
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    //declaring variables
    private EditText nameField, cpfField, phoneNumberField, streetField, addressNumberField, complementField, districtField, cepField, stateField, cityField, emailField;
    private ImageView userPicPlaceholder, accountIcon;
    private Uri imageUri;
    private FirebaseStorage storage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);

        //setting up user interface
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        getWindow().setStatusBarColor(getResources().getColor(android.R.color.white));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        //initializing bottom nav
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                if (item.getItemId() == R.id.nav_for_sale) {
                    Intent intent = new Intent(Account.this, ForSale.class);
                    startActivity(intent);
                    overridePendingTransition(0, 0);
                    return true;
                } else if (item.getItemId() == R.id.nav_shelf) {
                    Intent intent = new Intent(Account.this, Shelf.class);
                    startActivity(intent);
                    overridePendingTransition(0, 0);
                    return true;
                } else if (item.getItemId() == R.id.nav_interests) {
                    Intent intent = new Intent(Account.this, Interests.class);
                    startActivity(intent);
                    overridePendingTransition(0, 0);
                    return true;
                } else if (item.getItemId() == R.id.nav_catalog) {
                    Intent intent = new Intent(Account.this, Catalog.class);
                    startActivity(intent);
                    overridePendingTransition(0, 0);
                    return true;
                } else if (item.getItemId() == R.id.nav_finances) {
                    Intent intent = new Intent(Account.this, Finances.class);
                    startActivity(intent);
                    overridePendingTransition(0, 0);
                    return true;
                }
                return false;
            }
        });

        //initializing log out button
        Button logoutButton = findViewById(R.id.log_out_button);
        logoutButton.setOnClickListener(v -> logoutUser());

        //setting up firebase
        mAuth = FirebaseAuth.getInstance();
        storage = FirebaseStorage.getInstance();
        db = FirebaseFirestore.getInstance();

        //setting up fields
        nameField = findViewById(R.id.name_field);
        cpfField = findViewById(R.id.cpf_field);
        phoneNumberField = findViewById(R.id.phone_number_field);
        streetField = findViewById(R.id.street_field);
        addressNumberField = findViewById(R.id.address_number_field);
        complementField = findViewById(R.id.complement_field);
        districtField = findViewById(R.id.district_field);
        cepField = findViewById(R.id.cep_field);
        stateField = findViewById(R.id.state_field);
        cityField = findViewById(R.id.city_field);
        emailField = findViewById(R.id.email_field);
        userPicPlaceholder = findViewById(R.id.user_pic_placeholder);

        accountIcon = findViewById(R.id.account);

        nameField.setEnabled(false);
        cpfField.setEnabled(false);
        phoneNumberField.setEnabled(true);
        streetField.setEnabled(true);
        addressNumberField.setEnabled(true);
        complementField.setEnabled(true);
        districtField.setEnabled(true);
        cepField.setEnabled(true);
        stateField.setEnabled(true);
        cityField.setEnabled(true);
        emailField.setEnabled(false);

        loadUserData();

        //listener for the book image
        userPicPlaceholder.setOnClickListener(v -> openImagePicker());
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
                userPicPlaceholder.setImageURI(imageUri); //replaces placeholder with the selected image
                Glide.with(this)
                        .load(imageUri)
                        .transform(
                                new CircleCrop(),
                                new CircularBorderTransformation(this, 2, Color.parseColor("#91918E"))
                        )
                        .into(userPicPlaceholder);
            } else {
                Toast.makeText(this, "No image selected.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void loadUserData() {
        startProgressBar();

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
                                //getting data from collection
                                String name = document.getString("name");
                                String cpf = document.getString("cpfOrCnpj");
                                String phone = document.getString("phoneNumber");
                                String street = document.getString("street");
                                String addressNumber = document.getString("addressNumber");
                                String complement = document.getString("complement");
                                String district = document.getString("district");
                                String cep = document.getString("cep");
                                String state = document.getString("state");
                                String city = document.getString("city");
                                String email = currentUser.getEmail();

                                finishProgressBar();

                                //exhibiting data on text views
                                nameField.setText(name);
                                cpfField.setText(cpf);
                                phoneNumberField.setText(phone);
                                streetField.setText(street);
                                addressNumberField.setText(addressNumber);
                                complementField.setText(complement);
                                districtField.setText(district);
                                cepField.setText(cep);
                                stateField.setText(state);
                                cityField.setText(city);
                                emailField.setText(email);

                                // Carregar imagem, se existir
                                String imageUrl = document.getString("imageUrl");
                                if (imageUrl != null) {
                                    Glide.with(this)
                                            .load(imageUrl)
                                            .transform(
                                                    new CircleCrop(),
                                                    new CircularBorderTransformation(this, 2, Color.parseColor("#91918E"))
                                            )
                                            .into(userPicPlaceholder);
                                }
                                if (imageUrl != null) {
                                    resizeImageView(accountIcon, 48, 48);
                                    setImageViewMargin(accountIcon, 14);

                                    Glide.with(getApplicationContext())
                                            .load(imageUrl)
                                            .transform(
                                                    new CircleCrop(),
                                                    new CircularBorderTransformation(this, 2, Color.parseColor("#91918E"))
                                            )
                                            .into(accountIcon);
                                }
                            } else {
                                Toast.makeText(this, "User not found.", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(this, "Error retrieving data: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            Toast.makeText(this, "User not authenticated.", Toast.LENGTH_SHORT).show();
            //redirecting user to login page
            startActivity(new Intent(this, Login.class));
            finish();
        }
    }

    public void saveUser(View view) {
        startProgressBar();

        String userId = mAuth.getCurrentUser().getUid();

        // Referências aos campos
        EditText phoneNumberField = findViewById(R.id.phone_number_field);
        EditText streetField = findViewById(R.id.street_field);
        EditText addressNumberField = findViewById(R.id.address_number_field);
        EditText complementField = findViewById(R.id.complement_field);
        EditText districtField = findViewById(R.id.district_field);
        EditText cepField = findViewById(R.id.cep_field);
        EditText stateField = findViewById(R.id.state_field);
        EditText cityField = findViewById(R.id.city_field);

        // Captura os valores dos campos
        String phoneNumber = phoneNumberField.getText().toString().trim();
        String street = streetField.getText().toString().trim();
        String addressNumber = addressNumberField.getText().toString().trim();
        String complement = complementField.getText().toString().trim();
        String district = districtField.getText().toString().trim();
        String cep = cepField.getText().toString().trim();
        String state = stateField.getText().toString().trim();
        String city = cityField.getText().toString().trim();

        // Verificação de campos obrigatórios
        if (phoneNumber.isEmpty()) {
            Toast.makeText(this, "Please fill in all the required fields.", Toast.LENGTH_SHORT).show();
            return; // Impede a execução do restante do código
        }

        // Configuração de autenticação e armazenamento no Firebase
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Cria o objeto de dados do usuário
        Map<String, Object> user = new HashMap<>();
        user.put("phoneNumber", phoneNumber);
        user.put("street", street.isEmpty() ? null : street);
        user.put("addressNumber", addressNumber.isEmpty() ? null : addressNumber);
        user.put("complement", complement.isEmpty() ? null : complement);
        user.put("district", district.isEmpty() ? null : district);
        user.put("cep", cep.isEmpty() ? null : cep);
        user.put("state", state.isEmpty() ? null : state);
        user.put("city", city.isEmpty() ? null : city);

        //verifies if an image was selected
        if (imageUri != null) {
            uploadImageToFirebase(imageUri, user, userId); // Chama o método de upload de imagem
        } else {
            db.collection("users").document(userId).set(user)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "User successfully updated!", Toast.LENGTH_SHORT).show();
                        finishProgressBar();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error saving data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        finishProgressBar();
                    });
        }
    }

    private void uploadImageToFirebase(Uri imageUri, Map<String, Object> userMap, String userId) {
        startProgressBar();

        StorageReference storageRef = storage.getReference();
        String fileName = System.currentTimeMillis() + ".jpg"; // Nome único para a imagem
        StorageReference imageRef = storageRef.child("user_images/" + fileName);

        imageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        String imageUrl = uri.toString();
                        userMap.put("imageUrl", imageUrl); // Atualiza o mapa de dados com a URL da imagem

                        // Atualiza o documento no Firestore com a nova URL
                        FirebaseFirestore db = FirebaseFirestore.getInstance();
                        db.collection("users").document(userId).update("imageUrl", imageUrl)
                                .addOnSuccessListener(aVoid -> {
                                    finishProgressBar();
                                    Toast.makeText(Account.this, "User updated successfully!", Toast.LENGTH_SHORT).show();
                                    startActivity(new Intent(Account.this, Catalog.class));
                                })
                                .addOnFailureListener(e -> {
                                    finishProgressBar();
                                    Toast.makeText(Account.this, "Error saving image URL: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(Account.this, "Error uploading image", Toast.LENGTH_SHORT).show();
                    finishProgressBar();
                });
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

    private void logoutUser() {
        mAuth.signOut();
        //redirecting user to login page
        Toast.makeText(this, "Log out successful!", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(getApplicationContext(), Login.class);
        startActivity(intent);
        finish();
    }

    //if user clicks retroreads logo, it opens the catalog page
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