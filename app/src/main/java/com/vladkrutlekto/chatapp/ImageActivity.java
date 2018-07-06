package com.vladkrutlekto.chatapp;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ImageActivity extends AppCompatActivity {

    @BindView(R.id.act_img_image) ImageView actImgImage;
    @BindView(R.id.act_img_user) TextView actImgUser;
    @BindView(R.id.act_img_date) TextView actImgDate;

    private String userName;
    private long userDate;
    private String messageImg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image);

        ButterKnife.bind(this);
        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            userName = bundle.getString("user");
            userDate = bundle.getLong("date");
            messageImg = bundle.getString("url");
        }

        actImgUser.setText(userName);
        actImgDate.setText(new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.ENGLISH).format(new Date(userDate)));
        Glide.with(this).load(messageImg).into(actImgImage);
    }
}
