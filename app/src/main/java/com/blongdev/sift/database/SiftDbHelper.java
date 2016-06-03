package com.blongdev.sift.database;

/**
 * Created by Brian on 3/12/2016.
 */


import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import com.blongdev.sift.AccountInfo;
import com.blongdev.sift.CommentInfo;
import com.blongdev.sift.FavoritesInfo;
import com.blongdev.sift.FriendInfo;
import com.blongdev.sift.MessageInfo;
import com.blongdev.sift.PostInfo;
import com.blongdev.sift.SiftApplication;
import com.blongdev.sift.SubredditInfo;
import com.blongdev.sift.SubscriptionInfo;
import com.blongdev.sift.UserInfo;
import com.blongdev.sift.VoteInfo;

public class SiftDbHelper extends SQLiteOpenHelper {

    private ContentResolver mContentResolver;

    public SiftDbHelper(String name,
                        CursorFactory factory, int version) {
        super(SiftApplication.getContext(), SiftContract.DATABASE_NAME, factory, SiftContract.DATABASE_VERSION);
        mContentResolver = SiftApplication.getContext().getContentResolver();
    }

    public SiftDbHelper() {
        super(SiftApplication.getContext(), SiftContract.DATABASE_NAME, null, SiftContract.DATABASE_VERSION);
        mContentResolver = SiftApplication.getContext().getContentResolver();
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.execSQL("PRAGMA foreign_keys=ON;");
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SiftContract.Posts.CREATE_TABLE);
        db.execSQL(SiftContract.Accounts.CREATE_TABLE);
//        db.execSQL(SiftContract.Comments.CREATE_TABLE);
        db.execSQL(SiftContract.Subreddits.CREATE_TABLE);
        db.execSQL(SiftContract.Users.CREATE_TABLE);
        db.execSQL(SiftContract.Messages.CREATE_TABLE);
        db.execSQL(SiftContract.Subscriptions.CREATE_TABLE);
        db.execSQL(SiftContract.Favorites.CREATE_TABLE);
//        db.execSQL(SiftContract.Votes.CREATE_TABLE);
        db.execSQL(SiftContract.Friends.CREATE_TABLE);

        db.execSQL(SiftContract.Friends.CREATE_VIEW);
        db.execSQL(SiftContract.Subscriptions.CREATE_VIEW);
        db.execSQL(SiftContract.Favorites.CREATE_VIEW);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onCreate(db);
    }

}