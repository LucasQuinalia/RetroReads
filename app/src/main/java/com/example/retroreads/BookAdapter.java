package com.example.retroreads;

import android.content.Context;
import android.content.Intent;
import android.media.Image;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class BookAdapter extends RecyclerView.Adapter<BookAdapter.BookViewHolder> {
    private List<Book> bookList;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private int viewType;
    private OnBookDeleteListener onBookDeleteListener;
    private OnBookClickListener onBookClickListener;

    public interface OnBookClickListener {
        void onBookClick(int position);
    }

    public interface OnBookDeleteListener {
        void onBookDelete(String bookId);
    }

    public static final int TYPE_CATALOG = 0;
    public static final int TYPE_SHELF = 1;
    public static final int TYPE_INTERESTED = 2;
    public static final int TYPE_FOR_SALE = 3;

    public BookAdapter(List<Book> bookList, int viewType, OnBookClickListener onBookClickListener, OnBookDeleteListener onBookDeleteListener) {
        if (bookList == null) {
            this.bookList = new ArrayList<>();
        } else {
            this.bookList = bookList;
        }

        this.bookList = bookList;
        this.viewType = viewType;
        this.onBookClickListener = onBookClickListener;
        this.onBookDeleteListener = onBookDeleteListener;
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    @Override
    public int getItemViewType(int position) {
        return viewType;
    }

    @Override
    public BookViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        int layoutId;
        switch (viewType) {
            case TYPE_CATALOG: layoutId = R.layout.item_book; break;
            case TYPE_SHELF: layoutId = R.layout.shelf_book; break;
            case TYPE_INTERESTED: layoutId = R.layout.interested_book; break;
            case TYPE_FOR_SALE: layoutId = R.layout.for_sale_book; break;
            default: layoutId = R.layout.item_book; break;
        }

        View view = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
        return new BookViewHolder(view, onBookClickListener);
    }

    @Override
    public void onBindViewHolder(BookViewHolder holder, int position) {
        Book book = bookList.get(position);
        Context context = holder.itemView.getContext();

        holder.itemView.setOnClickListener(v -> {
            if (onBookClickListener != null) {
                onBookClickListener.onBookClick(position);
            }
        });

        // Condicional para permitir o clique apenas nos layouts 'interested' e 'catalog'
        if (viewType == TYPE_INTERESTED || viewType == TYPE_CATALOG) {
            holder.bookCover.setOnClickListener(v -> {
                Intent intent = new Intent(context, OpenBook.class);
                intent.putExtra("BOOK_ID", book.getId()); // Envia o ID do livro para a OpenBook Activity
                intent.putExtra("BOOK_USER_ID", book.getUserId()); // Envia o userId do dono
                context.startActivity(intent);
            });
        } else {
            holder.bookCover.setOnClickListener(null); // Remove o clique em outros tipos de layout
        }

        String bookId = book.getId();
        Glide.with(context)
                .load(book.getImageUrl())
                .apply(RequestOptions.bitmapTransform(new RoundedCorners(15)))
                .placeholder(R.drawable.default_book_cover)
                .error(R.drawable.default_book_cover)
                .into(holder.bookCover);

        holder.bookTitle.setText(book.getTitle());
        holder.bookAuthor.setText(book.getAuthor());

        if (holder.interestedPeople != null) {
            DocumentReference bookRef = db.collection("books").document(bookId);

            // Obtém a lista atual de usuários interessados e conta o número
            bookRef.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    List<String> interestedUsers = (List<String>) documentSnapshot.get("interestedUsers");
                    int interestedCount = (interestedUsers != null) ? interestedUsers.size() : 0;

                    // Atualiza o TextView com o número de interessados
                    holder.interestedPeople.setText(String.valueOf(interestedCount));
                }
            }).addOnFailureListener(e -> {
                holder.interestedPeople.setText("0"); // Define como 0 caso haja erro
            });
        }

        if (holder.ownerName != null || holder.ownerPhone != null) {
            // Obtenha o ID do proprietário do livro
            String ownerId = book.getUserId();

            // Verifique se o proprietário não é o usuário atual
            if (!ownerId.equals(mAuth.getCurrentUser().getUid())) {
                // Busca os detalhes do proprietário no Firestore
                db.collection("users").document(ownerId).get()
                        .addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists()) {
                                String ownerName = documentSnapshot.getString("name");
                                String ownerPhone = documentSnapshot.getString("phoneNumber");

                                holder.ownerName.setText(ownerName);
                                holder.ownerPhone.setText(ownerPhone);
                            }
                        })
                        .addOnFailureListener(e -> {

                        });
            } else {
                Toast.makeText(context, "This book is yours!", Toast.LENGTH_SHORT).show();
            }
        }

        if (holder.bookPrice != null) {
            holder.bookPrice.setText(String.valueOf(book.getPrice()));
        }

        if (holder.bookQuantity != null) {
            holder.bookQuantity.setText(String.valueOf(book.getQuantity()));
        }

        if (holder.bookStarRate != null) {
            holder.bookStarRate.setText(String.valueOf(book.getStarRate()));
        }

        if (holder.bookReadingStatus != null) {
            if ("want_to_sell".equals(book.getReadingStatus())) {
                holder.bookReadingStatus.setText("Want to sell");
                holder.bookReadingStatus.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.red));
                holder.bookReadingStatus.setBackgroundResource(R.drawable.button_default_red);
            } else if ("want_to_read".equals(book.getReadingStatus())) {
                holder.bookReadingStatus.setText("Want to read");
                holder.bookReadingStatus.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.gray));
                holder.bookReadingStatus.setBackgroundResource(R.drawable.button_default_gray);
            } else if ("currently_reading".equals(book.getReadingStatus())) {
                holder.bookReadingStatus.setText("Currently reading");
                holder.bookReadingStatus.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.yellow));
                holder.bookReadingStatus.setBackgroundResource(R.drawable.background_currently_reading);
            } else {
                holder.bookReadingStatus.setText("Already read");
                holder.bookReadingStatus.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.blue));
                holder.bookReadingStatus.setBackgroundResource(R.drawable.background_already_read);
            }
        }

        if (holder.deleteButton != null) {
            holder.deleteButton.setOnClickListener(v -> {
                if (onBookDeleteListener != null && bookId != null) {
                    onBookDeleteListener.onBookDelete(bookId);
                }
            });
        }

        if (holder.interestButton != null) {
            holder.interestButton.setOnClickListener(v -> {
                String ownerId = book.getUserId();

                if (!ownerId.equals(mAuth.getCurrentUser().getUid())) {
                    db.collection("users").document(ownerId).get()
                            .addOnSuccessListener(documentSnapshot -> {
                                if (documentSnapshot.exists()) {
                                    String ownerName = documentSnapshot.getString("name");
                                    String ownerPhone = documentSnapshot.getString("phoneNumber");

                                    // Inflate o layout customizado
                                    LayoutInflater inflater = LayoutInflater.from(context);
                                    View dialogView = inflater.inflate(R.layout.interest_register_custom_dialog, null);

                                    // Define o fundo para o layout interno
                                    dialogView.setBackgroundResource(R.drawable.input_field);

                                    TextView dialogName = dialogView.findViewById(R.id.dialog_name);
                                    TextView dialogPhone = dialogView.findViewById(R.id.dialog_phone);
                                    dialogName.setText(ownerName);
                                    dialogPhone.setText(ownerPhone);

                                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                                    builder.setView(dialogView);

                                    Button dialogButton = dialogView.findViewById(R.id.dialog_button);
                                    AlertDialog alertDialog = builder.create();

                                    // Aplica o fundo com bordas arredondadas no próprio AlertDialog
                                    alertDialog.getWindow().setBackgroundDrawableResource(R.drawable.input_field);

                                    dialogButton.setOnClickListener(btn -> alertDialog.dismiss());

                                    alertDialog.show();

                                    // Adiciona o usuário à lista de interessados
                                    registerInterest(book.getId(), mAuth.getCurrentUser().getUid());
                                }
                            })
                            .addOnFailureListener(e -> {

                            });
                } else {
                    Toast.makeText(context, "This book is yours!", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (holder.deleteInterestButton != null) {
            holder.deleteInterestButton.setOnClickListener(v -> deleteInterest(bookId, mAuth.getUid(), context));
        }
    }

    private void registerInterest(String bookId, String interestedUserId) {
        // Referência para o documento do livro
        DocumentReference bookRef = db.collection("books").document(bookId);

        // Adiciona o ID do usuário interessado ao campo "interestedUsers"
        bookRef.update("interestedUsers", FieldValue.arrayUnion(interestedUserId));
    }

    private void deleteInterest(String bookId, String interestedUserId, Context context) {
        if (bookId != null && !bookId.isEmpty()) {
            db.collection("books").document(bookId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String bookName = documentSnapshot.getString("title");

                            // Inflate o layout customizado
                            LayoutInflater inflater = LayoutInflater.from(context);
                            View dialogView = inflater.inflate(R.layout.interest_delete_custom_dialog, null);

                            // Define o fundo para o layout interno
                            dialogView.setBackgroundResource(R.drawable.input_field);

                            TextView dialogBookName = dialogView.findViewById(R.id.dialog_book_name);
                            dialogBookName.setText(bookName);

                            AlertDialog.Builder builder = new AlertDialog.Builder(context);
                            builder.setView(dialogView);

                            Button dialogYesButton = dialogView.findViewById(R.id.dialog_yes_button);
                            Button dialogNoButton = dialogView.findViewById(R.id.dialog_no_button);
                            AlertDialog alertDialog = builder.create();

                            // Aplica o fundo com bordas arredondadas no próprio AlertDialog
                            alertDialog.getWindow().setBackgroundDrawableResource(R.drawable.input_field);

                            dialogYesButton.setOnClickListener(btn -> {
                                // Executa a lógica de remoção apenas ao clicar no botão "Yes"
                                DocumentReference bookRef = db.collection("books").document(bookId);
                                bookRef.update("interestedUsers", FieldValue.arrayRemove(interestedUserId))
                                        .addOnSuccessListener(aVoid -> {
                                            // Atualiza a lista após a remoção
                                            loadInterestedBooks(interestedUserId);
                                            Toast.makeText(context, "Interest successfully deleted!", Toast.LENGTH_SHORT).show();
                                        })
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(context, "Error deleting interest!", Toast.LENGTH_SHORT).show();
                                        });
                                alertDialog.dismiss();
                            });

                            dialogNoButton.setOnClickListener(btn -> alertDialog.dismiss());

                            alertDialog.show();
                        }
                    });
        }
    }


    @Override
    public int getItemCount() {
        return bookList.size();
    }

    public static class BookViewHolder extends RecyclerView.ViewHolder {
        TextView bookTitle, bookAuthor, bookPrice, bookReadingStatus, bookStarRate, ownerName, ownerPhone, interestedPeople, bookQuantity;
        ImageView bookCover;
        Button interestButton;
        Button deleteInterestButton;
        ImageView deleteButton;
        ImageView editButton;
        OnBookClickListener onBookClickListener;

        public BookViewHolder(View itemView, OnBookClickListener onBookClickListener) {
            super(itemView);
            this.onBookClickListener = onBookClickListener;

            bookTitle = itemView.findViewById(R.id.book_title);
            bookAuthor = itemView.findViewById(R.id.book_author);
            bookPrice = itemView.findViewById(R.id.book_price);
            bookQuantity = itemView.findViewById(R.id.stock_number);
            bookReadingStatus = itemView.findViewById(R.id.book_reading_status);
            bookCover = itemView.findViewById(R.id.book_cover);
            bookStarRate = itemView.findViewById(R.id.book_star_rate);
            ownerName = itemView.findViewById(R.id.owner_name);
            ownerPhone = itemView.findViewById(R.id.owner_phone);
            interestedPeople = itemView.findViewById(R.id.interested_people);
            interestButton = itemView.findViewById(R.id.interest_button);
            deleteInterestButton = itemView.findViewById(R.id.delete_interest_button);
            deleteButton = itemView.findViewById(R.id.delete_button);
            editButton = itemView.findViewById(R.id.edit_button);

            if (editButton != null) {
                editButton.setOnClickListener(v -> {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        if (onBookClickListener != null) {
                            onBookClickListener.onBookClick(position);
                        }
                    }
                });
            }
        }
    }

    public void loadInterestedBooks(String userId) {
        db.collection("books")
                .whereArrayContains("interestedUsers", userId)  // Filtra livros com o usuário interessado
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Book> interestedBooks = new ArrayList<>();
                    for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                        Book book = documentSnapshot.toObject(Book.class);
                        book.setId(documentSnapshot.getId()); // Define o ID do livro
                        interestedBooks.add(book);
                    }
                    // Atualiza a lista de livros e notifica o adapter
                    bookList.clear();
                    bookList.addAll(interestedBooks);
                    notifyDataSetChanged();  // Atualiza o RecyclerView
                })
                .addOnFailureListener(e -> {

                });
    }

    public void updateBookList(List<Book> newBookList) {
        if (newBookList == null) {
            return;
        }

        if (bookList == null) {
            bookList = new ArrayList<>();
        }

        this.bookList.clear();  // Limpa a lista antes de adicionar os novos itens
        this.bookList.addAll(newBookList);
        notifyDataSetChanged();  // Notifica o RecyclerView sobre a mudança
    }
}