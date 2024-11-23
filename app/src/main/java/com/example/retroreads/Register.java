package com.example.retroreads;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.util.HashMap;
import java.util.Map;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

public class Register extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private ImageView userPicPlaceholder;
    private Uri imageUri;
    private FirebaseStorage storage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        setContentView(R.layout.activity_register);

        getWindow().setStatusBarColor(getResources().getColor(android.R.color.white));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        finishProgressBar();

        Button button = findViewById(R.id.login_button);
        String text = "Login";
        SpannableString spannable = new SpannableString(text);

        // Aplica negrito
        spannable.setSpan(new StyleSpan(Typeface.BOLD), 0, text.length(), 0);
        // Aplica sublinhado
        spannable.setSpan(new UnderlineSpan(), 0, text.length(), 0);

        button.setText(spannable);

        // Inicializar FirebaseAuth
        mAuth = FirebaseAuth.getInstance();

        // Inicializar FirebaseStorage
        storage = FirebaseStorage.getInstance();

        // Exemplo de botão de cadastro
        Button register_button = findViewById(R.id.register_button);
        register_button.setOnClickListener(v -> registerUser());

        TextInputLayout textInputLayoutPassword = findViewById(R.id.password_field_layout);
        TextInputLayout textInputLayoutRepeatPassword = findViewById(R.id.repeat_password_field_layout);
        Drawable eyeIconPassword = textInputLayoutPassword.getEndIconDrawable();
        Drawable eyeIconRepeatPassword = textInputLayoutRepeatPassword.getEndIconDrawable();

        if (eyeIconPassword != null) {
            eyeIconPassword.setColorFilter(ContextCompat.getColor(this, R.color.green), PorterDuff.Mode.SRC_IN);
            eyeIconRepeatPassword.setColorFilter(ContextCompat.getColor(this, R.color.green), PorterDuff.Mode.SRC_IN);
        }

        userPicPlaceholder = findViewById(R.id.user_pic_placeholder);

        //listener for the book image
        userPicPlaceholder.setOnClickListener(v -> openImagePicker());

        EditText stateField = findViewById(R.id.state_field);

        stateField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() > 2) {
                    stateField.setError("Apenas 2 caracteres são permitidos!");
                    s.delete(2, s.length()); // Remove caracteres excedentes
                }
            }
        });

        EditText cepField = findViewById(R.id.cep_field);

        cepField.addTextChangedListener(new TextWatcher() {
            private boolean isUpdating = false;
            private final String mask = "#####-###";

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (isUpdating) {
                    isUpdating = false;
                    return;
                }

                String unmasked = s.toString().replaceAll("[^\\d]", ""); // Remove caracteres não numéricos

                StringBuilder masked = new StringBuilder();
                int i = 0;

                for (char m : mask.toCharArray()) {
                    if (m != '#' && unmasked.length() > i) {
                        masked.append(m);
                    } else if (i < unmasked.length()) {
                        masked.append(unmasked.charAt(i));
                        i++;
                    }
                }

                isUpdating = true;
                cepField.setText(masked.toString());
                cepField.setSelection(masked.length()); // Move o cursor para o final
            }
        });

        EditText phoneField = findViewById(R.id.phone_number_field);

        phoneField.addTextChangedListener(new TextWatcher() {
            private boolean isUpdating = false;
            private final String maskWithDDD = "(##) #####-####";

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (isUpdating) {
                    isUpdating = false;
                    return;
                }

                String unmasked = s.toString().replaceAll("[^\\d]", ""); // Remove caracteres não numéricos

                // Limita a quantidade de números a 11 (com DDD)
                if (unmasked.length() > 11) {
                    unmasked = unmasked.substring(0, 11);
                }

                // Aplica a máscara ao número
                StringBuilder masked = new StringBuilder();
                int i = 0;

                // Aplica a máscara (##) #####-####
                for (char m : maskWithDDD.toCharArray()) {
                    if (m != '#' && i < unmasked.length()) {
                        masked.append(m);
                    } else if (i < unmasked.length()) {
                        masked.append(unmasked.charAt(i));
                        i++;
                    }
                }

                // Atualiza o campo com a máscara formatada
                isUpdating = true;
                phoneField.setText(masked.toString());
                phoneField.setSelection(masked.length()); // Move o cursor para o final
            }
        });

        EditText cpfField = findViewById(R.id.cpf_field);

        cpfField.addTextChangedListener(new TextWatcher() {
            private boolean isUpdating = false;
            private final String cpfMask = "###.###.###-##";  // Formato CPF
            private final String cnpjMask = "##.###.###/####-##";  // Formato CNPJ

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (isUpdating) {
                    isUpdating = false;
                    return;
                }

                String unmasked = s.toString().replaceAll("[^\\d]", ""); // Remove todos os caracteres não numéricos

                // Verifica se é CPF ou CNPJ, e aplica a máscara apropriada
                String mask = unmasked.length() <= 11 ? cpfMask : cnpjMask;

                StringBuilder masked = new StringBuilder();
                int i = 0;

                // Aplica a máscara no número
                for (char m : mask.toCharArray()) {
                    if (m != '#' && i < unmasked.length()) {
                        masked.append(m);
                    } else if (i < unmasked.length()) {
                        masked.append(unmasked.charAt(i));
                        i++;
                    }
                }

                // Atualiza o campo com a máscara formatada
                isUpdating = true;
                cpfField.setText(masked.toString());
                cpfField.setSelection(masked.length()); // Move o cursor para o final
            }
        });
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

    public void registerUser() {
        startProgressBar();

        // Referências aos campos
        RadioGroup radioUserType = findViewById(R.id.radio_user_type);
        RadioButton cpfRadio = findViewById(R.id.cpf_radio);
        RadioButton cnpjRadio = findViewById(R.id.cnpj_radio);
        EditText nameField = findViewById(R.id.name_field);
        EditText cpfField = findViewById(R.id.cpf_field);
        EditText phoneNumberField = findViewById(R.id.phone_number_field);
        EditText streetField = findViewById(R.id.street_field);
        EditText addressNumberField = findViewById(R.id.address_number_field);
        EditText complementField = findViewById(R.id.complement_field);
        EditText districtField = findViewById(R.id.district_field);
        EditText cepField = findViewById(R.id.cep_field);
        EditText stateField = findViewById(R.id.state_field);
        EditText cityField = findViewById(R.id.city_field);
        EditText emailField = findViewById(R.id.email_field);
        EditText passwordField = findViewById(R.id.password_field);
        EditText repeatPasswordField = findViewById(R.id.repeat_password_field);

        // Determina o tipo de usuário (0 para CPF, 1 para CNPJ)
        int userType = cpfRadio.isChecked() ? 0 : 1;

        // Captura os valores dos campos
        String name = nameField.getText().toString().trim();
        String cpfOrCnpj = cpfField.getText().toString().trim();
        String phoneNumber = phoneNumberField.getText().toString().trim();
        String street = streetField.getText().toString().trim();
        String addressNumber = addressNumberField.getText().toString().trim();
        String complement = complementField.getText().toString().trim();
        String district = districtField.getText().toString().trim();
        String cep = cepField.getText().toString().trim();
        String state = stateField.getText().toString().trim();
        String city = cityField.getText().toString().trim();
        String email = emailField.getText().toString().trim();
        String password = passwordField.getText().toString().trim();
        String repeatPassword = repeatPasswordField.getText().toString().trim();

        // Verificação de campos obrigatórios
        if (name.isEmpty() || cpfOrCnpj.isEmpty() || phoneNumber.isEmpty() || email.isEmpty() || password.isEmpty() || repeatPassword.isEmpty()) {
            Toast.makeText(this, "Please fill in all the required fields.", Toast.LENGTH_SHORT).show();
            return; // Impede a execução do restante do código
        }

        if (!password.equals(repeatPassword)) {
            Toast.makeText(this, "The passwords are not equal.", Toast.LENGTH_SHORT).show();
            return; // Impede a execução do restante do código
        }

        // Configuração de autenticação e armazenamento no Firebase
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Criar um novo usuário com email e senha
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Obtém o ID do usuário recém-criado
                        String userId = mAuth.getCurrentUser().getUid();

                        // Cria o objeto de dados do usuário
                        Map<String, Object> user = new HashMap<>();
                        user.put("userType", userType);
                        user.put("name", name);
                        user.put("cpfOrCnpj", cpfOrCnpj);
                        user.put("phoneNumber", phoneNumber);
                        user.put("street", street.isEmpty() ? null : street);
                        user.put("addressNumber", addressNumber.isEmpty() ? null : addressNumber);
                        user.put("complement", complement.isEmpty() ? null : complement);
                        user.put("district", district.isEmpty() ? null : district);
                        user.put("cep", cep.isEmpty() ? null : cep);
                        user.put("state", state.isEmpty() ? null : state);
                        user.put("city", city.isEmpty() ? null : city);
                        user.put("finances", 0);

                        //verifies if an image was selected
                        if (imageUri != null) {
                            uploadImageToFirebase(imageUri, user, userId); // Passando userId para a função
                        } else {
                            finishProgressBar();
                            startActivity(new Intent(this, Catalog.class));
                        }

                        finishProgressBar();
                        startActivity(new Intent(this, Catalog.class));

                        // Salva os dados do usuário no Firestore
                        db.collection("users").document(userId).set(user)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "You have been successfully registered!", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "Error saving data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        Toast.makeText(this, "Error creating user: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
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
                                    Toast.makeText(Register.this, "User registered successfully!", Toast.LENGTH_SHORT).show();
                                    startActivity(new Intent(Register.this, Catalog.class));
                                })
                                .addOnFailureListener(e -> {
                                    finishProgressBar();
                                    Toast.makeText(Register.this, "Error saving image URL: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(Register.this, "Error uploading image", Toast.LENGTH_SHORT).show();
                    finishProgressBar();
                });
    }

    public void openLogin(View view) {
        Intent myIntent = new Intent(getApplicationContext(), Login.class);
        startActivity(myIntent);
    }
}