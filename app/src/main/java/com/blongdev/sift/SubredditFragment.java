package com.blongdev.sift;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.blongdev.sift.database.SiftContract;

import net.dean.jraw.models.Comment;
import net.dean.jraw.models.Contribution;
import net.dean.jraw.models.Listing;
import net.dean.jraw.models.Submission;
import net.dean.jraw.paginators.Paginator;
import net.dean.jraw.paginators.SubmissionSearchPaginator;
import net.dean.jraw.paginators.SubredditPaginator;
import net.dean.jraw.paginators.TimePeriod;
import net.dean.jraw.paginators.UserContributionPaginator;

import java.util.ArrayList;
import java.util.List;


public class SubredditFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>, PostListAdapter.Callback {

    Activity mActivity;
    private long mSubredditId;
    private String mSubredditName;
    private List<ContributionInfo> mPosts;
    private PostListAdapter mPostListAdapter;
    private RecyclerView mRecyclerView;
    private ContentResolver mContentResolver;
    private ProgressBar mLoadingSpinner;

    private Reddit mReddit;
    private Paginator mPaginator;
    private boolean mLoading;
    private LinearLayoutManager mLayoutManager;

    private int mPaginatorType;
    private String mSearchTerm;
    private String mUsername;
    private String mCategory;
    protected static final int PAGE_SIZE = 25;
    private TextView mEmptyText;
    private int mRefreshPoint = 10;
    private boolean savePosts;

    private static final int CURSOR_LOADER_ID = 1;
    private static final int ASYNCTASK_LOADER_ID = 2;

    private LoaderManager.LoaderCallbacks<List<ContributionInfo>> mPostsLoaderCallbacks
            = new LoaderManager.LoaderCallbacks<List<ContributionInfo>>() {

        @Override
        public Loader<List<ContributionInfo>> onCreateLoader(int id, Bundle args) {
            mLoadingSpinner.setVisibility(View.VISIBLE);
            return new ContributionLoader(getContext(), mPaginator, savePosts, mSubredditId);
        }

        @Override
        public void onLoadFinished(Loader<List<ContributionInfo>> loader, List<ContributionInfo> data) {
            mLoadingSpinner.setVisibility(View.GONE);
            mPostListAdapter.refreshWithList(data);
            if (data.size() >= PAGE_SIZE) {
                mRefreshPoint = data.size() - PAGE_SIZE;
                if (mRefreshPoint < 10) {
                    mRefreshPoint = 10;
                }
            }

            if (data.size() == 0) {
                mEmptyText.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void onLoaderReset(Loader<List<ContributionInfo>> loader) {
            if(mPostListAdapter != null) {
                mPostListAdapter.refreshWithList(new ArrayList<ContributionInfo>());
            }
        }

    };

    public SubredditFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        mContentResolver = SiftApplication.getContext().getContentResolver();
        mActivity = getActivity();

        mLoadingSpinner = (ProgressBar) rootView.findViewById(R.id.progressSpinner);
        mEmptyText = (TextView) rootView.findViewById(R.id.empty);

        mReddit = Reddit.getInstance();
        mPosts = new ArrayList<ContributionInfo>();

        Bundle arg = getArguments();
        if (arg != null) {
            mSubredditId = arg.getLong(getString(R.string.subreddit_id));
            mSubredditName = arg.getString(getString(R.string.subreddit_name));
            mPaginatorType = arg.getInt(getString(R.string.paginator_type), SubredditInfo.SUBREDDIT_PAGINATOR);
            mSearchTerm = arg.getString(getString(R.string.search_term));
            mUsername = arg.getString(getString(R.string.username));
            mCategory = arg.getString(getString(R.string.category));
        } else {
            Intent intent = getActivity().getIntent();
            mSubredditName = intent.getStringExtra(getString(R.string.subreddit_name));
            mSubredditId = intent.getLongExtra(getString(R.string.subreddit_id), 0);
            mPaginatorType = intent.getIntExtra(getString(R.string.paginator_type), SubredditInfo.SUBREDDIT_PAGINATOR);
            mSearchTerm = intent.getStringExtra(getString(R.string.search_term));
            mUsername = intent.getStringExtra(getString(R.string.username));
            mCategory = intent.getStringExtra(getString(R.string.category));
        }

        setPaginator();

        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.cardList);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setNestedScrollingEnabled(true);
        mLayoutManager = new LinearLayoutManager(getActivity());
        mLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(mLayoutManager);

        mPostListAdapter = new PostListAdapter(mPosts, this);
        mRecyclerView.setAdapter(mPostListAdapter);

        return rootView;
    }

