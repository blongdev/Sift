package com.blongdev.sift;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import net.dean.jraw.paginators.Sorting;
import net.dean.jraw.paginators.TimePeriod;

/**
 * Created by Brian on 3/11/2016.
 */
public class TimeFragment extends DialogFragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.time_fragment, container, false);

        final Reddit reddit = Reddit.getInstance();

        RadioGroup timeGroup = (RadioGroup) rootView.findViewById(R.id.timeGroup);
        RadioButton timeHour = (RadioButton) rootView.findViewById(R.id.timeHour);
        RadioButton timeDay = (RadioButton) rootView.findViewById(R.id.timeDay);
        RadioButton timeWeek = (RadioButton) rootView.findViewById(R.id.timeWeek);
        RadioButton timeMonth = (RadioButton) rootView.findViewById(R.id.timeMonth);
        RadioButton timeYear = (RadioButton) rootView.findViewById(R.id.timeYear);
        RadioButton timeAll = (RadioButton) rootView.findViewById(R.id.timeAll);


        if (reddit.mTime == TimePeriod.HOUR) {
            timeHour.setChecked(true);
        } else if (reddit.mTime == TimePeriod.DAY) {
            timeDay.setChecked(true);
        } else if (reddit.mTime == TimePeriod.WEEK) {
            timeWeek.setChecked(true);
        } else if (reddit.mTime == TimePeriod.MONTH) {
            timeMonth.setChecked(true);
        } else if (reddit.mTime == TimePeriod.YEAR) {
            timeYear.setChecked(true);
        } else if (reddit.mTime == TimePeriod.ALL) {
            timeAll.setChecked(true);
        }

        timeGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId)
            {
                RadioButton checkedButton = (RadioButton) group.findViewById(checkedId);
                boolean isChecked = checkedButton.isChecked();
                if (isChecked) {
                    if (checkedId == R.id.timeHour) {
                        reddit.mTime = TimePeriod.HOUR;
                    } else if (checkedId == R.id.timeDay) {
                        reddit.mTime = TimePeriod.DAY;
                    } else if (checkedId == R.id.timeWeek) {
                        reddit.mTime = TimePeriod.WEEK;
                    } else if (checkedId == R.id.timeMonth) {
                        reddit.mTime =TimePeriod.MONTH;
                    } else if (checkedId == R.id.timeYear) {
                        reddit.mTime = TimePeriod.YEAR;
                    } else if (checkedId == R.id.timeAll) {
                        reddit.mTime = TimePeriod.ALL;
                    }

                    Intent intent = new Intent(getString(R.string.sortChanged));
                    SiftApplication.getContext().sendBroadcast(intent);

                    dismiss();
                }
            }
        });


        return rootView;
    }

}
