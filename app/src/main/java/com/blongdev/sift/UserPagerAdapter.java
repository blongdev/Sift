package com.blongdev.sift;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.text.TextUtils;

/**
 * Created by Brian on 5/16/2016.
 */
public class UserPagerAdapter extends FragmentStatePagerAdapter {

    private String mUsername;

    public UserPagerAdapter(FragmentManager fm, String username) {
        super(fm);
        mUsername = username;
    }

    public UserPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    public void setUser (String username) {
        mUsername = username;
        notifyDataSetChanged();
    }

    @Override
    public Fragment getItem(int position) {
        Bundle args = new Bundle();
        args.putString(SiftApplication.getContext().getString(R.string.username), mUsername);
        args.putInt(SiftApplication.getContext().getString(R.string.paginator_type), SubredditInfo.USER_CONTRIBUTION_PAGINATOR);

        switch (position) {
            case 0:
                args.putString(SiftApplication.getContext().getString(R.string.category), SiftApplication.getContext().getString(R.string.overview));
                SubredditFragment overviewFrag = new SubredditFragment();
                overviewFrag.setArguments(args);
                return overviewFrag;
            case 1:
                args.putString(SiftApplication.getContext().getString(R.string.category), SiftApplication.getContext().getString(R.string.submitted));
                SubredditFragment subFrag = new SubredditFragment();
                subFrag.setArguments(args);
                return subFrag;
            case 2:
                args.putString(SiftApplication.getContext().getString(R.string.category), SiftApplication.getContext().getString(R.string.comments));
                SubredditFragment commentsFrag = new SubredditFragment();
                commentsFrag.setArguments(args);
                return commentsFrag;
            case 3:
                args.putString(SiftApplication.getContext().getString(R.string.category), SiftApplication.getContext().getString(R.string.gilded));
                SubredditFragment gildedFrag = new SubredditFragment();
                gildedFrag.setArguments(args);
                return gildedFrag;
            case 4:
                args.putString(SiftApplication.getContext().getString(R.string.category), SiftApplication.getContext().getString(R.string.saved));
                SubredditFragment savedFrag = new SubredditFragment();
                savedFrag.setArguments(args);
                return savedFrag;
        }
        return null;
    }

    @Override
    public int getCount() {
        if (Utilities.loggedIn()) {
            if (TextUtils.equals(Utilities.getLoggedInUsername(), mUsername)) {
                return 5;
            }
        }
        return 4;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        switch (position) {
            case 0:
                return SiftApplication.getContext().getString(R.string.overview);
            case 1:
                return SiftApplication.getContext().getString(R.string.submissions);
            case 2:
                return SiftApplication.getContext().getString(R.string.comments);
            case 3:
                return SiftApplication.getContext().getString(R.string.gilded);
            case 4:
                return SiftApplication.getContext().getString(R.string.saved);
        }
        return null;
    }
}