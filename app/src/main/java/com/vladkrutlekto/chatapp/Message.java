package com.vladkrutlekto.chatapp;

import com.google.firebase.firestore.ServerTimestamp;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Message {
    private SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.ENGLISH);

    private String user;
    private String message;
    @ServerTimestamp Date date;
    private String imageUrl;

    public Message() {}

    public Message(String user, String message) {
        this.user = user;
        this.message = message;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String dateToString() {
        if (date != null) {
            return sdf.format(date);
        } else {
            return null;
        }
        //sdf.setTimeZone(TimeZone.getTimeZone("Asia/Krasnoyarsk"));
    }
}
