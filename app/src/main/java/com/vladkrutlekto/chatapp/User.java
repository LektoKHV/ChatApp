package com.vladkrutlekto.chatapp;

public class User {
    private String userName;
    private String userEmail;
    private String userPicUriString;
    private String userNickName;
    private String userId;

    public User(String userName, String userEmail, String userPicUriString, String userId) {
        this.userName = userName;
        this.userEmail = userEmail;
        this.userPicUriString = userPicUriString;
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getUserPicUriString() {
        return userPicUriString;
    }

    public void setUserPicUriString(String userPicUriString) {
        this.userPicUriString = userPicUriString;
    }

    public String getUserNickName() {
        return userNickName;
    }

    public void setUserNickName(String userNickName) {
        this.userNickName = userNickName;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
