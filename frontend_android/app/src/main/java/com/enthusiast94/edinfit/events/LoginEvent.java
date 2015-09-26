package com.enthusiast94.edinfit.events;

/**
 * Created by manas on 26-09-2015.
 */
public class LoginEvent {

    private String email;
    private String password;

    public LoginEvent(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }
}
