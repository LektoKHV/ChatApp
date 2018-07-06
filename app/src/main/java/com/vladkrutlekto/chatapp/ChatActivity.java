package com.vladkrutlekto.chatapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ChatActivity extends AppCompatActivity implements View.OnClickListener {

    // Views
    @BindView(R.id.written_field) EditText writtenField;
    @BindView(R.id.add_button) ImageButton addButton;
    @BindView(R.id.attached_img) ImageView attachedImg;
    @BindView(R.id.message_list) RecyclerView messageList;
    private LinearLayoutManager linearLayoutManager;

    // Variables
    private String name;
    private String url;

    // Firestore
    private FirebaseFirestore db;
    private FirestoreRecyclerAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        ButterKnife.bind(this);
        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            name = bundle.getString("usernick");
        }

        addButton.setOnClickListener(this);

        writtenField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }
            @Override
            public void afterTextChanged(Editable editable) {
                System.out.println("Поиск совпадений");
                //Ищем совпадение с шаблоном ссылки на изображение
                //Хотел написать [\\./], но по какой-то причине не обрабатывает такое
                Pattern p = Pattern.compile("(https?://)?(([-\\w]/?)(\\.?))+\\.(jpg|png)");
                Matcher m = p.matcher(editable.toString());
                //Если есть
                if (m.find()) {
                    System.out.println("Есть совпадение");
                    //Делаем ImageView предв. просмотра видимой
                    attachedImg.setVisibility(View.VISIBLE);
                    //Заносим url в переменную
                    url = m.group();
                    //Загружаем саму картинку в виджет ImageView
                    Glide.with(ChatActivity.this).load(url).fitCenter().into(attachedImg);
                } else {
                    System.out.println("Нет совпадений");
                }
            }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }
        });

        init();
        adapter = getFirestoreRecyclerAdapter();
        messageList.setAdapter(adapter);
    }

    private void init() {
        linearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        messageList.setLayoutManager(linearLayoutManager);
        db = FirebaseFirestore.getInstance();
    }

    // Get adapter for message list
    private FirestoreRecyclerAdapter<Message, ChatHolder> getFirestoreRecyclerAdapter() {
        Query query = db.collection("messages").orderBy("date", Query.Direction.ASCENDING);

        FirestoreRecyclerOptions<Message> options = new FirestoreRecyclerOptions.Builder<Message>()
                .setQuery(query, Message.class)
                .build();

        return new FirestoreRecyclerAdapter<Message, ChatHolder>(options) {
            @Override
            protected void onBindViewHolder(ChatHolder holder, int position, Message message) {
                holder.user.setText(message.getUser());
                holder.text.setText(message.getMessage());
                holder.date.setText(message.dateToString());

                if (message.getImageUrl() != null) {
                    Glide.with(ChatActivity.this).load(message.getImageUrl()).fitCenter().into(holder.image);
                    holder.image.setOnClickListener(view -> {
                        Intent toImg = new Intent(ChatActivity.this, ImageActivity.class);
                        toImg.putExtra("user", message.getUser());
                        toImg.putExtra("url", message.getImageUrl());
                        toImg.putExtra("date", message.getDate().getTime());
                        startActivity(toImg);
                    });
                    holder.image.setVisibility(View.VISIBLE);
                } else {
                    holder.image.setVisibility(View.GONE);
                }

                if (message.getUser().equals(name)) {
                    holder.itemView.setBackground(getResources().getDrawable(R.drawable.message_window_yours));
                } else {
                    holder.itemView.setBackground(getResources().getDrawable(R.drawable.message_window));
                }
                Log.d("onBindViewHolder", "itemCount = " + String.valueOf(adapter.getItemCount()));
            }
            @Override
            public ChatHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.message_layout, parent, false);
                ChatHolder CH = new ChatHolder(view);
                // List to the bottom
                messageList.smoothScrollToPosition(adapter.getItemCount());
                return CH;
            }
        };
    }

    public class ChatHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.message_user) TextView user;
        @BindView(R.id.message_text) TextView text;
        @BindView(R.id.message_date) TextView date;
        @BindView(R.id.message_image) ImageView image;

        ChatHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.add_button:
                final DocumentReference doc = db.collection("messages").document();
                if (writtenField.getText().toString().length() != 0) {
                    final Message message = new Message(name, writtenField.getText().toString());
                    if (url != null) message.setImageUrl(url);
                    doc.set(message);
                    doc.update("date", FieldValue.serverTimestamp()).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            doc.get().addOnCompleteListener(task1 -> {
                                DocumentSnapshot snapshot = task1.getResult();
                                Date date = (Date) snapshot.get("date");
                                message.setDate(date);
                            });
                        }
                    });
                    writtenField.setText("");
                    hideSoftKeyboard(this);
                    adapter.notifyDataSetChanged();
                    attachedImg.setVisibility(View.GONE);
                    url = null;
                }
                break;
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        adapter.startListening();
    }

    @Override
    public void onStop() {
        super.onStop();
        adapter.stopListening();
    }

    public static void hideSoftKeyboard(Activity activity) {
        InputMethodManager inputMethodManager = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        if (inputMethodManager != null && activity.getCurrentFocus() != null)
            inputMethodManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
    }
}