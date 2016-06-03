package com.blongdev.sift;

import android.content.Intent;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class MessageDetailActivityFragment extends Fragment {

    public MessageDetailActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_message_detail, container, false);

        TextView titleView = (TextView) rootView.findViewById(R.id.message_detail_title);
        TextView bodyView = (TextView) rootView.findViewById(R.id.message_detail_body);
        TextView fromView = (TextView) rootView.findViewById(R.id.message_detail_from);

        //Change toolbar title to username
        Intent intent = getActivity().getIntent();
        String title, body, from;
        Bundle args = getArguments();
        if (args != null) {
            title = args.getString(getString(R.string.title));
            body = args.getString(getString(R.string.body));
            from = args.getString(getString(R.string.from));
        } else {
            title = intent.getStringExtra(getString(R.string.title));
            body = intent.getStringExtra(getString(R.string.body));
            from = intent.getStringExtra(getString(R.string.from));
        }

        if (!TextUtils.isEmpty(title)) {
            titleView.setText(title);
        }

        if (!TextUtils.isEmpty(body)) {
            bodyView.setText(body);
        }

        if (!TextUtils.isEmpty(from)) {
            fromView.setText(SiftApplication.getContext().getString(R.string.from_label) + " " + from);
        }

        return rootView;
    }
}
