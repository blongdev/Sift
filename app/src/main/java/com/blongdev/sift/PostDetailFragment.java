package com.blongdev.sift;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;


public class PostDetailFragment extends Fragment {

    TextView mTitle;
    TextView mUsername;
    TextView mAge;
    WebView mWebView;
    ProgressBar mLoadingSpinner;
    TextView mBody;

    private long mPostId = 0;
    private String mPostServerId;

    public PostDetailFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView =  inflater.inflate(R.layout.fragment_post_detail, container, false);

        Intent intent = getActivity().getIntent();

        String url = intent.getStringExtra(getString(R.string.url));
        String body = intent.getStringExtra(getString(R.string.body));

        mWebView = (WebView) rootView.findViewById(R.id.post_web_view);
        mLoadingSpinner = (ProgressBar) rootView.findViewById(R.id.progressSpinner);
        mBody = (TextView) rootView.findViewById(R.id.post_body);

        mBody.setText(body);
        Linkify.addLinks(mBody, Linkify.ALL);

        Bundle args = getArguments();
        if (args != null) {
            mPostId = args.getLong(getString(R.string.post_id));
            mPostServerId = args.getString(getString(R.string.server_id));
        }

        if (TextUtils.isEmpty(body) && !TextUtils.isEmpty(url)) {
                mWebView.loadUrl(url);

                mWebView.setWebChromeClient(new WebChromeClient() {
                    @Override
                    public void onProgressChanged(WebView view, int newProgress) {

                    }
                });

                mWebView.setWebViewClient(new WebViewClient() {
                    @Override
                    public void onPageStarted(WebView view, String url, Bitmap favicon) {
                        mLoadingSpinner.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onPageFinished(WebView view, String url) {
                        mLoadingSpinner.setVisibility(View.GONE);
                    }
                });

                mWebView.getSettings().setJavaScriptEnabled(true);
                mWebView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
                mWebView.getSettings().setLoadWithOverviewMode(true);
                mWebView.getSettings().setUseWideViewPort(true);
                mWebView.getSettings().setBuiltInZoomControls(true);

        }

        return rootView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onPause() {
        super.onPause();
        mWebView.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        mWebView.onResume();
    }
}
