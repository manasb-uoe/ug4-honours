package com.enthusiast94.edinfit.network;

/**
 * Created by manas on 26-09-2015.
 */
public interface Callback<T> {
    void onSuccess(T data);
    void onFailure(String message);
}
