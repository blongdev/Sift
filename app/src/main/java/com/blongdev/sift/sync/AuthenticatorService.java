package com.blongdev.sift.sync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

/**
 * Created by Brian on 3/23/2016.
 */

public class AuthenticatorService extends Service {
    private Authenticator mAuthenticator;
    @Override
    public void onCreate() {
        Log.i("Authenticator", "Service created");
        mAuthenticator = new Authenticator(this);
    }


    @Override
    public IBinder onBind(Intent intent) {
        return mAuthenticator.getIBinder();
    }
}