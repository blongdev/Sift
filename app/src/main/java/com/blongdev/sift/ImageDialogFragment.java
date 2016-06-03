package com.blongdev.sift;

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
import com.squareup.picasso.Picasso;

/**
 * Created by Brian on 3/11/2016.
 */
public class ImageDialogFragment extends DialogFragment {

    private static final boolean PINCH_ZOOM_ENABLED = false;

    Matrix mMatrix;
    ImageView mImageView;

    float mInitialScaleFactor = 1;
    float mScaleFactor = 1f;

    ScaleGestureDetector mScaleGestureDetector;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.image_dialog_fragment, container, false);

        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);

        mImageView = (ImageView) rootView.findViewById(R.id.dialog_fragment_image);
        Bundle args = getArguments();
        String imageUrl = args.getString(getString(R.string.image_url));
        Picasso.with(getContext()).load(imageUrl).into(mImageView);

        mMatrix = mImageView.getMatrix();

        if (PINCH_ZOOM_ENABLED) {
            mScaleGestureDetector = new ScaleGestureDetector(getContext(), new ScaleListener());

            mImageView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    mScaleGestureDetector.onTouchEvent(event);
                    return true;
                }
            });
        }

        return rootView;
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {

            mScaleFactor *=  detector.getScaleFactor();
            mScaleFactor = Math.max(0.2f*mInitialScaleFactor, Math.min(mScaleFactor, 5f*mInitialScaleFactor));

            mMatrix.setScale(mScaleFactor,mScaleFactor);
            mImageView.setImageMatrix(mMatrix);
            return true;
        }
    }

    @Override
    public void onResume()
    {
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int maxWidth = metrics.widthPixels;
        int maxHeight = metrics.heightPixels;

        int imageWidth = mImageView.getDrawable().getIntrinsicWidth();
        int imageHeight = mImageView.getDrawable().getIntrinsicHeight();

        if ((maxHeight - imageHeight) < (maxWidth - imageWidth)) {
            mInitialScaleFactor = Float.valueOf(maxHeight)/Float.valueOf(imageHeight);
        } else if(imageWidth > 0){
            mInitialScaleFactor = Float.valueOf(maxWidth)/Float.valueOf(imageWidth);
        }

        mMatrix.setScale(mInitialScaleFactor, mInitialScaleFactor);
        mImageView.setImageMatrix(mMatrix);
        mScaleFactor = mInitialScaleFactor;

        int newWidth = (int)(imageWidth * mInitialScaleFactor);
        int newHeight = (int)(imageHeight * mInitialScaleFactor);

        getDialog().getWindow().setLayout(newWidth, newHeight);

        super.onResume();
    }
}
