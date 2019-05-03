package com.blongdev.sift;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.ActivityOptions;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.text.format.Time;
import android.util.Log;
import android.widget.Toast;

import com.blongdev.sift.database.SiftContract;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.RateLimiter;

import net.dean.jraw.ApiException;
import net.dean.jraw.RedditClient;
import net.dean.jraw.http.LoggingMode;
import net.dean.jraw.http.NetworkException;
import net.dean.jraw.http.UserAgent;
import net.dean.jraw.http.oauth.Credentials;
import net.dean.jraw.http.oauth.OAuthData;
import net.dean.jraw.http.oauth.OAuthException;
import net.dean.jraw.http.oauth.OAuthHelper;
import net.dean.jraw.models.Captcha;
import net.dean.jraw.models.Comment;
import net.dean.jraw.models.LoggedInAccount;
import net.dean.jraw.models.Submission;
import net.dean.jraw.models.Subreddit;
import net.dean.jraw.models.VoteDirection;
import net.dean.jraw.paginators.Paginator;
import net.dean.jraw.paginators.Sorting;
import net.dean.jraw.paginators.TimePeriod;


import java.net.URL;
import java.util.List;
import java.util.UUID;

/**
 * Created by Brian on 3/21/2016.
 */
public class Reddit {

    private static final String REFRESH_KEY = "refreshKey";
    private static final String CLIENT_ID = "pFsQuM_0DQdv-g";
    private static final String REDIRECT_URL = "http://www.google.com";

    public static final String ACCOUNT_TYPE = "com.blongdev";
    public static final String GENERAL_ACCOUNT = "General";

    public static final String AUTHENTICATED = "com.blongdev.sift.AUTHENTICATED";


    public static final long SECONDS_PER_MINUTE = 60L;
    public static final long SYNC_INTERVAL_IN_MINUTES = 60L;
    public static final long SYNC_INTERVAL = SYNC_INTERVAL_IN_MINUTES * SECONDS_PER_MINUTE;

    private static Reddit instance = new Reddit();
    public RedditClient mRedditClient;

    public UserAgent mUserAgent;
    public String mRefreshToken;
    public Credentials mCredentials;
    public OAuthHelper mOAuthHelper;
    public RateLimiter mRateLimiter;
    public Sorting mSort;
    public TimePeriod mTime;
    public long mRefreshTime;

    private static final String LOG_TAG = "Reddit Singleton";


    public static Reddit getInstance() {
        return instance;
    }

    private Reddit() {
        mUserAgent = getUserAgent();
        mRedditClient = new RedditClient(mUserAgent);
        mCredentials = getCredentials();
        mOAuthHelper = mRedditClient.getOAuthHelper();
        mRateLimiter = RateLimiter.create(1);
        mSort = Paginator.DEFAULT_SORTING;
        mTime = Paginator.DEFAULT_TIME_PERIOD;

        mRedditClient.setLoggingMode(LoggingMode.ALWAYS);
        mRedditClient.setSaveResponseHistory(true);

        mRefreshTime = System.currentTimeMillis() + (60 * 60 * 1000);
    }

    public static UserAgent getUserAgent () {
        return UserAgent.of("Android", "com.blongdev.sift", BuildConfig.VERSION_NAME, "blongdev");
    }

    public static Credentials getCredentials() {
        return Credentials.installedApp(CLIENT_ID, REDIRECT_URL);
    }


