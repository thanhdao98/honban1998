package com.example.rgbmems_smartphoneapp;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
public class ConnectionViewModel extends ViewModel {
    private final MutableLiveData<Boolean> isConnected = new MutableLiveData<>();

    public void setConnectionStatus(boolean status) {
        isConnected.setValue(status);
    }

    public LiveData<Boolean> getConnectionStatus() {
        return isConnected;
    }
}

