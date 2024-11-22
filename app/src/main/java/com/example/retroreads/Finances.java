package com.example.retroreads;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;

public class Finances extends AppCompatActivity {
    private List<Book> bookList;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private TextView totalStockValueTextView;
    private TextView cashFlowTextView;
    private EditText valueField;
    private Button plusButton, minusButton;
    private ImageView accountIcon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_finances);

        //setting up user interface
        getWindow().setStatusBarColor(getResources().getColor(android.R.color.white));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        finishProgressBar();

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_finances);

        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                if (item.getItemId() == R.id.nav_for_sale) {
                    Intent intent = new Intent(Finances.this, ForSale.class);
                    startActivity(intent);
                    overridePendingTransition(0, 0);
                    return true;
                } else if (item.getItemId() == R.id.nav_shelf) {
                    Intent intent = new Intent(Finances.this, Shelf.class);
                    startActivity(intent);
                    overridePendingTransition(0, 0);
                    return true;
                } else if (item.getItemId() == R.id.nav_interests) {
                    Intent intent = new Intent(Finances.this, Interests.class);
                    startActivity(intent);
                    overridePendingTransition(0, 0);
                    return true;
                } else if (item.getItemId() == R.id.nav_catalog) {
                    Intent intent = new Intent(Finances.this, Catalog.class);
                    startActivity(intent);
                    overridePendingTransition(0, 0);
                    return true;
                }
                return false;
            }
        });

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        bookList = new ArrayList<>();
        totalStockValueTextView = findViewById(R.id.total_stock_value);  // Certifique-se de que o ID corresponde ao TextView no layout
        cashFlowTextView = findViewById(R.id.cash_flow);
        valueField = findViewById(R.id.value_field);
        plusButton = findViewById(R.id.plus_button);
        minusButton = findViewById(R.id.minus_button);

        // Chame o método para calcular e exibir o valor total em estoque
        stockValue();
        cashFlow();

        // Adiciona o valor ao cash flow ao clicar no botão de adição
        plusButton.setOnClickListener(v -> updateCashFlow(true));

        // Subtrai o valor do cash flow ao clicar no botão de subtração
        minusButton.setOnClickListener(v -> updateCashFlow(false));

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

                                        Glide.with(Finances.this)
                                                .load(imageUrl)
                                                .transform(
                                                        new CircleCrop(),
                                                        new CircularBorderTransformation(Finances.this, 1, Color.parseColor("#91918E"))
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

    public void cashFlow() {
        startProgressBar();
        // Usa o UID diretamente como ID do documento na coleção "users"
        db.collection("users")
                .document(mAuth.getCurrentUser().getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Verifica se o campo 'finances' existe e é um número
                        if (documentSnapshot.contains("finances")) {
                            Double cashFlowValue = documentSnapshot.getDouble("finances");
                            if (cashFlowValue != null) {
                                // Exibe o valor em um TextView formatado como moeda
                                cashFlowTextView.setText(String.format("R$ %.2f", cashFlowValue));
                            } else {
                                cashFlowTextView.setText("Erro ao recuperar o valor");
                            }
                        } else {
                            cashFlowTextView.setText("Campo 'finances' não encontrado");
                        }
                    } else {
                        cashFlowTextView.setText("Usuário não encontrado");
                    }
                    finishProgressBar();
                })
                .addOnFailureListener(e -> {
                    // Manipula erros, se houver
                    cashFlowTextView.setText("Erro ao calcular o valor do fluxo de caixa");
                });
    }

    // Atualiza o cash flow com base na operação (adição/subtração)
    private void updateCashFlow(boolean isAddition) {
        startProgressBar();

        String valueText = valueField.getText().toString();

        if (!valueText.isEmpty()) {
            double value = Double.parseDouble(valueText);

            db.collection("users")
                    .document(mAuth.getCurrentUser().getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Double currentCashFlow = documentSnapshot.getDouble("finances");
                            if (currentCashFlow == null) {
                                currentCashFlow = 0.0;
                            }

                            // Calcula o novo valor com base no botão clicado
                            double updatedCashFlow = isAddition ? currentCashFlow + value : currentCashFlow - value;

                            // Atualiza o campo 'finances' no Firestore
                            db.collection("users")
                                    .document(mAuth.getCurrentUser().getUid())
                                    .update("finances", updatedCashFlow)
                                    .addOnSuccessListener(aVoid -> {
                                        cashFlowTextView.setText(String.format("R$ %.2f", updatedCashFlow));
                                        valueField.setText("");  // Limpa o campo após a operação
                                    })
                                    .addOnFailureListener(e -> cashFlowTextView.setText("Erro ao atualizar o cash flow"));
                        }
                        finishProgressBar();
                    });
        } else {
            valueField.setError("Digite um valor");
        }
    }

    public void stockValue() {
        db.collection("books")
                .whereEqualTo("userId", mAuth.getCurrentUser().getUid())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    bookList.clear();  // Limpa qualquer conteúdo anterior
                    double totalStockValue = 0;

                    for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                        Book book = documentSnapshot.toObject(Book.class);
                        if (book != null) {
                            book.setId(documentSnapshot.getId()); // Define o ID do livro
                            bookList.add(book);
                            // Calcula o valor total do estoque acumulando o valor de cada livro
                            totalStockValue += book.getPrice() * book.getQuantity();
                        }
                    }

                    // Atualiza o TextView com o valor total do estoque
                    totalStockValueTextView.setText(String.format("R$ %.2f", totalStockValue));
                })
                .addOnFailureListener(e -> {
                    // Manipule erros, se houver
                    totalStockValueTextView.setText("Erro ao calcular o valor do estoque");
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