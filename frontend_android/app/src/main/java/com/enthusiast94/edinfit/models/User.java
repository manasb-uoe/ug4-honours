package com.enthusiast94.edinfit.models;

/**
 * Created by manas on 26-09-2015.
 */
public class User {
    private String id;
    private String name;
    private String email;
    private long createdAt;
    private String authToken;

    public User(String id, String name, String email, long createdAt, String authtoken) {
        this.id = id;;
        this.name = name;
        this.email = email;
        this.createdAt = createdAt;
        this.authToken = authtoken;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public String getAuthToken() {
        return authToken;
    }
}
