package com.blongdev.sift;

import android.app.ActivityOptions;
import android.app.SearchManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;

public class SearchResultsActivity extends BaseActivity implements SubredditListActivityFragment.Callback {

    private SearchPagerAdapter mSearchPagerAdapter;
    private ViewPager mViewPager;

    private String mSearchTerm;
    private boolean mIsTablet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_results);
        Intent intent = getIntent();
        if (intent != null && Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            mSearchTerm = query;

            ActionBar toolbar = getSupportActionBar();
            if (toolbar != null) {
                toolbar.setTitle(mSearchTerm);
            }
        }

        if (findViewById(R.id.submissions_fragment) != null) {
            mIsTablet = true;

            FragmentManager fm = getSupportFragmentManager();
            SubredditFragment postFrag = new SubredditFragment();
            SubredditListActivityFragment subsFrag = new SubredditListActivityFragment();
            Bundle args = new Bundle();
            args.putString(getString(R.string.search_term), mSearchTerm);
            args.putInt(getString(R.string.paginator_type), SubredditInfo.SUBMISSION_SEARCH_PAGINATOR);
            postFrag.setArguments(args);
            subsFrag.setArguments(args);
            android.support.v4.app.FragmentTransaction ft = fm.beginTransaction();
            ft.replace(R.id.submissions_fragment, postFrag);
            ft.replace(R.id.subreddits_fragment, subsFrag);
            ft.commit();
        } else {

            mSearchPagerAdapter = new SearchPagerAdapter(getSupportFragmentManager());

            mViewPager = (ViewPager) findViewById(R.id.pager);
            mViewPager.setAdapter(mSearchPagerAdapter);

            TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
            tabLayout.setupWithViewPager(mViewPager);
        }

    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            mSearchTerm = query;
        }
    }


    public class SearchPagerAdapter extends FragmentPagerAdapter {

        public SearchPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            Bundle args = new Bundle();
            args.putString(getString(R.string.search_term), mSearchTerm);
            args.putInt(getString(R.string.paginator_type), SubredditInfo.SUBMISSION_SEARCH_PAGINATOR);

            switch (position) {
                case 0:
                    SubredditFragment subFrag = new SubredditFragment();
                    subFrag.setArguments(args);
                    return subFrag;
                case 1:
                    SubredditListActivityFragment subredditList = new SubredditListActivityFragment();
                    subredditList.setArguments(args);
                    return subredditList;
            }
            return null;
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return getString(R.string.submissions);
                case 1:
                    return getString(R.string.subreddits);
            }
            return null;
        }
    }

    @Override
    public void onItemSelected(long id, String name) {
        Intent intent = new Intent(SiftApplication.getContext(), SubredditActivity.class);
        intent.putExtra(getString(R.string.subreddit_id), id);
        intent.putExtra(getString(R.string.subreddit_name), name);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            SearchResultsActivity.this.startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(SearchResultsActivity.this).toBundle());
        } else {
            SearchResultsActivity.this.startActivity(intent);
        }
    }
}
