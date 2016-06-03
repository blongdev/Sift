package com.blongdev.sift;


import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Application;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;
import com.blongdev.sift.database.SiftContract;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;

/**
 * This is a subclass of {@link Application} used to provide shared objects for this app, such as
 * the {@link Tracker}.
 */
public class SiftApplication extends Application {

    public static final String ACCOUNT_TYPE = "com.blongdev";
    public static final String ACCOUNT_NAME = "Sift";
    public static final long SECONDS_PER_MINUTE = 60L;
    public static final long SYNC_INTERVAL_IN_MINUTES = 60L;
    public static final long SYNC_INTERVAL = SYNC_INTERVAL_IN_MINUTES * SECONDS_PER_MINUTE * 2;

    private Tracker mTracker;

    private static SiftApplication instance;
    public SiftApplication() {
        instance = this;
    }

    public static Context getContext() {
        return instance;
    }

    @Override
    public void onCreate ()
    {
        super.onCreate();
        //create sync account if it doesnt already exist
        createSyncAccount();
    }

    synchronized public Tracker getDefaultTracker() {
        if (mTracker == null) {
            GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
            mTracker = analytics.newTracker(R.xml.tracker);
        }
        return mTracker;
    }

    public static Account createSyncAccount() {
        Account newAccount = new Account(
                ACCOUNT_NAME, ACCOUNT_TYPE);
        AccountManager accountManager =
                (AccountManager) getContext().getSystemService(getContext().ACCOUNT_SERVICE);

        if (accountManager.addAccountExplicitly(newAccount, null, null)) {
            ContentResolver.setIsSyncable(newAccount, SiftContract.AUTHORITY, 1);
            ContentResolver.setSyncAutomatically(newAccount, SiftContract.AUTHORITY, true);
            ContentResolver.addPeriodicSync(newAccount, SiftContract.AUTHORITY, Bundle.EMPTY, SYNC_INTERVAL);

            return newAccount;
        }

        return null;
    }
}