package com.scorm.generator.dto;

public class UserProfileResponse {
    private Long userId;
    private String fname;
    private String minit;
    private String lname;
    private String email;
    private String avatarUrl;

    public UserProfileResponse() {
    }

    public UserProfileResponse(Long userId, String fname, String minit, String lname, String email, String avatarUrl) {
        this.userId = userId;
        this.fname = fname;
        this.minit = minit;
        this.lname = lname;
        this.email = email;
        this.avatarUrl = avatarUrl;
    }

    // Getters and Setters
    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getFname() {
        return fname;
    }

    public void setFname(String fname) {
        this.fname = fname;
    }

    public String getMinit() {
        return minit;
    }

    public void setMinit(String minit) {
        this.minit = minit;
    }

    public String getLname() {
        return lname;
    }

    public void setLname(String lname) {
        this.lname = lname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }
}