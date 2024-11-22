package com.example.retroreads;

import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.SpannableString;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;

import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class Login extends AppCompatActivity {
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        setContentView(R.layout.activity_login);

        getWindow().setStatusBarColor(getResources().getColor(android.R.color.white));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        finishProgressBar();

        Button button_register = findViewById(R.id.register_button);
        Button button_login = findViewById(R.id.login_button);

        button_login.setOnClickListener(v -> loginUser());

        String text = "Register";
        SpannableString spannable = new SpannableString(text);

        // Aplica negrito
        spannable.setSpan(new StyleSpan(Typeface.BOLD), 0, text.length(), 0);
        // Aplica sublinhado
        spannable.setSpan(new UnderlineSpan(), 0, text.length(), 0);

        button_register.setText(spannable);

        mAuth = FirebaseAuth.getInstance();

        TextInputLayout textInputLayout = findViewById(R.id.password_field_layout);
        Drawable eyeIcon = textInputLayout.getEndIconDrawable();

        if (eyeIcon != null) {
            eyeIcon.setColorFilter(ContextCompat.getColor(this, R.color.green), PorterDuff.Mode.SRC_IN);
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

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            startProgressBar();
            Intent intent = new Intent(getApplicationContext(), Catalog.class);
            startActivity(intent);
            finish();
            finishProgressBar();
        }
    }

    private void loginUser() {
        startProgressBar();
        String email = ((EditText) findViewById(R.id.email_field)).getText().toString().trim();
        String password = ((EditText) findViewById(R.id.password_field)).getText().toString().trim();

        // Verifica se os campos estão preenchidos
        if (email.isEmpty()) {
            Toast.makeText(this, "Por favor, insira o e-mail.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.isEmpty()) {
            Toast.makeText(this, "Por favor, insira a senha.", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Login bem-sucedido", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(getApplicationContext(), Catalog.class);
                        startActivity(intent);
                        finishProgressBar();
                        finish();
                    } else {
                        Toast.makeText(this, "Erro ao fazer login: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }


    public void openRegister(View view) {
        Intent myIntent = new Intent(getApplicationContext(), Register.class);
        startActivity(myIntent);
    }

}
