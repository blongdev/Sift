package com.blongdev.sift;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.widget.Toast;

public class SiftBroadcastReceiver extends BroadcastReceiver {

    public final static String LOGGED_IN = "com.blongdev.sift.loggedIn";
    public final static String LOGGED_OUT= "com.blongdev.sift.loggedOut";

    public SiftBroadcastReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (TextUtils.equals(intent.getAction(), LOGGED_IN)) {
            Toast.makeText(context, context.getString(R.string.logged_in), Toast.LENGTH_LONG).show();
            Intent activity = new Intent(SiftApplication.getContext(), MainActivity.class);
            activity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            SiftApplication.getContext().startActivity(activity);
        } else if (TextUtils.equals(intent.getAction(), LOGGED_OUT)) {
                Toast.makeText(context, context.getString(R.string.logged_out), Toast.LENGTH_LONG).show();
                Intent activity = new Intent(SiftApplication.getContext(), MainActivity.class);
                activity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                SiftApplication.getContext().startActivity(activity);
        }
    }
}
