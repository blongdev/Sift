package com.blongdev.sift;

import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import com.blongdev.sift.database.SiftContract;

public class MessageActivity extends BaseActivity implements MessageActivityFragment.Callback {

    private SectionsPagerAdapter mSectionsPagerAdapter;
    private ViewPager mViewPager;
    private MessageDetailActivityFragment mMessageDetailFragment;
    private FragmentManager mFragmentManager;
    boolean mIsTablet;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message_list);

        if(findViewById(R.id.detail_fragment) != null) {
            mIsTablet = true;
        }

        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        mViewPager = (ViewPager) findViewById(R.id.message_container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);

        mFragmentManager = getSupportFragmentManager();

    }


    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            Bundle args = new Bundle();

            MessageActivityFragment messageFrag = new MessageActivityFragment();

            switch (position) {
                case 0:
                    args.putInt(getString(R.string.mailbox), SiftContract.Messages.MAILBOX_TYPE_INBOX);
                    if (mIsTablet) {
                        args.putBoolean(getString(R.string.selectFirst), true);
                    }
                    break;
                case 1:
                    args.putInt(getString(R.string.mailbox), SiftContract.Messages.MAILBOX_TYPE_INBOX);
                    args.putBoolean(getString(R.string.unread), true);
                    break;
                case 2:
                    args.putInt(getString(R.string.mailbox), SiftContract.Messages.MAILBOX_TYPE_SENT);
                    break;
                case 3:
                    args.putInt(getString(R.string.mailbox), SiftContract.Messages.MAILBOX_TYPE_MENTIONS);
                    break;
            }


            if (mIsTablet) {
                args.putBoolean(getString(R.string.isTablet), true);
            }

            messageFrag.setArguments(args);
            return messageFrag;

        }

        @Override
        public int getCount() {
            return 4;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return getString(R.string.inbox);
                case 1:
                    return getString(R.string.unread);
                case 2:
                    return getString(R.string.sent);
                case 3:
                    return getString(R.string.mentions);
            }
            return null;
        }
    }

    @Override
    public void onItemSelected(String title, String body, String from) {
        if(mIsTablet) {
            mMessageDetailFragment = new MessageDetailActivityFragment();
            Bundle args = new Bundle();
            args.putString(getString(R.string.title), title);
            args.putString(getString(R.string.body), body);
            args.putString(getString(R.string.from), from);
            mMessageDetailFragment.setArguments(args);
            android.support.v4.app.FragmentTransaction ft = mFragmentManager.beginTransaction();
            ft.replace(R.id.detail_fragment, mMessageDetailFragment);
            ft.commitAllowingStateLoss();
        } else {
            Intent intent = new Intent(SiftApplication.getContext(), MessageDetailActivity.class);
            intent.putExtra(getString(R.string.from), from);
            intent.putExtra(getString(R.string.title), title);
            intent.putExtra(getString(R.string.body), body);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(MessageActivity.this).toBundle());
            } else {
                startActivity(intent);
            }
        }
    }
}
