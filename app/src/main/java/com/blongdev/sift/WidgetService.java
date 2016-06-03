package com.blongdev.sift;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;
import com.blongdev.sift.database.SiftContract;
import java.util.ArrayList;

/**
 * Created by Brian on 4/21/2016.
 */
public class WidgetService extends RemoteViewsService {

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        int appWidgetId = intent.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);

        return (new ListProvider(this.getApplicationContext(), intent));
    }

    public class ListProvider implements RemoteViewsService.RemoteViewsFactory {
        private ArrayList<PostInfo> mList = new ArrayList<PostInfo>();
        private Context context = null;
        private int appWidgetId;

        private static final int LIMIT = 25;
        public static final String GO_TO_POST_DETAIL = "GoToPostDetail";

        public ListProvider(Context context, Intent intent) {
            this.context = context;
            appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);

            mList = getPosts(context);
        }

        public ArrayList<PostInfo> getPosts(Context context) {
            ArrayList<PostInfo> array = new ArrayList<PostInfo>();

            String selection = SiftContract.Posts.COLUMN_SUBREDDIT_ID + " = -1";
            String orderBy = SiftContract.Posts.COLUMN_POSITION;
            Cursor cursor = null;
            try {
                cursor = context.getContentResolver().query(SiftContract.Posts.CONTENT_URI, null, selection, null, orderBy);
                if (cursor != null) {
                    while (cursor.moveToNext() && cursor.getPosition() < LIMIT) {
                        PostInfo post = new PostInfo();

                        post.mId = cursor.getLong(cursor.getColumnIndex(SiftContract.Posts._ID));
                        post.mTitle = cursor.getString(cursor.getColumnIndex(SiftContract.Posts.COLUMN_TITLE));
                        post.mUsername = cursor.getString(cursor.getColumnIndex(SiftContract.Posts.COLUMN_OWNER_USERNAME));
                        post.mUserId = cursor.getLong(cursor.getColumnIndex(SiftContract.Posts.COLUMN_OWNER_ID));
                        post.mSubreddit = cursor.getString(cursor.getColumnIndex(SiftContract.Posts.COLUMN_SUBREDDIT_NAME));
                        post.mSubredditId = cursor.getLong(cursor.getColumnIndex(SiftContract.Posts.COLUMN_SUBREDDIT_ID));
                        post.mPoints = cursor.getInt(cursor.getColumnIndex(SiftContract.Posts.COLUMN_POINTS));
                        post.mImageUrl = cursor.getString(cursor.getColumnIndex(SiftContract.Posts.COLUMN_IMAGE_URL));
                        post.mUrl = cursor.getString(cursor.getColumnIndex(SiftContract.Posts.COLUMN_URL));
                        post.mComments = cursor.getInt(cursor.getColumnIndex(SiftContract.Posts.COLUMN_NUM_COMMENTS));
                        post.mAge = cursor.getLong(cursor.getColumnIndex(SiftContract.Posts.COLUMN_DATE_CREATED));
                        post.mFavorited = cursor.getInt(cursor.getColumnIndex(SiftContract.Posts.COLUMN_FAVORITED)) == 1 ? true : false;
                        post.mBody = cursor.getString(cursor.getColumnIndex(SiftContract.Posts.COLUMN_BODY));
                        post.mServerId =cursor.getString(cursor.getColumnIndex(SiftContract.Posts.COLUMN_SERVER_ID));
                        post.mDomain = cursor.getString(cursor.getColumnIndex(SiftContract.Posts.COLUMN_DOMAIN));
                        post.mPosition = cursor.getInt(cursor.getColumnIndex(SiftContract.Posts.COLUMN_POSITION));
                        post.mVote = cursor.getInt(cursor.getColumnIndex(SiftContract.Posts.COLUMN_VOTE));

                        array.add(post);
                    }
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }

            return array;
        }

        @Override
        public int getCount() {
            return mList.size();
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public RemoteViews getViewAt(int position) {
            final RemoteViews remoteView = new RemoteViews(
                    context.getPackageName(), R.layout.sift_widget_item);
            PostInfo post = mList.get(position);

            remoteView.setTextViewText(R.id.post_title, post.mTitle);
            remoteView.setTextViewText(R.id.post_username, post.mUsername);
            remoteView.setTextViewText(R.id.post_subreddit, post.mSubreddit);
            remoteView.setTextViewText(R.id.post_points, String.valueOf(post.mPoints));
            remoteView.setTextViewText(R.id.post_comments, post.mComments + " Comments");
            remoteView.setTextViewText(R.id.post_url, post.mDomain);
            remoteView.setTextViewText(R.id.post_age, Utilities.getAgeString(post.mAge));

            Intent intent = new Intent(context, SiftWidget.class);
            intent.putExtra(context.getString(R.string.title), post.mTitle);
            intent.putExtra(context.getString(R.string.username), post.mUsername);
            intent.putExtra(context.getString(R.string.subreddit), post.mSubreddit);
            intent.putExtra(context.getString(R.string.points), String.valueOf(post.mPoints));
            intent.putExtra(context.getString(R.string.comments), post.mComments);
            intent.putExtra(context.getString(R.string.url), post.mUrl);
            intent.putExtra(context.getString(R.string.age), Utilities.getAgeString(post.mAge));
            intent.putExtra(context.getString(R.string.image_url), post.mImageUrl);
            intent.putExtra(context.getString(R.string.post_id), post.mId);
            intent.putExtra(context.getString(R.string.server_id), post.mServerId);
            intent.putExtra(context.getString(R.string.body), post.mBody);
            intent.putExtra(context.getString(R.string.domain), post.mDomain);
            intent.putExtra(context.getString(R.string.vote), post.mVote);
            remoteView.setOnClickFillInIntent(R.id.post_title, intent);

            return remoteView;
        }


        @Override
        public RemoteViews getLoadingView() {
            return null;
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public void onCreate() {
            mList = getPosts(context);
        }

        @Override
        public void onDataSetChanged() {
            mList = getPosts(context);
        }

        @Override
        public void onDestroy() {
        }

    }
}