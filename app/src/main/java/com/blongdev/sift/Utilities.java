package com.blongdev.sift;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.webkit.MimeTypeMap;

import com.blongdev.sift.database.SiftContract;

import net.dean.jraw.RedditClient;

/**
 * Created by Brian on 3/29/2016.
 */
public class Utilities {

    private static final int SECONDS_IN_MINUTE = 60;
    private static final int SECONDS_IN_HOUR = SECONDS_IN_MINUTE * 60;
    private static final int SECONDS_IN_DAY = SECONDS_IN_HOUR * 24;
    private static final int SECONDS_IN_MONTH_ISH = SECONDS_IN_DAY * 30;
    private static final int SECONDS_IN_YEAR_ISH = SECONDS_IN_DAY * 365;

    public static boolean connectedToNetwork() {
        ConnectivityManager cm =
                (ConnectivityManager) SiftApplication.getContext().getSystemService(SiftApplication.getContext().CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnected();
    }


    //returns _ID or -1 if updated
    public static long insertOrUpdate(Uri uri, ContentValues cv, String where, String[] selectionArgs) {
        int count = SiftApplication.getContext().getContentResolver().update(uri, cv, where, selectionArgs);
        if (count <= 0) {
            Uri newUri = SiftApplication.getContext().getContentResolver().insert(uri, cv);
            return ContentUris.parseId(newUri);
        }
        return -1;
    }

    public static long getSubredditId(String serverId) {
        String[] projection = new String[]{SiftContract.Subreddits._ID};
        String selection = SiftContract.Subreddits.COLUMN_SERVER_ID + " =?";
        String[] selectionArgs = new String[]{serverId};
        Cursor cursor = null;
        try {
            cursor = SiftApplication.getContext().getContentResolver().query(SiftContract.Subreddits.CONTENT_URI, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getLong(0);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return -1;
    }

    public static long getSubscriptionId(String subredditName) {
        String[] projection = new String[]{SiftContract.Subscriptions.COLUMN_SUBREDDIT_ID};
        String selection = SiftContract.Subreddits.COLUMN_NAME + " =?";
        String[] selectionArgs = new String[]{subredditName};
        Cursor cursor = null;
        try {
            cursor = SiftApplication.getContext().getContentResolver().query(SiftContract.Subscriptions.VIEW_URI, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getLong(0);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return -1;
    }

    public static long getFavoriteId(String serverId) {
        String[] projection = new String[]{SiftContract.Favorites.COLUMN_POST_ID};
        String selection = SiftContract.Posts.COLUMN_SERVER_ID + " = ?";
        String[] selectionArgs = new String[]{serverId};
        Cursor cursor = null;
        try {
            cursor = SiftApplication.getContext().getContentResolver().query(SiftContract.Favorites.VIEW_URI, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getLong(0);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return -1;
    }

    public static String getMimeType(String url) {
        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(url);
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }
        return type;
    }


    public static long getAgeInSeconds(long date) {
        long currentTime = System.currentTimeMillis();
        return (currentTime - date)/(1000);
    }

    public static String getAgeString(long date) {
        long age = getAgeInSeconds(date);
        if (age < SECONDS_IN_HOUR) {
            return age/SECONDS_IN_MINUTE + SiftApplication.getContext().getString(R.string.m);
        } else if (age < SECONDS_IN_DAY) {
            return age/SECONDS_IN_HOUR + SiftApplication.getContext().getString(R.string.h);
        } else if (age < SECONDS_IN_MONTH_ISH){
            return age/SECONDS_IN_DAY + SiftApplication.getContext().getString(R.string.d);
        } else if (age < SECONDS_IN_YEAR_ISH) {
            return age/SECONDS_IN_MONTH_ISH + SiftApplication.getContext().getString(R.string.M);
        } else {
            return age/SECONDS_IN_YEAR_ISH + SiftApplication.getContext().getString(R.string.Y);
        }
    }

    public static String getAgeStringLong(long date) {
        long age = getAgeInSeconds(date);
        if (age < SECONDS_IN_HOUR) {
            return age/SECONDS_IN_MINUTE + " " + SiftApplication.getContext().getString(R.string.minutes);
        } else if (age < SECONDS_IN_DAY) {
            return age/SECONDS_IN_HOUR + " " + SiftApplication.getContext().getString(R.string.hours);
        } else if (age < SECONDS_IN_MONTH_ISH){
            return age/SECONDS_IN_DAY + " " + SiftApplication.getContext().getString(R.string.days);
        } else if (age < SECONDS_IN_YEAR_ISH) {
            return age/SECONDS_IN_MONTH_ISH + " " + SiftApplication.getContext().getString(R.string.months);
        } else {
            return age/SECONDS_IN_YEAR_ISH + " " + SiftApplication.getContext().getString(R.string.years);
        }
    }

    public static boolean loggedIn () {
        Cursor cursor = null;
        try {
            cursor = SiftApplication.getContext().getContentResolver().query(SiftContract.Accounts.CONTENT_URI, null, null,null, null);
            if (cursor != null && cursor.moveToFirst()) {
                return true;
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return false;
    }

    public static String getLoggedInUsername () {
        Cursor cursor = null;
        try {
            cursor = SiftApplication.getContext().getContentResolver().query(SiftContract.Accounts.CONTENT_URI, null, null,null, null);
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getString(cursor.getColumnIndex(SiftContract.Accounts.COLUMN_USERNAME));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return null;
    }

    public static long getAccountId () {
        Cursor cursor = null;
        try {
            Reddit reddit = Reddit.getInstance();
            if(reddit.mRedditClient.isAuthenticated() && reddit.mRedditClient.hasActiveUserContext()) {
                String selection = SiftContract.Accounts.COLUMN_USERNAME + " = ?";
                reddit.mRateLimiter.acquire();
                String[] selectionArgs = new String[]{reddit.mRedditClient.me().getFullName()};
                cursor = SiftApplication.getContext().getContentResolver().query(SiftContract.Accounts.CONTENT_URI, null, selection, selectionArgs, null);
                if (cursor != null && cursor.moveToFirst()) {
                    return cursor.getLong(cursor.getColumnIndex(SiftContract.Accounts._ID));
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return -1;
    }

    public static long getSavedPostId (String serverId) {
        Cursor cursor = null;
        try {
            String selection = SiftContract.Posts.COLUMN_SERVER_ID + " = ? AND " + SiftContract.Posts.FAVORITED + " = 1";
            String[] selectionArgs = new String[]{serverId};
            cursor = SiftApplication.getContext().getContentResolver().query(SiftContract.Posts.CONTENT_URI, null, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getLong(cursor.getColumnIndex(SiftContract.Posts._ID));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return -1;
    }

    public static long getSavedUserId (String username) {
        Cursor cursor = null;
        try {
            String selection = SiftContract.Users.COLUMN_USERNAME + " = ?";
            String[] selectionArgs = new String[]{username};
            cursor = SiftApplication.getContext().getContentResolver().query(SiftContract.Users.CONTENT_URI, null, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getLong(cursor.getColumnIndex(SiftContract.Users._ID));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return -1;
    }

    public static boolean isFriend (String username) {
        Cursor cursor = null;
        try {
            String selection = SiftContract.Users.COLUMN_USERNAME + " = ?";
            String[] selectionArgs = new String[]{username};
            cursor = SiftApplication.getContext().getContentResolver().query(SiftContract.Friends.VIEW_URI, null, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                return true;
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return false;
    }

    public static String getServerIdFromFullName(String fullName) {
        String[] parts = fullName.split("_");
        if (parts.length > 1) {
            return parts[1];
        }
        return null;
    }

    public static int getVoteValue(String postServerId) {
        Cursor cursor = null;
        try {
            String[] projection = new String[] {SiftContract.Posts.COLUMN_VOTE};
            String selection = SiftContract.Posts.COLUMN_SERVER_ID + " = ?";
            String[] selectionArgs = new String[]{postServerId};
            cursor = SiftApplication.getContext().getContentResolver().query(SiftContract.Posts.CONTENT_URI, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getInt(0);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return SiftContract.Posts.NO_VOTE;
    }

    public static void markRead(long messageId) {
        ContentValues cv = new ContentValues();
        cv.put(SiftContract.Messages.COLUMN_READ, 1);
        String where = SiftContract.Messages._ID + " = ?";
        String[] whereArgs = new String[]{String.valueOf(messageId)};
        SiftApplication.getContext().getContentResolver().update(SiftContract.Messages.CONTENT_URI, cv, where, whereArgs);
    }
}
