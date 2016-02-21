package com.enthusiast94.edinfit.models;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Select;

/**
 * Created by manas on 08-01-2016.
 */

@Table(name = "Users")
public class User extends Model {

    @Column private String _id; /* id provided by server */
    @Column private String name;
    @Column private String email;
    @Column private long createdAt;
    @Column private String authToken;
    @Column private int weight; /* in kg */

    public User() {
        super();
    }

    public User(String _id, String name, String email, long createdAt, String authToken) {
        this._id = _id;
        this.name = name;
        this.email = email;
        this.createdAt = createdAt;
        this.authToken = authToken;
    }

    public String get_id() {
        return _id;
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

    public int getWeight() {
        return weight;
    }

    public void set_id(String _id) {
        this._id = _id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public static User findById(long id) {
        return new Select()
                .from(User.class)
                .where("id = ?", id)
                .executeSingle();
    }
}