    public void setPaginator() {
        if (mPaginatorType == SubredditInfo.SUBMISSION_SEARCH_PAGINATOR && !TextUtils.isEmpty(mSearchTerm)) {
            mPaginator = new SubmissionSearchPaginator(mReddit.mRedditClient, mSearchTerm);
            savePosts = false;
            //have to use setSearchSorting for submissionSearchPaginator;
        } else if (mPaginatorType == SubredditInfo.USER_CONTRIBUTION_PAGINATOR && !TextUtils.isEmpty(mUsername) && !TextUtils.isEmpty(mCategory)) {
            mPaginator = new UserContributionPaginator(mReddit.mRedditClient, mCategory, mUsername);
            savePosts = false;
            mPaginator.setSorting(mReddit.mSort);
        } else {
            if (TextUtils.equals(mSubredditName, getString(R.string.frontPage))) {
                mPaginator = new SubredditPaginator(mReddit.mRedditClient);
            } else {
                mPaginator = new SubredditPaginator(mReddit.mRedditClient, mSubredditName);
            }
            savePosts = true;

            mPaginator.setSorting(mReddit.mSort);
        }
        mPaginator.setTimePeriod(mReddit.mTime);
        mPaginator.setLimit(PAGE_SIZE);

        if (savePosts) {
            getLoaderManager().initLoader(CURSOR_LOADER_ID, null, this);
        }

        if (Utilities.connectedToNetwork()) {
            if (mReddit.mRedditClient.isAuthenticated()) {
                getLoaderManager().initLoader(ASYNCTASK_LOADER_ID, null, mPostsLoaderCallbacks).forceLoad();
            } else {
                //Base Activity is authenticating. add spinner until authenticated
                mLoadingSpinner.setVisibility(View.VISIBLE);
            }
        }
    }

    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String selection = SiftContract.Posts.COLUMN_SUBREDDIT_ID + " = ?";
        String[] selectionArgs = new String[]{String.valueOf(mSubredditId)};
        return new CursorLoader(getActivity(), SiftContract.Posts.CONTENT_URI,
                null, selection, selectionArgs,
                SiftContract.Posts.COLUMN_POSITION);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (cursor != null) {
            mPosts.clear();
            while (cursor.moveToNext()) {
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
                mPosts.add(post);
            }
        }

        mPostListAdapter.refreshWithList(mPosts);
    }

    public void onLoaderReset(Loader<Cursor> loader) {
        if (mPostListAdapter != null) {
            mPostListAdapter.refreshWithList(new ArrayList<ContributionInfo>());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mRecyclerView.addOnScrollListener(mOnScrollistener);

        IntentFilter filter = new IntentFilter();
        filter.addAction(getString(R.string.sortChanged));
        mActivity.registerReceiver(mReceiver, filter);
    }

    @Override
    public void onPause() {
        mRecyclerView.removeOnScrollListener(mOnScrollistener);
        mActivity.unregisterReceiver(mReceiver);
        super.onPause();
    }

    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            setPaginator();

            if (savePosts) {
                getLoaderManager().restartLoader(CURSOR_LOADER_ID, null, SubredditFragment.this);
            }

            if (Utilities.connectedToNetwork()) {
                if (mReddit.mRedditClient.isAuthenticated()) {
                    getLoaderManager().restartLoader(ASYNCTASK_LOADER_ID, null, mPostsLoaderCallbacks).forceLoad();
                } else {
                    mLoadingSpinner.setVisibility(View.VISIBLE);
                }
            }
        }
    };

    RecyclerView.OnScrollListener mOnScrollistener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);

            if (dy > 0 && mLayoutManager.findLastVisibleItemPosition() >= mRefreshPoint && !mLoading) {
                if (Utilities.connectedToNetwork()) {
                    if (mReddit.mRedditClient.isAuthenticated()) {
                        getLoaderManager().initLoader(ASYNCTASK_LOADER_ID, null, mPostsLoaderCallbacks).forceLoad();
                    }
                }
            }
        }
    };

    @Override
    public void onDestroy() {
        mRecyclerView.clearOnScrollListeners();
        mRecyclerView.setAdapter(null);
        mRecyclerView = null;
        mPostListAdapter = null;
        super.onDestroy();
    }

    @Override
    public void onItemSelected(Intent intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mActivity.startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(mActivity).toBundle());
        } else {
            mActivity.startActivity(intent);
        }
    }
}


class ContributionLoader extends AsyncTaskLoader<List<ContributionInfo>> {
    public Paginator paginator;
    ArrayList<ContributionInfo> posts;
    boolean savePosts;
    long subredditId;

    public ContributionLoader(Context context, Paginator p, boolean save, long subId) {
        super(context);
        paginator = p;
        savePosts = save;
        subredditId = subId;
        posts = new ArrayList<ContributionInfo>();
    }

