package com.blongdev.sift;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class ComposeMessageActivity extends AppCompatActivity {

    private static final String BODY = "body";
    private static final String SUBJECT = "subject";

    String mUserTo;
    TextView mTo;
    EditText mSubject;
    EditText mBody;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compose_message);

        android.support.v7.widget.Toolbar toolbar = (android.support.v7.widget.Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        mTo = (TextView) findViewById(R.id.to_label);
        mSubject = (EditText) findViewById(R.id.subject_text);
        mBody = (EditText) findViewById(R.id.body_text);

        mSubject.requestFocus();

        Intent intent = getIntent();
        if (intent != null) {
            mUserTo = intent.getStringExtra(getString(R.string.username));
            mTo.setText(mUserTo);
            String subject = intent.getStringExtra(getString(R.string.title));
            if (!TextUtils.isEmpty(subject)) {
                mSubject.setText(subject);
                mBody.requestFocus();
            }
        }

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String subject = mSubject.getText().toString();
                String body = mBody.getText().toString();

                if (TextUtils.isEmpty(subject)) {
                    Toast.makeText(getApplicationContext(), getString(R.string.add_subject), Toast.LENGTH_LONG).show();
                    return;
                }

                if (TextUtils.isEmpty(body)) {
                    Toast.makeText(getApplicationContext(), getString(R.string.add_body), Toast.LENGTH_LONG).show();
                    return;
                }

                Reddit.sendMessage(ComposeMessageActivity.this, mUserTo, subject, body);
            }

//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
        });
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putString(BODY, mBody.getText().toString());
        savedInstanceState.putString(SUBJECT, mSubject.getText().toString());
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mSubject.setText(savedInstanceState.getString(SUBJECT));
        mBody.setText(savedInstanceState.getString(BODY));
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
