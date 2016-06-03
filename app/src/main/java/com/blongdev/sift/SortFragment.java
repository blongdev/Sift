package com.blongdev.sift;

import android.content.Intent;
import android.graphics.Matrix;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.squareup.picasso.Picasso;

import net.dean.jraw.paginators.Sorting;

/**
 * Created by Brian on 3/11/2016.
 */
public class SortFragment extends DialogFragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.sort_fragment, container, false);

        final Reddit reddit = Reddit.getInstance();

        RadioGroup sortGroup = (RadioGroup) rootView.findViewById(R.id.sortGroup);
        RadioButton sortHot = (RadioButton) rootView.findViewById(R.id.sortHot);
        RadioButton sortNew = (RadioButton) rootView.findViewById(R.id.sortNew);
        RadioButton sortRising = (RadioButton) rootView.findViewById(R.id.sortRising);
        RadioButton sortControversial = (RadioButton) rootView.findViewById(R.id.sortControversial);
        RadioButton sortTop = (RadioButton) rootView.findViewById(R.id.sortTop);

        if (reddit.mSort == Sorting.HOT) {
            sortHot.setChecked(true);
        } else if (reddit.mSort == Sorting.NEW) {
            sortNew.setChecked(true);
        } else if (reddit.mSort == Sorting.RISING) {
            sortRising.setChecked(true);
        } else if (reddit.mSort == Sorting.CONTROVERSIAL) {
            sortControversial.setChecked(true);
        } else if (reddit.mSort == Sorting.TOP) {
            sortTop.setChecked(true);
        }

        sortGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId)
            {
                RadioButton checkedButton = (RadioButton) group.findViewById(checkedId);
                boolean isChecked = checkedButton.isChecked();
                if (isChecked) {
                    if (checkedId == R.id.sortHot) {
                        reddit.mSort = Sorting.HOT;
                    } else if (checkedId == R.id.sortNew) {
                        reddit.mSort = Sorting.NEW;
                    } else if (checkedId == R.id.sortRising) {
                        reddit.mSort = Sorting.RISING;
                    } else if (checkedId == R.id.sortControversial) {
                        reddit.mSort = Sorting.CONTROVERSIAL;
                    } else if (checkedId == R.id.sortTop) {
                        reddit.mSort = Sorting.TOP;
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
