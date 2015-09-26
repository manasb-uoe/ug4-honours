package com.enthusiast94.edinfit.events;

/**
 * Created by manas on 26-09-2015.
 */
public class SignupEvent {
    private String name;
    private String email;
    private String password;

    public SignupEvent(String name, String email, String password) {
        this.name = name;
        this.email = email;
        this.password = password;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }
}
