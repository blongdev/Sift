package com.blongdev.sift;

import android.app.ActivityOptions;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import net.dean.jraw.ApiException;
import net.dean.jraw.models.Captcha;
import net.dean.jraw.models.Subreddit;

public class SubredditActivity extends BaseActivity implements LoaderManager.LoaderCallbacks<String>{

    MenuItem mSubscribe;
    String mSubredditName;
    boolean mSubscribed;
    boolean mIsTablet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subreddit);

        //Change toolbar title to username
        Intent intent = getIntent();
        mSubredditName = intent.getStringExtra(getString(R.string.subreddit_name));
        if (!TextUtils.isEmpty(mSubredditName)) {
            ActionBar toolbar = getSupportActionBar();
            if (toolbar != null) {
                toolbar.setTitle(mSubredditName);
            }
        }

        if (findViewById(R.id.description) != null) {
            mIsTablet = true;
            getSupportLoaderManager().initLoader(1, null, this).forceLoad();
        }

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!Utilities.loggedIn()) {
                    Toast.makeText(getApplicationContext(), getString(R.string.must_log_in), Toast.LENGTH_LONG).show();
                    return;
                }

                Intent intent = new Intent(getApplicationContext(), ComposePostActivity.class);
                intent.putExtra(getString(R.string.subreddit_name), mSubredditName);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    SubredditActivity.this.startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(SubredditActivity.this).toBundle());
                } else {
                    SubredditActivity.this.startActivity(intent);
                }
            }
        });
    }

    public Loader<String> onCreateLoader(int id, Bundle args) {
        return new SidebarLoader(getApplicationContext(), mSubredditName);
    }

    @Override
    public void onLoadFinished(Loader<String> loader, String sidebar) {
        if (!TextUtils.isEmpty(sidebar)) {
            TextView desc = (TextView) findViewById(R.id.description);
            if (desc != null) {
                desc.setText(sidebar);
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<String> loader) {

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_subreddit, menu);
        SearchManager searchManager =
                (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView =
                (SearchView) menu.findItem(R.id.menu_search).getActionView();
        searchView.setSearchableInfo(
                searchManager.getSearchableInfo(getComponentName()));

        mSubscribe = (MenuItem) menu.findItem(R.id.subscribe);

        if (mReddit.mRedditClient.isAuthenticated() && mReddit.mRedditClient.hasActiveUserContext()) {
            if (!TextUtils.isEmpty(mSubredditName)) {
                long subscriptionId = Utilities.getSubscriptionId(mSubredditName);
                if (subscriptionId > 0) {
                    mSubscribed = true;
                    mSubscribe.setIcon(R.drawable.ic_favorite_24dp);
                }
            }
        }

        mSubscribe.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (!Utilities.loggedIn()) {
                    Toast.makeText(getApplicationContext(), getApplicationContext().getString(R.string.must_log_in), Toast.LENGTH_LONG).show();
                    return false;
                }

                if (mSubscribed) {
                    mSubscribed = false;
                    mSubscribe.setIcon(R.drawable.ic_favorite_outline_24dp);
                    Reddit.getInstance().unsubscribe(mSubredditName);
                    Toast.makeText(getApplicationContext(), getApplicationContext().getString(R.string.unsubscribed), Toast.LENGTH_LONG).show();
                } else {
                    mSubscribed = true;
                    mSubscribe.setIcon(R.drawable.ic_favorite_24dp);
                    Reddit.getInstance().subscribe(mSubredditName);
                    Toast.makeText(getApplicationContext(), getApplicationContext().getString(R.string.subscribed), Toast.LENGTH_LONG).show();
                }
                return true;
            }
        });

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_sort:
                showSortFragment();
                return true;
            case R.id.menu_time:
                showTimeFragment();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void showSortFragment() {
        FragmentManager fm = getSupportFragmentManager();
        SortFragment sortFragment = new SortFragment();
        sortFragment.show(fm, getString(R.string.sort));
    }

    public void showTimeFragment() {
        FragmentManager fm = getSupportFragmentManager();
        TimeFragment timeFragment = new TimeFragment();
        timeFragment.show(fm, getString(R.string.time));
    }
}

class SidebarLoader extends AsyncTaskLoader<String> {

    String mName;

    public SidebarLoader(Context context, String subredditName) {
        super(context);
        mName = subredditName;
    }

    @Override
    public String loadInBackground() {
        Reddit reddit = Reddit.getInstance();
        if (!reddit.mRedditClient.isAuthenticated()) {
            return null;
        }

        try {
            reddit.mRateLimiter.acquire();
            Subreddit sub = reddit.mRedditClient.getSubreddit(mName);
            return sub.getSidebar();
        } catch (RuntimeException e) {
            e.printStackTrace();
        }

        return null;
    }
}
