package com.blongdev.sift;

import android.app.ActivityOptions;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.design.widget.TabLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.SearchView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import net.dean.jraw.models.Account;
import net.dean.jraw.models.Subreddit;

public class UserInfoActivity extends BaseActivity implements LoaderManager.LoaderCallbacks<UserInfo>{

    private UserPagerAdapter mUserPagerAdapter;
    private ViewPager mViewPager;
    private String mUsername;
    private MenuItem mAddFriend;
    private boolean mIsFriend;
    private boolean mIsTablet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_info);

        //Change toolbar title to username
        Intent intent = getIntent();
        mUsername = intent.getStringExtra(getString(R.string.username));
        if (!TextUtils.isEmpty(mUsername)) {
            ActionBar toolbar = getSupportActionBar();
            if (toolbar != null) {
                toolbar.setTitle(mUsername);
            }
        }

        if (findViewById(R.id.link_karma) != null) {
            mIsTablet = true;
            getSupportLoaderManager().initLoader(1, null, this).forceLoad();
        }

        mUserPagerAdapter = new UserPagerAdapter(getSupportFragmentManager(), mUsername);
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mUserPagerAdapter);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);


        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!Utilities.loggedIn()) {
                    Toast.makeText(getApplicationContext(), getString(R.string.must_log_in), Toast.LENGTH_LONG).show();
                    return;
                }

                Intent intent = new Intent(getApplicationContext(), ComposeMessageActivity.class);
                intent.putExtra(getString(R.string.username), mUsername);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    UserInfoActivity.this.startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(UserInfoActivity.this).toBundle());
                } else {
                    UserInfoActivity.this.startActivity(intent);
                }
            }
        });

        if (Utilities.loggedIn()) {
            if (TextUtils.equals(Utilities.getLoggedInUsername(), mUsername)) {
                fab.setVisibility(View.GONE);
            }
        }
    }

    public Loader<UserInfo> onCreateLoader(int id, Bundle args) {
        return new UserSidebarLoader(getApplicationContext(), mUsername);
    }

    @Override
    public void onLoadFinished(Loader<UserInfo> loader, UserInfo user) {
        if (user != null) {
            TextView age = (TextView) findViewById(R.id.age);
            TextView link = (TextView) findViewById(R.id.link_karma);
            TextView comment = (TextView) findViewById(R.id.comment_karma);
            if (age != null) {
                age.setText(getString(R.string.member_for) + " " + Utilities.getAgeStringLong(user.mAge));
            }

            if (link != null) {
                link.setText(getString(R.string.link_karma) + " " + user.mLinkKarma);
            }

            if (comment != null) {
                comment.setText(getString(R.string.comment_karma) + " " + user.mCommentKarma);
            }

        }
    }

    @Override
    public void onLoaderReset(Loader<UserInfo> loader) {

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_user, menu);
        SearchManager searchManager =
                (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView =
                (SearchView) menu.findItem(R.id.menu_search).getActionView();
        searchView.setSearchableInfo(
                searchManager.getSearchableInfo(getComponentName()));

        mAddFriend = (MenuItem) menu.findItem(R.id.subscribe);

        if (Utilities.loggedIn()) {
            if (Utilities.isFriend(mUsername)) {
                mIsFriend = true;
                mAddFriend.setIcon(R.drawable.ic_favorite_24dp);
            }
        }

        mAddFriend.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (!Utilities.loggedIn()) {
                    Toast.makeText(getApplicationContext(), getApplicationContext().getString(R.string.must_log_in), Toast.LENGTH_LONG).show();
                    return false;
                }

                if (mIsFriend) {
                    mIsFriend = false;
                    mAddFriend.setIcon(R.drawable.ic_favorite_outline_24dp);
                    Reddit.getInstance().removeFriend(mUsername);
                } else {
                    mIsFriend = true;
                    mAddFriend.setIcon(R.drawable.ic_favorite_24dp);
                    Reddit.getInstance().addFriend(mUsername);
                }
                return true;
            }
        });

        //hide friend icon on my profile
        if (TextUtils.equals(Utilities.getLoggedInUsername(), mUsername)) {
            mAddFriend.setVisible(false);
        }

        return true;
    }

}

class UserSidebarLoader extends AsyncTaskLoader<UserInfo> {

    String mName;

    public UserSidebarLoader(Context context, String username) {
        super(context);
        mName = username;
    }

    @Override
    public UserInfo loadInBackground() {
        Reddit reddit = Reddit.getInstance();
        if (!reddit.mRedditClient.isAuthenticated()) {
            return null;
        }

        try {
            reddit.mRateLimiter.acquire();
            Account user = reddit.mRedditClient.getUser(mName);
            UserInfo userInfo = new UserInfo();
            userInfo.mLinkKarma = user.getLinkKarma();
            userInfo.mCommentKarma = user.getCommentKarma();
            userInfo.mAge = user.getCreatedUtc().getTime();
            return userInfo;
        } catch (RuntimeException e) {
            e.printStackTrace();
        }

        return null;
    }
}
