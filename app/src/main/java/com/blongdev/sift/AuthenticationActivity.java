package com.blongdev.sift;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import net.dean.jraw.http.NetworkAdapter;
import net.dean.jraw.http.OkHttpNetworkAdapter;
import net.dean.jraw.oauth.Credentials;
import net.dean.jraw.oauth.OAuthHelper;
import net.dean.jraw.oauth.StatefulAuthHelper;


public class AuthenticationActivity extends BaseActivity {

    WebView mWebView;
    Reddit mReddit;
    Activity mActivity;

    private static final String LOG_TAG = "Authentication Activity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_authentication);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mActivity = this;

        mWebView = (WebView) findViewById(R.id.web_view);

        mReddit = Reddit.getInstance();

        final Credentials credentials = Credentials.installedApp(getString(R.string.client_id), getString(R.string.redirect_url));
        NetworkAdapter networkAdapter = new OkHttpNetworkAdapter(mReddit.mUserAgent);
        final StatefulAuthHelper oAuth = OAuthHelper.interactive(networkAdapter, credentials);
        String[] scopes = new String[]{"identity", "edit", "flair", "history", "modconfig", "modflair",
                "modlog", "modposts", "modwiki", "mysubreddits", "privatemessages", "read", "report",
                "save", "submit", "subscribe", "vote", "wikiedit", "wikiread"};
        String url = oAuth.getAuthorizationUrl(true, true, scopes);

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.removeAllCookie();

        mWebView.loadUrl(url);

        mWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {

            }
        });

        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                if (url.contains("code=")) {
                    mReddit.runUserChallengeTask(url);
                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(mActivity).toBundle());
                    } else {
                        startActivity(intent);
                    }
                }
            }
        });
    }

}