    public void refreshKey() {
        Cursor cursor = null;
        try {
            cursor = SiftApplication.getContext().getContentResolver().query(SiftContract.Accounts.CONTENT_URI, null, null, null, null);
            if (cursor != null) {
                 if (cursor.moveToFirst()) {
                    mRefreshToken = cursor.getString(cursor.getColumnIndex(SiftContract.Accounts.COLUMN_REFRESH_KEY));
                    if (mRefreshToken != null && !mRefreshToken.isEmpty()) {
                        new RefreshTokenTask().execute();
                        return;
                    }
                 } else {
                    new GetUserlessTask().execute();
                 }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

    }

    private final class UserChallengeTask extends AsyncTask<String, Void, OAuthData> {

        public UserChallengeTask() {
            Log.v(LOG_TAG, "UserChallengeTask()");
        }

        @Override
        protected OAuthData doInBackground(String... params) {
            Log.v(LOG_TAG, "doInBackground()");
            Log.v(LOG_TAG, "params[0]: " + params[0]);
            try {
                OAuthData oAuthData =  mOAuthHelper.onUserChallenge(params[0], mCredentials);
                if (oAuthData != null) {
                    mRateLimiter.acquire();
                    mRedditClient.authenticate(oAuthData);
                    mRefreshTime = System.currentTimeMillis() + (60 * 60 * 1000);
                    addUser();
                } else {
                    Log.e(LOG_TAG, "Passed in OAuthData was null");
                }
            } catch (RuntimeException | OAuthException e) {
                Log.e(LOG_TAG, e.getMessage());
            }
            return null;
        }

        @Override
        protected void onPostExecute(OAuthData oAuthData) {
            Log.v(LOG_TAG, "onPostExecute()");
        }
    }

    public AsyncTask runUserChallengeTask(String url) {
        return new UserChallengeTask().execute(url);
    }

    public void addUser() {
        if (!mRedditClient.isAuthenticated()) {
            return;
        }

        mRateLimiter.acquire();
        LoggedInAccount me = mRedditClient.me();
        String username = me.getFullName();
        String serverId = me.getId();
        long date = me.getCreated().getTime();
        long commentKarma = me.getCommentKarma();
        long linkKarma = me.getLinkKarma();

        //user
        ContentValues cv = new ContentValues();
        cv.put(SiftContract.Users.COLUMN_USERNAME, username);
        cv.put(SiftContract.Users.COLUMN_DATE_CREATED, date);
        cv.put(SiftContract.Users.COLUMN_SERVER_ID, serverId);
        cv.put(SiftContract.Users.COLUMN_COMMENT_KARMA, commentKarma);
        cv.put(SiftContract.Users.COLUMN_LINK_KARMA, linkKarma);

        Uri userUri = SiftApplication.getContext().getContentResolver().insert(SiftContract.Users.CONTENT_URI, cv);
        long userId = ContentUris.parseId(userUri);

        cv.clear();

        //account
        String refreshToken = mRedditClient.getOAuthData().getRefreshToken();

        cv.put(SiftContract.Accounts.COLUMN_REFRESH_KEY, refreshToken);
        cv.put(SiftContract.Accounts.COLUMN_USER_ID, userId);
        cv.put(SiftContract.Accounts.COLUMN_USERNAME, username);
        SiftApplication.getContext().getContentResolver().insert(SiftContract.Accounts.CONTENT_URI, cv);

        cv.clear();

        runInitialSync();
    }

    private final class RefreshTokenTask extends AsyncTask<String, Void, OAuthData> {

        public RefreshTokenTask() {
        }

        @Override
        protected OAuthData doInBackground(String... params) {
            if (!mRefreshToken.isEmpty()) {
                mOAuthHelper.setRefreshToken(mRefreshToken);
                try {
                    mRateLimiter.acquire();
                    OAuthData finalData = mOAuthHelper.refreshToken(mCredentials);
                    mRateLimiter.acquire();
                    mRedditClient.authenticate(finalData);
                    mRefreshTime = System.currentTimeMillis() + (60 * 60 * 1000);
                    if (mRedditClient.isAuthenticated()) {
                        Log.v(LOG_TAG, "Authenticated");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(OAuthData oAuthData) {
            Log.v(LOG_TAG, "onPostExecute()");

            Intent refreshIntent = new Intent(AUTHENTICATED);
            LocalBroadcastManager.getInstance(SiftApplication.getContext()).sendBroadcast(refreshIntent);
        }
    }



    private final class GetUserlessTask extends AsyncTask<String, Void, Void> {

        public GetUserlessTask() {

        }

        @Override
        protected Void doInBackground(String... params) {
            try {
                String android_id = Settings.Secure.getString(SiftApplication.getContext().getContentResolver(),
                        Settings.Secure.ANDROID_ID);
                UUID uuid = UUID.randomUUID();
                Credentials credentials = Credentials.userlessApp(CLIENT_ID, uuid);

                mRateLimiter.acquire();
                OAuthData authData = mRedditClient.getOAuthHelper().easyAuth(credentials);
                mRateLimiter.acquire();
                mRedditClient.authenticate(authData);
                mRefreshTime = System.currentTimeMillis() + (60 * 60 * 1000);
                if (mRedditClient.isAuthenticated()) {
                    Log.v(LOG_TAG, "Authenticated");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void nothing) {
            Log.v(LOG_TAG, "onPostExecute()");

            Intent refreshIntent = new Intent(AUTHENTICATED);
            LocalBroadcastManager.getInstance(SiftApplication.getContext()).sendBroadcast(refreshIntent);
        }
    }


    public void runInitialSync() {
        AccountManager accountManager = (AccountManager) SiftApplication.getContext().getSystemService(SiftApplication.getContext().ACCOUNT_SERVICE);
        Account account = accountManager.getAccountsByType(ACCOUNT_TYPE)[0];

        if (account != null) {
            Bundle bundle = new Bundle();
            bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
            bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
            bundle.putBoolean(SiftApplication.getContext().getString(R.string.initial_sync), true);
            ContentResolver.requestSync(account, SiftContract.AUTHORITY, bundle);
        }
    }


    public void removeAccounts() {
        //revoke access token
        if (mRedditClient.isAuthenticated() && mRedditClient.hasActiveUserContext()) {
            new RevokeTokenTask().execute();
        }
    }

    private final class RevokeTokenTask extends AsyncTask<String, Void, Void> {

        public RevokeTokenTask() {

        }

        @Override
        protected Void doInBackground(String... params) {

            Cursor cursor = null;
            try {
                cursor = SiftApplication.getContext().getContentResolver().query(SiftContract.Accounts.CONTENT_URI, null, null, null, null);
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        String accountId = cursor.getString(cursor.getColumnIndex(SiftContract.Accounts._ID));
                        mRefreshToken = cursor.getString(cursor.getColumnIndex(SiftContract.Accounts.COLUMN_REFRESH_KEY));
                        if (mRefreshToken != null && !mRefreshToken.isEmpty()) {
                            String selection = SiftContract.Accounts._ID + " =?";
                            String[] selectionArgs = new String[]{accountId};
                            SiftApplication.getContext().getContentResolver().delete(SiftContract.Accounts.CONTENT_URI, selection, selectionArgs);
                            instance = new Reddit();
                        }
                    }
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void nothing) {
            Log.v(LOG_TAG, "onPostExecute()");

            Intent refreshIntent = new Intent(SiftBroadcastReceiver.LOGGED_OUT);
            SiftApplication.getContext().sendBroadcast(refreshIntent);
        }
    }

    public static void votePost(String serverId, int vote) {
        new VotePostTask(serverId, vote).execute();
    }

    private static final class VotePostTask extends AsyncTask<String, Void, Void> {
        String mServerId;
        int mVote;

        public VotePostTask( String serverId, int vote) {
            mServerId = serverId;
            mVote = vote;
        }

        @Override
        protected void onPreExecute() {

        }

        @Override
        protected Void doInBackground(String... params) {
            try {

                if (!instance.mRedditClient.isAuthenticated() || !instance.mRedditClient.hasActiveUserContext()) {
                    return null;
                }

                instance.mRateLimiter.acquire();
                Submission sub = instance.mRedditClient.getSubmission(mServerId);
                if (sub == null || sub.isArchived()) {
                    return null;
                }
                net.dean.jraw.managers.AccountManager accountManager = new net.dean.jraw.managers.AccountManager(instance.mRedditClient);

                switch (mVote) {
                    case SiftContract.Posts.NO_VOTE:
                        instance.mRateLimiter.acquire();
                        accountManager.vote(sub, VoteDirection.NO_VOTE);
                        break;
                    case SiftContract.Posts.UPVOTE:
                        instance.mRateLimiter.acquire();
                        accountManager.vote(sub, VoteDirection.UPVOTE);
                        break;
                    case SiftContract.Posts.DOWNVOTE:
                        instance.mRateLimiter.acquire();
                        accountManager.vote(sub, VoteDirection.DOWNVOTE);
                        break;
                }

                ContentValues cv = new ContentValues();
                cv.put(SiftContract.Posts.COLUMN_VOTE, mVote);
                String selection = SiftContract.Posts.COLUMN_SERVER_ID + " = ?";
                String[] selectionArgs = new String[]{String.valueOf(mServerId)};
                int count = SiftApplication.getContext().getContentResolver().update(SiftContract.Posts.CONTENT_URI, cv, selection, selectionArgs);
                Log.v("Reddit", count + " vote updated");

                if (count == 0) {
                    cv.clear();
                    cv.put(SiftContract.Posts.COLUMN_SERVER_ID, sub.getId());
                    cv.put(SiftContract.Posts.COLUMN_TITLE, sub.getTitle());
                    cv.put(SiftContract.Posts.COLUMN_OWNER_USERNAME, sub.getAuthor());
                    cv.put(SiftContract.Posts.COLUMN_SUBREDDIT_NAME, sub.getSubredditName());
                    cv.put(SiftContract.Posts.COLUMN_POINTS, sub.getScore());
                    cv.put(SiftContract.Posts.COLUMN_URL, sub.getUrl());
                    cv.put(SiftContract.Posts.COLUMN_IMAGE_URL, getImageUrl(sub));
                    cv.put(SiftContract.Posts.COLUMN_NUM_COMMENTS, sub.getCommentCount());
                    cv.put(SiftContract.Posts.COLUMN_BODY, sub.getSelftext());
                    cv.put(SiftContract.Posts.COLUMN_DOMAIN, sub.getDomain());
                    cv.put(SiftContract.Posts.COLUMN_DATE_CREATED, sub.getCreated().getTime());
                    cv.put(SiftContract.Posts.COLUMN_VOTE, mVote);
                    cv.put(SiftContract.Posts.COLUMN_FAVORITED, 1);
                    SiftApplication.getContext().getContentResolver().insert(SiftContract.Posts.CONTENT_URI, cv);
                }

            } catch (ApiException | RuntimeException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void nothing) {

        }
    }

    public static void voteComment(Comment comment, int vote) {
        new VoteCommentTask(comment, vote).execute();
    }

    private static final class VoteCommentTask extends AsyncTask<String, Void, Void> {
        Comment mComment;
        int mVote;

        public VoteCommentTask(Comment comment, int vote) {
            mComment = comment;
            mVote = vote;
        }

        @Override
        protected void onPreExecute() {

        }

        @Override
        protected Void doInBackground(String... params) {
            try {

                if (!instance.mRedditClient.isAuthenticated() || !instance.mRedditClient.hasActiveUserContext()) {
                    return null;
                }

                if (mComment == null || mComment.isArchived()) {
                    return null;
                }
                net.dean.jraw.managers.AccountManager accountManager = new net.dean.jraw.managers.AccountManager(instance.mRedditClient);

                switch (mVote) {
                    case SiftContract.Posts.NO_VOTE:
                        instance.mRateLimiter.acquire();
                        accountManager.vote(mComment, VoteDirection.NO_VOTE);
                        break;
                    case SiftContract.Posts.UPVOTE:
                        instance.mRateLimiter.acquire();
                        accountManager.vote(mComment, VoteDirection.UPVOTE);
                        break;
                    case SiftContract.Posts.DOWNVOTE:
                        instance.mRateLimiter.acquire();
                        accountManager.vote(mComment, VoteDirection.DOWNVOTE);
                        break;
                }

            } catch (ApiException | RuntimeException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void nothing) {

        }
    }

    public static void subscribe(String name) {
        new SubscribeTask(name).execute();
    }

    private static final class SubscribeTask extends AsyncTask<String, Void, Void> {
        String mName;

        public SubscribeTask(String name) {
            mName = name;
        }

        @Override
        protected void onPreExecute() {

        }

        @Override
        protected Void doInBackground(String... params) {

            if (!instance.mRedditClient.isAuthenticated() || !instance.mRedditClient.hasActiveUserContext()) {
                return null;
            }

            try {
                instance.mRateLimiter.acquire();
                Subreddit subreddit = instance.mRedditClient.getSubreddit(mName);

                if (subreddit == null) {
                    return null;
                }

                net.dean.jraw.managers.AccountManager accountManager = new net.dean.jraw.managers.AccountManager(instance.mRedditClient);
                instance.mRateLimiter.acquire();
                accountManager.subscribe(subreddit);

                ContentValues cv = new ContentValues();
                long subredditId = Utilities.getSubredditId(subreddit.getId());
                if (subredditId <= 0) {
                    cv.put(SiftContract.Subreddits.COLUMN_NAME, subreddit.getDisplayName());
                    cv.put(SiftContract.Subreddits.COLUMN_SERVER_ID, subreddit.getId());
                    cv.put(SiftContract.Subreddits.COLUMN_DESCRIPTION, subreddit.getPublicDescription());

                    try {
                        //bug in jraw library sometimes throws nullpointerexception
                        cv.put(SiftContract.Subreddits.COLUMN_SUBSCRIBERS, subreddit.getSubscriberCount());
                    } catch (NullPointerException e) {
                        e.printStackTrace();
                    }

                    Uri subredditUri = SiftApplication.getContext().getContentResolver().insert(SiftContract.Subreddits.CONTENT_URI, cv);
                    subredditId = ContentUris.parseId(subredditUri);
                    cv.clear();
                }

                //add subscription
                long accountId = Utilities.getAccountId();
                if (accountId > 0) {
                    cv.put(SiftContract.Subscriptions.COLUMN_ACCOUNT_ID, accountId);
                    cv.put(SiftContract.Subscriptions.COLUMN_SUBREDDIT_ID, subredditId);
                    SiftApplication.getContext().getContentResolver().insert(SiftContract.Subscriptions.CONTENT_URI, cv);
                    cv.clear();
                }

            } catch (RuntimeException e) {
                e.printStackTrace();
            }

            Log.v("Reddit", "Subscribed");

            return null;
        }

        @Override
        protected void onPostExecute(Void nothing) {

        }
    }

    public static void unsubscribe(String name) {
        new UnsubscribeTask(name).execute();
    }

    private static final class UnsubscribeTask extends AsyncTask<String, Void, Void> {
        String mName;

        public UnsubscribeTask(String name) {
            mName = name;
        }

        @Override
        protected void onPreExecute() {

        }

        @Override
        protected Void doInBackground(String... params) {

            if (!instance.mRedditClient.isAuthenticated() || !instance.mRedditClient.hasActiveUserContext()) {
                return null;
            }

            try {
                instance.mRateLimiter.acquire();
                Subreddit subreddit = instance.mRedditClient.getSubreddit(mName);
                if (subreddit == null) {
                    return null;
                }

                net.dean.jraw.managers.AccountManager accountManager = new net.dean.jraw.managers.AccountManager(instance.mRedditClient);
                instance.mRateLimiter.acquire();
                accountManager.unsubscribe(subreddit);

                long subredditId = Utilities.getSubredditId(subreddit.getId());

                String selection = SiftContract.Subscriptions.COLUMN_SUBREDDIT_ID + " = ?";
                String[] selectionArgs = new String[]{String.valueOf(subredditId)};
                int count = SiftApplication.getContext().getContentResolver().delete(SiftContract.Subscriptions.CONTENT_URI, selection, selectionArgs);
                Log.v("Reddit", "Unsubscribed");

            } catch (RuntimeException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void nothing) {

        }
    }

    public static void favoritePost(String serverId) {
        new FavoritePostTask(serverId).execute();
    }

    private static final class FavoritePostTask extends AsyncTask<String, Void, Void> {
        String mServerId;

        public FavoritePostTask(String serverId) {
            mServerId = serverId;
        }

        @Override
        protected void onPreExecute() {

        }

        @Override
        protected Void doInBackground(String... params) {

            if (!instance.mRedditClient.isAuthenticated() || !instance.mRedditClient.hasActiveUserContext()) {
                return null;
            }

            try {
                instance.mRateLimiter.acquire();
                Submission sub = instance.mRedditClient.getSubmission(mServerId);
                if (sub == null) {
                    return null;
                }

                net.dean.jraw.managers.AccountManager accountManager = new net.dean.jraw.managers.AccountManager(instance.mRedditClient);

                instance.mRateLimiter.acquire();
                accountManager.save(sub);

                ContentValues cv = new ContentValues();
                cv.put(SiftContract.Posts.COLUMN_SERVER_ID, sub.getId());
                cv.put(SiftContract.Posts.COLUMN_TITLE, sub.getTitle());
                cv.put(SiftContract.Posts.COLUMN_OWNER_USERNAME, sub.getAuthor());
                cv.put(SiftContract.Posts.COLUMN_SUBREDDIT_NAME, sub.getSubredditName());
                cv.put(SiftContract.Posts.COLUMN_POINTS, sub.getScore());
                cv.put(SiftContract.Posts.COLUMN_URL, sub.getUrl());
                cv.put(SiftContract.Posts.COLUMN_IMAGE_URL, getImageUrl(sub));
                cv.put(SiftContract.Posts.COLUMN_NUM_COMMENTS, sub.getCommentCount());
                cv.put(SiftContract.Posts.COLUMN_BODY, sub.getSelftext());
                cv.put(SiftContract.Posts.COLUMN_DOMAIN, sub.getDomain());
                cv.put(SiftContract.Posts.COLUMN_DATE_CREATED, sub.getCreated().getTime());
                cv.put(SiftContract.Posts.COLUMN_VOTE, sub.getVote().getValue());
                cv.put(SiftContract.Posts.COLUMN_FAVORITED, 1);
                Uri uri = SiftApplication.getContext().getContentResolver().insert(SiftContract.Posts.CONTENT_URI, cv);
                long postId = ContentUris.parseId(uri);

                cv.clear();
                long accountId = Utilities.getAccountId();
                cv.put(SiftContract.Favorites.COLUMN_ACCOUNT_ID, accountId);
                cv.put(SiftContract.Favorites.COLUMN_POST_ID, postId);
                SiftApplication.getContext().getContentResolver().insert(SiftContract.Favorites.CONTENT_URI, cv);

            } catch (ApiException | RuntimeException e) {
                e.printStackTrace();
            }

            Log.v("Reddit", "Saved");

            return null;
        }

        @Override
        protected void onPostExecute(Void nothing) {

        }
    }

    public static void unfavoritePost(String serverId) {
        new UnfavoritePostTask(serverId).execute();
    }

    private static final class UnfavoritePostTask extends AsyncTask<String, Void, Void> {
        String mServerId;
        long mPostId;

        public UnfavoritePostTask(String serverId) {
            mServerId = serverId;
        }

        @Override
        protected void onPreExecute() {

        }

        @Override
        protected Void doInBackground(String... params) {

            if (!instance.mRedditClient.isAuthenticated() || !instance.mRedditClient.hasActiveUserContext()) {
                return null;
            }

            try {
                instance.mRateLimiter.acquire();
                Submission sub = instance.mRedditClient.getSubmission(mServerId);
                if (sub == null) {
                    return null;
                }

                net.dean.jraw.managers.AccountManager accountManager = new net.dean.jraw.managers.AccountManager(instance.mRedditClient);

                instance.mRateLimiter.acquire();
                accountManager.unsave(sub);

                long postId = Utilities.getSavedPostId(mServerId);

                if (postId > 0) {
                    String selection = SiftContract.Favorites.COLUMN_POST_ID + " = ?";
                    String[] selectionArgs = new String[]{String.valueOf(postId)};
                    int count = SiftApplication.getContext().getContentResolver().delete(SiftContract.Favorites.CONTENT_URI, selection, selectionArgs);
                    Log.v("Reddit", count + " Unsaved");

                }

            } catch (ApiException | RuntimeException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void nothing) {

        }
    }


    public static void commentOnPost(String serverId, String comment) {
        new CommentOnPostTask(serverId, comment).execute();
    }

    private static final class CommentOnPostTask extends AsyncTask<String, Void, Void> {
        String mServerId;
        String mComment;

        public CommentOnPostTask(String serverId, String comment) {
            mServerId = serverId;
            mComment =  comment;
        }

        @Override
        protected void onPreExecute() {

        }

        @Override
        protected Void doInBackground(String... params) {

            if (!instance.mRedditClient.isAuthenticated() || !instance.mRedditClient.hasActiveUserContext()) {
                return null;
            }

            try {
                instance.mRateLimiter.acquire();
                Submission sub = instance.mRedditClient.getSubmission(mServerId);
                if (sub == null) {
                    return null;
                }

                net.dean.jraw.managers.AccountManager accountManager = new net.dean.jraw.managers.AccountManager(instance.mRedditClient);
                instance.mRateLimiter.acquire();
                accountManager.reply(sub, mComment);
            } catch (ApiException | RuntimeException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void nothing) {
            Toast.makeText(SiftApplication.getContext(), SiftApplication.getContext().getString(R.string.comment_posted), Toast.LENGTH_LONG).show();
        }
    }

    public static void replyToComment(Comment comment, String reply) {
        new ReplyToCommentTask(comment, reply).execute();
    }

    private static final class ReplyToCommentTask extends AsyncTask<String, Void, Void> {
        String mReply;
        Comment mComment;

        public ReplyToCommentTask(Comment comment, String reply) {
            mReply = reply;
            mComment =  comment;
        }

        @Override
        protected void onPreExecute() {

        }

        @Override
        protected Void doInBackground(String... params) {

            if (!instance.mRedditClient.isAuthenticated() || !instance.mRedditClient.hasActiveUserContext()) {
                return null;
            }

            try {
                net.dean.jraw.managers.AccountManager accountManager = new net.dean.jraw.managers.AccountManager(instance.mRedditClient);
                instance.mRateLimiter.acquire();
                accountManager.reply(mComment, mReply);

            } catch (ApiException | RuntimeException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void nothing) {
            Toast.makeText(SiftApplication.getContext(), SiftApplication.getContext().getString(R.string.comment_posted), Toast.LENGTH_LONG).show();
        }
    }


    public static void addFriend(String username) {
        new AddFriendTask(username).execute();
    }

    private static final class AddFriendTask extends AsyncTask<String, Void, Void> {
        String mUsername;

        public AddFriendTask(String username) {
            mUsername = username;
        }

        @Override
        protected void onPreExecute() {

        }

        @Override
        protected Void doInBackground(String... params) {

            if (!instance.mRedditClient.isAuthenticated() || !instance.mRedditClient.hasActiveUserContext()) {
                return null;
            }

            try {
                instance.mRateLimiter.acquire();
                net.dean.jraw.models.Account user = instance.mRedditClient.getUser(mUsername);

                if (!user.isFriend()) {
                    net.dean.jraw.managers.AccountManager accountManager = new net.dean.jraw.managers.AccountManager(instance.mRedditClient);
                    instance.mRateLimiter.acquire();
                    accountManager.updateFriend(mUsername);
                }

                ContentValues cv = new ContentValues();
                cv.put(SiftContract.Users.COLUMN_SERVER_ID, user.getId());
                cv.put(SiftContract.Users.COLUMN_USERNAME, mUsername);
                cv.put(SiftContract.Users.COLUMN_DATE_CREATED, user.getCreated().getTime());
                cv.put(SiftContract.Users.COLUMN_COMMENT_KARMA, user.getCommentKarma());
                cv.put(SiftContract.Users.COLUMN_LINK_KARMA, user.getLinkKarma());
                Uri userUri = SiftApplication.getContext().getContentResolver().insert(SiftContract.Users.CONTENT_URI, cv);
                long userId = ContentUris.parseId(userUri);
                cv.clear();

                //add friend
                long accountId = Utilities.getAccountId();
                cv.put(SiftContract.Friends.COLUMN_ACCOUNT_ID, accountId);
                cv.put(SiftContract.Friends.COLUMN_FRIEND_USER_ID, userId);
                SiftApplication.getContext().getContentResolver().insert(SiftContract.Friends.CONTENT_URI, cv);
            } catch (RuntimeException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void nothing) {
            Toast.makeText(SiftApplication.getContext(), SiftApplication.getContext().getString(R.string.friend_added), Toast.LENGTH_LONG).show();
        }
    }


    public static void removeFriend(String username) {
        new RemoveFriendTask(username).execute();
    }

    private static final class RemoveFriendTask extends AsyncTask<String, Void, Void> {
        String mUsername;

        public RemoveFriendTask( String username) {
            mUsername = username;
        }

        @Override
        protected void onPreExecute() {

        }

        @Override
        protected Void doInBackground(String... params) {

            if (!instance.mRedditClient.isAuthenticated() || !instance.mRedditClient.hasActiveUserContext()) {
                return null;
            }

            try {
                instance.mRateLimiter.acquire();
                net.dean.jraw.models.Account user = instance.mRedditClient.getUser(mUsername);
                if (user.isFriend()) {
                    net.dean.jraw.managers.AccountManager accountManager = new net.dean.jraw.managers.AccountManager(instance.mRedditClient);
                    instance.mRateLimiter.acquire();
                    accountManager.deleteFriend(mUsername);
                }

                long userId = Utilities.getSavedUserId(mUsername);

                if (userId > 0) {
                    String selection = SiftContract.Friends.COLUMN_FRIEND_USER_ID + " = ?";
                    String[] selectionArgs = new String[]{String.valueOf(userId)};
                    int count = SiftApplication.getContext().getContentResolver().delete(SiftContract.Friends.CONTENT_URI, selection, selectionArgs);
                    Log.v("Reddit", count + " Removed");
                }
            } catch (RuntimeException e) {
                e.printStackTrace();
            }


            return null;
        }

        @Override
        protected void onPostExecute(Void nothing) {
            Toast.makeText(SiftApplication.getContext(), SiftApplication.getContext().getString(R.string.friend_removed), Toast.LENGTH_LONG).show();
        }
    }

    public static void goToUser(Activity activity, String username) {
        new GoToUserTask(activity, username).execute();
    }

    private static final class GoToUserTask extends AsyncTask<String, Void, Void> {
        String mUsername;
        boolean mUserFound = false;
        Activity mActivity;

        public GoToUserTask(Activity activity, String username) {
            mUsername = username;
            mActivity = activity;
        }

        @Override
        protected void onPreExecute() {

        }

        @Override
        protected Void doInBackground(String... params) {
            if (!instance.mRedditClient.isAuthenticated()) {
                return null;
            }

            try {
                instance.mRateLimiter.acquire();
                instance.mRedditClient.getUser(mUsername);
                mUserFound = true;
            } catch (RuntimeException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void nothing) {
            if (mUserFound) {
                Intent intent = new Intent(SiftApplication.getContext(), UserInfoActivity.class);
                intent.putExtra(SiftApplication.getContext().getString(R.string.username), mUsername);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    mActivity.startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(mActivity).toBundle());
                } else {
                    mActivity.startActivity(intent);
                }
            } else {
                Toast.makeText(SiftApplication.getContext(), SiftApplication.getContext().getString(R.string.user_not_found), Toast.LENGTH_LONG).show();
            }
        }
    }



    public static void goToSubreddit(Activity activity, String subreddit) {
        new GoToSubredditTask(activity, subreddit).execute();
    }

    private static final class GoToSubredditTask extends AsyncTask<String, Void, Void> {
        String mSubreddit;
        boolean mSubredditFound = false;
        Activity mActivity;

        public GoToSubredditTask(Activity activity, String subreddit) {
            mActivity = activity;
            mSubreddit = subreddit;
        }

        @Override
        protected void onPreExecute() {

        }

        @Override
        protected Void doInBackground(String... params) {
            if (!instance.mRedditClient.isAuthenticated()) {
                return null;
            }

            try {
                instance.mRateLimiter.acquire();
                instance.mRedditClient.getSubreddit(mSubreddit);
                mSubredditFound = true;
            } catch (RuntimeException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void nothing) {
            if (mSubredditFound) {
                Intent intent = new Intent(SiftApplication.getContext(), SubredditActivity.class);
                intent.putExtra(SiftApplication.getContext().getString(R.string.subreddit_name), mSubreddit);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    mActivity.startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(mActivity).toBundle());
                } else {
                    mActivity.startActivity(intent);
                }
            } else {
                Toast.makeText(SiftApplication.getContext(), SiftApplication.getContext().getString(R.string.subreddit_not_found), Toast.LENGTH_LONG).show();
            }
        }
    }


    public static void textSubmission(Activity activity, String subreddit, String title, String text, Captcha captcha, String captchaAttempt) {
        new TextSubmissionTask(activity, subreddit, title, text, captcha, captchaAttempt).execute();
    }

    private static final class TextSubmissionTask extends AsyncTask<String, Void, Void> {
        String mSubreddit;
        String mTitle;
        String mText;
        Captcha mCaptcha;
        String mCaptchaAttempt;
        boolean mSubmitted = false;
        Activity mActivity;

        public TextSubmissionTask(Activity activity, String subreddit, String title, String text, Captcha captcha, String captchaAttempt) {
            mSubreddit = subreddit;
            mTitle = title;
            mText = text;
            mCaptcha = captcha;
            mCaptchaAttempt = captchaAttempt;
            mActivity = activity;
        }

        @Override
        protected void onPreExecute() {

        }

        @Override
        protected Void doInBackground(String... params) {
            if (!instance.mRedditClient.isAuthenticated()) {
                return null;
            }

            try {
                net.dean.jraw.managers.AccountManager accountManager = new net.dean.jraw.managers.AccountManager(instance.mRedditClient);
                net.dean.jraw.managers.AccountManager.SubmissionBuilder submission
                        = new net.dean.jraw.managers.AccountManager.SubmissionBuilder(mText, mSubreddit, mTitle);

                if (mCaptcha != null && !TextUtils.isEmpty(mCaptchaAttempt)) {
                    instance.mRateLimiter.acquire();
                    accountManager.submit(submission, mCaptcha, mCaptchaAttempt);
                } else {
                    instance.mRateLimiter.acquire();
                    accountManager.submit(submission);
                }
                mSubmitted = true;
            } catch (RuntimeException | ApiException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void nothing) {
            if (mSubmitted) {
                Toast.makeText(SiftApplication.getContext(), SiftApplication.getContext().getString(R.string.submit_successful), Toast.LENGTH_LONG).show();
                Intent intent = new Intent(SiftApplication.getContext(), MainActivity.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    mActivity.startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(mActivity).toBundle());
                } else {
                    mActivity.startActivity(intent);
                }
            } else {
                Toast.makeText(SiftApplication.getContext(), SiftApplication.getContext().getString(R.string.submit_error), Toast.LENGTH_LONG).show();
            }
        }
    }

    public static void linkSubmission(Activity activity, String subreddit, String title, URL url, Captcha captcha, String captchaAttempt) {
        new LinkSubmissionTask(activity, subreddit, title, url, captcha, captchaAttempt).execute();
    }

    private static final class LinkSubmissionTask extends AsyncTask<String, Void, Void> {
        String mSubreddit;
        String mTitle;
        URL mUrl;
        Captcha mCaptcha;
        String mCaptchaAttempt;
        boolean mSubmitted = false;
        Activity mActivity;

        public LinkSubmissionTask(Activity activity, String subreddit, String title, URL url, Captcha captcha, String captchaAttempt) {
            mSubreddit = subreddit;
            mTitle = title;
            mUrl = url;
            mCaptcha = captcha;
            mCaptchaAttempt = captchaAttempt;
            mActivity = activity;
        }

        @Override
        protected void onPreExecute() {

        }

        @Override
        protected Void doInBackground(String... params) {
            if (!instance.mRedditClient.isAuthenticated()) {
                return null;
            }

            try {
                net.dean.jraw.managers.AccountManager accountManager = new net.dean.jraw.managers.AccountManager(instance.mRedditClient);
                net.dean.jraw.managers.AccountManager.SubmissionBuilder submission
                        = new net.dean.jraw.managers.AccountManager.SubmissionBuilder(mUrl, mSubreddit, mTitle);

                if (mCaptcha != null && !TextUtils.isEmpty(mCaptchaAttempt)) {
                    instance.mRateLimiter.acquire();
                    accountManager.submit(submission, mCaptcha, mCaptchaAttempt);
                } else {
                    instance.mRateLimiter.acquire();
                    accountManager.submit(submission);
                }
                mSubmitted = true;
            } catch (RuntimeException | ApiException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void nothing) {
            if (mSubmitted) {
                Toast.makeText(SiftApplication.getContext(), SiftApplication.getContext().getString(R.string.submit_successful), Toast.LENGTH_LONG).show();
                Intent intent = new Intent(SiftApplication.getContext(), MainActivity.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    mActivity.startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(mActivity).toBundle());
                } else {
                    mActivity.startActivity(intent);
                }
            } else {
                Toast.makeText(SiftApplication.getContext(), SiftApplication.getContext().getString(R.string.submit_error), Toast.LENGTH_LONG).show();
            }
        }
    }



    public static void sendMessage(Activity activity, String to, String subject, String body) {
        new SendMessageTask(activity, to, subject, body).execute();
    }

    private static final class SendMessageTask extends AsyncTask<String, Void, Void> {
        String mTo;
        String mSubject;
        String mBody;
        boolean mSent = false;
        Activity mActivity;

        public SendMessageTask(Activity activity, String to, String subject, String body) {
            mTo = to;
            mSubject = subject;
            mBody = body;
            mActivity = activity;
        }

        @Override
        protected void onPreExecute() {

        }

        @Override
        protected Void doInBackground(String... params) {
            if (!instance.mRedditClient.isAuthenticated()) {
                return null;
            }

            try {
                net.dean.jraw.managers.InboxManager inboxManager = new net.dean.jraw.managers.InboxManager(instance.mRedditClient);
                instance.mRateLimiter.acquire();
                inboxManager.compose(mTo, mSubject, mBody);
                mSent = true;
            } catch (RuntimeException | ApiException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void nothing) {
            if (mSent) {
                Toast.makeText(SiftApplication.getContext(), SiftApplication.getContext().getString(R.string.message_sent), Toast.LENGTH_LONG).show();
                Intent intent = new Intent(SiftApplication.getContext(), MessageActivity.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    mActivity.startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(mActivity).toBundle());
                } else {
                    mActivity.startActivity(intent);
                }
            } else {
                Toast.makeText(SiftApplication.getContext(), SiftApplication.getContext().getString(R.string.message_error), Toast.LENGTH_LONG).show();
            }
        }
    }


    public static String getImageUrl(Submission sub) {
        JsonNode data = sub.getDataNode();
        if (data != null) {
            JsonNode preview = data.findValue("preview");
            if (preview != null) {
                JsonNode images = preview.findValue("images");
                if (images != null) {
                    JsonNode source = images.findValue("source");
                    if (source != null) {
                        List<String> urls = source.findValuesAsText("url");
                        if (urls != null && urls.size() > 0) {
                            return urls.get(0);
                        }
                    }
                }
            }
        }

        return null;
    }
}
