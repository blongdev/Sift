package com.blongdev.sift;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.URLUtil;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.squareup.picasso.Picasso;
import net.dean.jraw.ApiException;
import net.dean.jraw.http.NetworkException;
import net.dean.jraw.managers.CaptchaHelper;
import net.dean.jraw.models.Captcha;
import java.net.MalformedURLException;
import java.net.URL;

public class ComposePostActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Captcha>{

    private static final String BODY = "body";
    private static final String TITLE = "title";
    private static final String CAPTCHA = "captcha";
    private static final String LINK = "link";

    String mSubredditName;
    Captcha mCaptcha;
    ImageView mCaptchaImage;
    CheckBox mLinkBox;
    TextView mTextLabel;
    EditText mTitle;
    EditText mBody;
    EditText mCaptchaText;
    View mCaptchaBlock;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compose_post);

        android.support.v7.widget.Toolbar toolbar = (android.support.v7.widget.Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        TextView subreddit = (TextView) findViewById(R.id.subreddit_label);
        mCaptchaImage = (ImageView) findViewById(R.id.captcha_image);
        mLinkBox = (CheckBox) findViewById(R.id.link);
        mTextLabel = (TextView) findViewById(R.id.body_label);
        mTitle = (EditText) findViewById(R.id.title_text);
        mBody = (EditText) findViewById(R.id.body_text);
        mCaptchaText = (EditText) findViewById(R.id.captcha_text);
        mCaptchaBlock = findViewById(R.id.captcha_block);

        mTitle.requestFocus();

        Intent intent = getIntent();
        if (intent != null) {
            mSubredditName = intent.getStringExtra(getString(R.string.subreddit_name));
            subreddit.setText(mSubredditName);
        }

        getSupportLoaderManager().initLoader(1, null, this).forceLoad();


        mLinkBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mLinkBox.isChecked()) {
                    mTextLabel.setText(getString(R.string.compose_url));
                } else {
                    mTextLabel.setText(getString(R.string.compose_text));
                }
            }
        });

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String title = mTitle.getText().toString();
                String body = mBody.getText().toString();
                String captchaText = mCaptchaText.getText().toString();

                if (TextUtils.isEmpty(title)) {
                    Toast.makeText(getApplicationContext(), getString(R.string.add_title), Toast.LENGTH_LONG).show();
                    return;
                }

                if (mLinkBox.isChecked()) {
                    if (TextUtils.isEmpty(body) || !URLUtil.isValidUrl(body)) {
                        Toast.makeText(getApplicationContext(), getString(R.string.add_url), Toast.LENGTH_LONG).show();
                        return;
                    }
                }

                if (mCaptcha != null && TextUtils.isEmpty(captchaText)) {
                    Toast.makeText(getApplicationContext(), getString(R.string.add_captcha), Toast.LENGTH_LONG).show();
                    return;
                }

                if (mLinkBox.isChecked()) {
                    try {
                        URL url = new URL(body);
                        Reddit.linkSubmission(ComposePostActivity.this, mSubredditName, title, url, mCaptcha, captchaText);
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    }
                } else {
                    Reddit.textSubmission(ComposePostActivity.this, mSubredditName, title, body, mCaptcha, captchaText);
                }
            }
        });
    }

    public Loader<Captcha> onCreateLoader(int id, Bundle args) {
        return new CaptchaLoader(getApplicationContext());
    }

    @Override
    public void onLoadFinished(Loader<Captcha> loader, Captcha captcha) {
        if (captcha != null) {
            mCaptcha = captcha;
            Picasso.with(getApplicationContext()).load(captcha.getImageUrl().toString()).into(mCaptchaImage);
            mCaptchaBlock.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onLoaderReset(Loader<Captcha> loader) {
        mCaptchaBlock.setVisibility(View.GONE);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putString(BODY, mBody.getText().toString());
        savedInstanceState.putString(TITLE, mTitle.getText().toString());
        savedInstanceState.putString(CAPTCHA, mCaptchaText.getText().toString());
        savedInstanceState.putBoolean(LINK, mLinkBox.isChecked());
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mTitle.setText(savedInstanceState.getString(TITLE));
        mBody.setText(savedInstanceState.getString(BODY));
        mCaptchaText.setText(savedInstanceState.getString(CAPTCHA));
        mLinkBox.setChecked(savedInstanceState.getBoolean(LINK));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_post_detail, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}

class CaptchaLoader extends AsyncTaskLoader<Captcha> {
    public CaptchaLoader(Context context) {
        super(context);
    }
    @Override
    public Captcha loadInBackground() {
        Reddit reddit = Reddit.getInstance();
        if (!reddit.mRedditClient.isAuthenticated()) {
            return null;
        }

        try {
            reddit.mRateLimiter.acquire();
            CaptchaHelper captchaHelper = new CaptchaHelper(reddit.mRedditClient);
            if (captchaHelper.isNecessary()) {
                reddit.mRateLimiter.acquire();
                return captchaHelper.getNew();
            }
        } catch (RuntimeException | ApiException e) {
            e.printStackTrace();
        }

        return null;
    }
}
