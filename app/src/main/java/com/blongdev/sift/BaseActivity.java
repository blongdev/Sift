package com.blongdev.sift;

import android.app.ActivityOptions;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.design.widget.NavigationView;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import com.blongdev.sift.database.SiftContract;
import com.google.android.gms.analytics.Tracker;

public class BaseActivity extends AppCompatActivity {

    Reddit mReddit;
    private DrawerLayout mDrawerLayout;
    private NavigationView mNavigationView;
    View mNavHeader;
    Menu mNavMenu;
    MenuItem mNavProfile;
    MenuItem mNavInbox;
    MenuItem mNavFriends;
    MenuItem mMySubreddits;
    MenuItem mPopular;
    String mUsername;
    boolean mHasUser;
    View mRemoveAccount;
    Tracker mTracker;
    TextView mNavUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SiftApplication application = (SiftApplication) getApplication();
        mTracker = application.getDefaultTracker();
        mTracker.enableExceptionReporting(true);

        mReddit = Reddit.getInstance();

    }

    @Override
    protected void onResume(){
        super.onResume();
        if ((!mReddit.mRedditClient.isAuthenticated() || System.currentTimeMillis() > mReddit.mRefreshTime) && Utilities.connectedToNetwork()) {
            mReddit.refreshKey();
        }
    }

    protected void onCreateDrawer() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mNavigationView = (NavigationView) findViewById(R.id.navigation_view);
        mNavHeader = mNavigationView.inflateHeaderView(R.layout.nav_header);
        mNavMenu = mNavigationView.getMenu();
        mNavFriends = mNavMenu.findItem(R.id.nav_friends);
        mNavProfile = mNavMenu.findItem(R.id.nav_profile);
        mNavInbox = mNavMenu.findItem(R.id.nav_inbox);
        mMySubreddits = mNavMenu.findItem(R.id.nav_subreddits);
        mPopular = mNavMenu.findItem(R.id.nav_popular);
        mRemoveAccount = mNavHeader.findViewById(R.id.remove_account);
        mNavUser = (TextView) mNavHeader.findViewById(R.id.nav_username);

        mNavHeader.setFocusable(true);
        mNavUser.setOnFocusChangeListener(mTextFocusListener);

        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(SiftContract.Accounts.CONTENT_URI, null, null, null, null);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    mUsername = cursor.getString(cursor.getColumnIndex(SiftContract.Accounts.COLUMN_USERNAME));
                    if (!TextUtils.isEmpty(mUsername)) {
                        mNavUser.setText(mUsername);
                        mHasUser = true;
                    }
                } else {
                    mNavFriends.setVisible(false);
                    mNavProfile.setVisible(false);
                    mNavInbox.setVisible(false);
                    mMySubreddits.setVisible(false);
                    mPopular.setVisible(false);
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        mNavigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {

            @Override
            public boolean onNavigationItemSelected(MenuItem menuItem) {

            if (menuItem.isChecked()) menuItem.setChecked(false);
            else menuItem.setChecked(true);

            mDrawerLayout.closeDrawers();

            Intent intent;

            switch (menuItem.getItemId()) {
                case R.id.nav_home:
                    intent = new Intent(getApplicationContext(), MainActivity.class);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(BaseActivity.this).toBundle());
                    } else {
                        startActivity(intent);
                    }
                    return true;
                case R.id.nav_profile:
                    intent = new Intent(getApplicationContext(), UserInfoActivity.class);
                    intent.putExtra(getString(R.string.username), mUsername);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(BaseActivity.this).toBundle());
                    } else {
                        startActivity(intent);
                    }
                    return true;
                case R.id.nav_inbox:
                    intent = new Intent(getApplicationContext(), MessageActivity.class);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(BaseActivity.this).toBundle());
                    } else {
                        startActivity(intent);
                    }
                    return true;
                case R.id.nav_friends:
                    intent = new Intent(getApplicationContext(), FriendsListActivity.class);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(BaseActivity.this).toBundle());
                    } else {
                        startActivity(intent);
                    }
                    return true;
                case R.id.nav_subreddits:
                    intent = new Intent(getApplicationContext(), SubredditListActivity.class);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(BaseActivity.this).toBundle());
                    } else {
                        startActivity(intent);
                    }
                    return true;
                case R.id.nav_popular:
                    intent = new Intent(getApplicationContext(), MainActivity.class);
                    intent.putExtra(getString(R.string.category), getString(R.string.popular));
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(BaseActivity.this).toBundle());
                    } else {
                        startActivity(intent);
                    }
                    return true;
                case R.id.nav_go_to_user:
                    AlertDialog.Builder userBuilder = new AlertDialog.Builder(BaseActivity.this);

                    final EditText userInput = new EditText(BaseActivity.this);
                    userInput.setInputType(InputType.TYPE_CLASS_TEXT);
                    userInput.setTextColor(Color.BLACK);
                    userBuilder.setView(userInput);

                    userBuilder.setMessage(getString(R.string.nav_go_to_user))
                        .setPositiveButton(R.string.go, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                String user = userInput.getText().toString();
                                if (!TextUtils.isEmpty(user)) {
                                    Reddit.goToUser(BaseActivity.this, user);
                                }
                            }
                        })
                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {

                            }
                        });
                    AlertDialog userDialog = userBuilder.create();
                    userDialog.show();
                    return true;
                case R.id.nav_go_to_subreddit:
                    AlertDialog.Builder subredditBuilder = new AlertDialog.Builder(BaseActivity.this);

                    final EditText subredditInput = new EditText(BaseActivity.this);
                    subredditInput.setInputType(InputType.TYPE_CLASS_TEXT);
                    subredditInput.setTextColor(Color.BLACK);
                    subredditBuilder.setView(subredditInput);

                    subredditBuilder.setMessage(getString(R.string.nav_go_to_subreddit))
                            .setPositiveButton(R.string.go, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    String sub = subredditInput.getText().toString();
                                    if (!TextUtils.isEmpty(sub)) {
                                        Reddit.goToSubreddit(BaseActivity.this, sub);
                                    }
                                }
                            })
                            .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {

                                }
                            });
                    AlertDialog subredditDialog = subredditBuilder.create();
                    subredditDialog.show();
                    return true;

                default:
                    return true;
            }
            }
        });


        if (mHasUser) {
            mRemoveAccount.setVisibility(View.VISIBLE);
            mRemoveAccount.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(BaseActivity.this)
                    .setMessage(getString(R.string.remove_account))
                    .setPositiveButton(R.string.remove, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            mReddit.removeAccounts();
                        }
                    })
                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {

                        }
                    });
                    AlertDialog dialog = builder.create();
                    dialog.show();
                }
            });
        } else {
            mNavUser.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent authIntent = new Intent(getApplicationContext(), AuthenticationActivity.class);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        startActivity(authIntent, ActivityOptions.makeSceneTransitionAnimation(BaseActivity.this).toBundle());
                    } else {
                        startActivity(authIntent);
                    }
                }
            });
        }

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle actionBarDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, toolbar, R.string.drawer_open, R.string.drawer_close) {

            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                mDrawerLayout.requestFocus();
            }
        };

        mDrawerLayout.setDrawerListener(actionBarDrawerToggle);
        actionBarDrawerToggle.syncState();
    }

    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        super.setContentView(layoutResID);
        onCreateDrawer();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        SearchManager searchManager =
                (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView =
                (SearchView) menu.findItem(R.id.menu_search).getActionView();
        searchView.setSearchableInfo(
                searchManager.getSearchableInfo(getComponentName()));

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        mReddit = null;
        super.onDestroy();
    }

    private View.OnFocusChangeListener mTextFocusListener = (new View.OnFocusChangeListener() {
        @Override
        public void onFocusChange(View v, boolean hasFocus ){
            TextView text = (TextView) v;
            if (hasFocus) {
                text.setTextColor(ContextCompat.getColor(SiftApplication.getContext(), R.color.colorAccent));
            } else {
                if (v == mNavUser) {
                    text.setTextColor(Color.WHITE);
                } else {
                    text.setTextColor(ContextCompat.getColor(SiftApplication.getContext(), R.color.secondary_text));
                }
            }
        }
    });

    private View.OnFocusChangeListener mImageFocusListener = (new View.OnFocusChangeListener() {
        @Override
        public void onFocusChange(View v, boolean hasFocus ){
            ImageView image = (ImageView) v;
            if (hasFocus) {
                image.setColorFilter(ContextCompat.getColor(SiftApplication.getContext(), R.color.colorAccent));
            } else {
                image.setColorFilter(Color.TRANSPARENT);
            }
        }
    });
}