    @Override
    public List<ContributionInfo> loadInBackground() {
        Reddit reddit = Reddit.getInstance();
        ArrayList<ContributionInfo> newPostArray = new ArrayList<ContributionInfo>();
        if (!reddit.mRedditClient.isAuthenticated() || !Utilities.connectedToNetwork())
            return newPostArray;

        try {
            if (paginator != null && paginator.hasNext()) {
                reddit.mRateLimiter.acquire();
                Listing<Contribution> page = paginator.next();
                int i = 0;
                for (Contribution contribution : page) {
                    if (contribution instanceof Comment) {
                        Comment comment = (Comment) contribution;
                        CommentInfo commentInfo = new CommentInfo();
                        commentInfo.mUsername = comment.getAuthor();
                        commentInfo.mPoints = comment.getScore();
                        commentInfo.mBody = comment.getBody();
                        commentInfo.mAge = comment.getCreatedUtc().getTime();
                        commentInfo.mContributionType = ContributionInfo.CONTRIBUTION_COMMENT;
                        commentInfo.mVote = comment.getVote().getValue();
                        commentInfo.mJrawComment = comment;
                        commentInfo.mPostServerId = Utilities.getServerIdFromFullName(comment.getSubmissionId());
                        newPostArray.add(commentInfo);
                        posts.add(commentInfo);
                    } else {
                        Submission submission = (Submission) contribution;
                        PostInfo post = new PostInfo();
                        post.mServerId = submission.getId();
                        post.mTitle = submission.getTitle();
                        post.mUsername = submission.getAuthor();
                        post.mSubreddit = submission.getSubredditName();
                        post.mSubredditId = subredditId;
                        post.mPoints = submission.getScore();
                        post.mUrl = submission.getUrl();
                        post.mImageUrl = Reddit.getImageUrl(submission);
                        post.mComments = submission.getCommentCount();
                        post.mBody = submission.getSelftext();
                        post.mDomain = submission.getDomain();
                        post.mAge = submission.getCreatedUtc().getTime();
                        post.mPosition = ((paginator.getPageIndex() - 1) * SubredditFragment.PAGE_SIZE) + i;
                        post.mContributionType = ContributionInfo.CONTRIBUTION_POST;
                        post.mVote = submission.getVote().getValue();
                        post.mFavorited = submission.isSaved();
                        newPostArray.add(post);
                        posts.add(post);
                    }
                    i++;
                }
            }
        } catch (RuntimeException e) {
            e.printStackTrace();
        }

        if(savePosts) {
            new AddPostsToDbTask(newPostArray).execute();
        }

        return posts;
    }
}

final class AddPostsToDbTask extends AsyncTask<String, Void, Void> {

    private ArrayList<ContributionInfo> mNewPosts;

    public AddPostsToDbTask(ArrayList<ContributionInfo> posts) {
        mNewPosts = posts;
    }

    @Override
    protected Void doInBackground(String... params) {
        //add to db
        for (ContributionInfo post : mNewPosts) {
            if (post.mContributionType != ContributionInfo.CONTRIBUTION_COMMENT) {
                addPostToDb((PostInfo) post);
            }
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void nothing) {

    }

    private void addPostToDb(PostInfo post) {
        ContentValues cv = new ContentValues();
        cv.put(SiftContract.Posts.COLUMN_OWNER_USERNAME, post.mUsername);
        cv.put(SiftContract.Posts.COLUMN_SERVER_ID, post.mServerId);
        cv.put(SiftContract.Posts.COLUMN_NUM_COMMENTS, post.mComments);
        cv.put(SiftContract.Posts.COLUMN_POINTS, post.mPoints);
        cv.put(SiftContract.Posts.COLUMN_SUBREDDIT_ID, post.mSubredditId);
        cv.put(SiftContract.Posts.COLUMN_SUBREDDIT_NAME, post.mSubreddit);
        cv.put(SiftContract.Posts.COLUMN_URL, post.mUrl);
        cv.put(SiftContract.Posts.COLUMN_IMAGE_URL, post.mImageUrl);
        cv.put(SiftContract.Posts.COLUMN_TITLE, post.mTitle);
        cv.put(SiftContract.Posts.COLUMN_BODY, post.mBody);
        cv.put(SiftContract.Posts.COLUMN_DATE_CREATED, post.mAge);
        cv.put(SiftContract.Posts.COLUMN_DOMAIN, post.mDomain);
        cv.put(SiftContract.Posts.COLUMN_POSITION, post.mPosition);
        cv.put(SiftContract.Posts.COLUMN_SERVER_ID, post.mServerId);
        cv.put(SiftContract.Posts.COLUMN_VOTE, post.mVote);
        cv.put(SiftContract.Posts.COLUMN_FAVORITED, post.mFavorited);
        SiftApplication.getContext().getContentResolver().insert(SiftContract.Posts.CONTENT_URI, cv);
    }
}
