package com.blongdev.sift;

import android.app.ActivityOptions;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;

public class SubredditListActivity extends BaseActivity implements SubredditListActivityFragment.Callback {

    FragmentManager mFragmentManager;
    boolean mIsTablet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subreddit_list);
        mFragmentManager = getSupportFragmentManager();
        if (findViewById(R.id.posts_fragment) != null) {
            mIsTablet = true;
        }

        String category = null;
        Intent intent = getIntent();
        if (intent != null) {
            category = intent.getStringExtra(getString(R.string.category));
        }

        if(mIsTablet) {
            ActionBar toolbar = getSupportActionBar();
            if (toolbar != null) {
                toolbar.setTitle(getString(R.string.sift));
            }

            if (savedInstanceState == null) {
                SubredditListActivityFragment subListFrag = new SubredditListActivityFragment();

                Bundle args = new Bundle();
                args.putBoolean(getString(R.string.isTablet), true);
                if (!Utilities.loggedIn() || !TextUtils.isEmpty(category) ||
                        (mReddit.mRedditClient.isAuthenticated() && !mReddit.mRedditClient.hasActiveUserContext())) {
                    args.putBoolean(getString(R.string.popular), true);

                }
                subListFrag.setArguments(args);
                android.support.v4.app.FragmentTransaction ft = mFragmentManager.beginTransaction();
                ft.replace(R.id.fragment, subListFrag);
                ft.commit();
            }
        }

    }

    @Override
    public void onItemSelected(long id, String name) {
        if (mIsTablet) {
            SubredditFragment subFrag = new SubredditFragment();
            Bundle args = new Bundle();
            args.putLong(getString(R.string.subreddit_id), id);
            args.putString(getString(R.string.subreddit_name), name);
            subFrag.setArguments(args);
            android.support.v4.app.FragmentTransaction ft = mFragmentManager.beginTransaction();
            ft.replace(R.id.posts_fragment, subFrag);
            ft.commitAllowingStateLoss();
        } else {
            Intent intent = new Intent(SiftApplication.getContext(), SubredditActivity.class);
            intent.putExtra(getString(R.string.subreddit_id), id);
            intent.putExtra(getString(R.string.subreddit_name), name);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                SubredditListActivity.this.startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(SubredditListActivity.this).toBundle());
            } else {
                SubredditListActivity.this.startActivity(intent);
            }
        }
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mReddit.mRedditClient.isAuthenticated()) {
                if (!Utilities.loggedIn()) {
                    recreate();
                }
            }
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver,
                new IntentFilter(Reddit.AUTHENTICATED));
    }

    @Override
    public void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
        super.onPause();
    }
}
