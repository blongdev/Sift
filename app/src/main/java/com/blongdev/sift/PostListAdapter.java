package com.blongdev.sift;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.VectorDrawable;
import android.media.Image;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.telecom.Call;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.blongdev.sift.database.SiftContract;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;
import net.dean.jraw.http.NetworkException;
import net.dean.jraw.models.Comment;
import net.dean.jraw.models.Submission;
import java.util.List;

/**
 * Created by Brian on 2/24/2016.
 */
public class PostListAdapter extends RecyclerView.Adapter<PostListAdapter.PostViewHolder> {

    private List<ContributionInfo> mPostList;
    private Callback mCallback;

    public PostListAdapter(List<ContributionInfo> postList, Callback callback) {
        mPostList = postList;
        mCallback = callback;
    }

    public void refreshWithList(List<ContributionInfo> postList) {
        this.mPostList = postList;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return mPostList.size();
    }

    @Override
    public int getItemViewType(int position) {
        if (mPostList.get(position) instanceof CommentInfo) {
            return ContributionInfo.CONTRIBUTION_COMMENT;
        } else  {
            return ContributionInfo.CONTRIBUTION_POST;
        }
    }

    @Override
    public void onBindViewHolder(final PostViewHolder postViewHolder, int i) {
        int type = mPostList.get(i).mContributionType;
        postViewHolder.mContribution = mPostList.get(i);

        if (type == ContributionInfo.CONTRIBUTION_COMMENT) {
            CommentInfo comment = (CommentInfo) mPostList.get(i);
            postViewHolder.mUsername.setText(comment.mUsername);
            postViewHolder.mPoints.setText(String.valueOf(comment.mPoints));
            postViewHolder.mAge.setText(Utilities.getAgeString(comment.mAge));
            postViewHolder.mPostId = comment.mPost;
            postViewHolder.mServerId = comment.mServerId;
            postViewHolder.mPostServerId = comment.mPostServerId;
            postViewHolder.mTitle.setText(comment.mBody);
            postViewHolder.mContributionType = comment.mContributionType;
            postViewHolder.mVote = comment.mVote;
            postViewHolder.mJrawComment = comment.mJrawComment;

            if (comment.mVote == SiftContract.Posts.UPVOTE) {
                postViewHolder.mUpvote.setImageDrawable(SiftApplication.getContext().getResources().getDrawable(R.drawable.ic_up_arrow_filled_24dp));
                postViewHolder.mDownvote.setImageDrawable(SiftApplication.getContext().getResources().getDrawable(R.drawable.ic_down_arrow_white_24dp));
                postViewHolder.mPoints.setTextColor(SiftApplication.getContext().getResources().getColor(R.color.upvote));
            } else if (comment.mVote == SiftContract.Posts.DOWNVOTE) {
                postViewHolder.mDownvote.setImageDrawable(SiftApplication.getContext().getResources().getDrawable(R.drawable.ic_down_arrow_filled_24dp));
                postViewHolder.mUpvote.setImageDrawable(SiftApplication.getContext().getResources().getDrawable(R.drawable.ic_up_arrow_white_24dp));
                postViewHolder.mPoints.setTextColor(SiftApplication.getContext().getResources().getColor(R.color.downvote));
            } else {
                postViewHolder.mUpvote.setImageDrawable(SiftApplication.getContext().getResources().getDrawable(R.drawable.ic_up_arrow_white_24dp));
                postViewHolder.mDownvote.setImageDrawable(SiftApplication.getContext().getResources().getDrawable(R.drawable.ic_down_arrow_white_24dp));
                postViewHolder.mPoints.setTextColor(Color.WHITE);
            }

        } else {
            PostInfo post = (PostInfo) mPostList.get(i);
            postViewHolder.mUsername.setText(post.mUsername);
            postViewHolder.mSubreddit.setText(post.mSubreddit);
            postViewHolder.mTitle.setText(post.mTitle);
            postViewHolder.mPoints.setText(String.valueOf(post.mPoints));
            postViewHolder.mComments.setText(post.mComments + " Comments");
            postViewHolder.mDomain.setText(post.mDomain);
            postViewHolder.mAge.setText(Utilities.getAgeString(post.mAge));
            postViewHolder.mImageUrl = post.mImageUrl;
            postViewHolder.mPostId = post.mId;
            postViewHolder.mServerId = post.mServerId;
            postViewHolder.mUrl = post.mUrl;
            postViewHolder.mBody = post.mBody;
            postViewHolder.mContributionType = post.mContributionType;
            postViewHolder.mVote = post.mVote;

            if (post.mVote == SiftContract.Posts.UPVOTE) {
                postViewHolder.mUpvote.setImageDrawable(SiftApplication.getContext().getResources().getDrawable(R.drawable.ic_up_arrow_filled_24dp));
                postViewHolder.mDownvote.setImageDrawable(SiftApplication.getContext().getResources().getDrawable(R.drawable.ic_down_arrow_white_24dp));
                postViewHolder.mPoints.setTextColor(SiftApplication.getContext().getResources().getColor(R.color.upvote));
            } else if (post.mVote == SiftContract.Posts.DOWNVOTE) {
                postViewHolder.mDownvote.setImageDrawable(SiftApplication.getContext().getResources().getDrawable(R.drawable.ic_down_arrow_filled_24dp));
                postViewHolder.mUpvote.setImageDrawable(SiftApplication.getContext().getResources().getDrawable(R.drawable.ic_up_arrow_white_24dp));
                postViewHolder.mPoints.setTextColor(SiftApplication.getContext().getResources().getColor(R.color.downvote));
            } else {
                postViewHolder.mUpvote.setImageDrawable(SiftApplication.getContext().getResources().getDrawable(R.drawable.ic_up_arrow_white_24dp));
                postViewHolder.mDownvote.setImageDrawable(SiftApplication.getContext().getResources().getDrawable(R.drawable.ic_down_arrow_white_24dp));
                postViewHolder.mPoints.setTextColor(Color.WHITE);
            }

            postViewHolder.mImage.setImageDrawable(null);

            //picasso needs to be passed null to prevent listview from displaying incorrectly cached images
            if(!TextUtils.isEmpty(post.mImageUrl)) {
                Picasso.with(SiftApplication.getContext()).cancelRequest(postViewHolder.mImage);
                Picasso.with(SiftApplication.getContext())
                        .load(post.mImageUrl)
                        .fit()
                        .centerCrop()
                        .placeholder(R.drawable.ic_photo_48dp)
                        .into(postViewHolder.mImage);
            } else {
                Picasso.with(SiftApplication.getContext())
                        .load(post.mImageUrl)
                        .into(postViewHolder.mImage);
            }
        }
    }

    @Override
    public PostViewHolder onCreateViewHolder(ViewGroup viewGroup, int type) {
        View itemView;
        if (type == ContributionInfo.CONTRIBUTION_COMMENT) {
            itemView = LayoutInflater.
                    from(viewGroup.getContext()).
                    inflate(R.layout.comment_card, viewGroup, false);
        } else {
            itemView = LayoutInflater.
                    from(viewGroup.getContext()).
                    inflate(R.layout.post, viewGroup, false);
        }

        return new PostViewHolder(mCallback, itemView, type);
    }

    public static class PostViewHolder extends RecyclerView.ViewHolder  {
        protected Callback mCallback;
        protected TextView mUsername;
        protected TextView mSubreddit;
        protected TextView mTitle;
        protected TextView mPoints;
        protected TextView mComments;
        protected TextView mDomain;
        protected TextView mAge;
        protected ImageView mImage;
        protected ImageView mUpvote;
        protected ImageView mDownvote;

        protected String mImageUrl;
        protected long mPostId;
        protected String mServerId;
        protected String mBody;
        protected String mUrl;
        protected int mContributionType;
        protected int mVote;
        protected Comment mJrawComment;
        protected String mPostServerId;
        protected ContributionInfo mContribution;

        public PostViewHolder(Callback callback, View v, int contributionType) {
            super(v);
            mCallback = callback;

            if (contributionType == ContributionInfo.CONTRIBUTION_COMMENT) {
                mUsername =  (TextView) v.findViewById(R.id.comment_username);
                mTitle = (TextView)  v.findViewById(R.id.comment_body);
                mPoints = (TextView) v.findViewById(R.id.comment_points);
                mAge = (TextView) v.findViewById(R.id.comment_age);
                mUpvote = (ImageView) v.findViewById(R.id.upvote);
                mDownvote = (ImageView) v.findViewById(R.id.downvote);

                mUpvote.setOnClickListener(mOnClickListener);
                mDownvote.setOnClickListener(mOnClickListener);
                mTitle.setOnClickListener(mOnClickListener);
                mUsername.setOnClickListener(mOnClickListener);
            } else {
                mUsername =  (TextView) v.findViewById(R.id.post_username);
                mSubreddit = (TextView) v.findViewById(R.id.post_subreddit);
                mTitle = (TextView) v.findViewById(R.id.post_title);
                mPoints = (TextView) v.findViewById(R.id.post_points);
                mComments = (TextView)  v.findViewById(R.id.post_comments);
                mDomain = (TextView)  v.findViewById(R.id.post_url);
                mAge = (TextView) v.findViewById(R.id.post_age);
                mImage = (ImageView) v.findViewById(R.id.post_image);
                mUpvote = (ImageView) v.findViewById(R.id.upvote);
                mDownvote = (ImageView) v.findViewById(R.id.downvote);

                mUpvote.setOnClickListener(mOnClickListener);
                mDownvote.setOnClickListener(mOnClickListener);
                mTitle.setOnClickListener(mOnClickListener);
                mUsername.setOnClickListener(mOnClickListener);
                mImage.setOnClickListener(mOnClickListener);
                mSubreddit.setOnClickListener(mOnClickListener);

                mTitle.setOnFocusChangeListener(mTextFocusListener);
                mUsername.setOnFocusChangeListener(mTextFocusListener);
                mSubreddit.setOnFocusChangeListener(mTextFocusListener);
                mUpvote.setOnFocusChangeListener(mImageFocusListener);
                mDownvote.setOnFocusChangeListener(mImageFocusListener);
            }
        }

        private View.OnFocusChangeListener mTextFocusListener = (new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus ){
                TextView text = (TextView) v;
                if (hasFocus) {
                    text.setTextColor(ContextCompat.getColor(SiftApplication.getContext(), R.color.colorAccent));
                } else {
                    if (v == mTitle) {
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

        private View.OnClickListener mOnClickListener = (new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v == mTitle || v == mImage) {
                    goToPostDetail(v);
                } else if (v == mUsername) {
                    goToUserInfo(v);
                } else if (v == mSubreddit) {
                    goToSubreddit(v);
                } else if (v == mUpvote) {
                    upvote();
                } else if(v == mDownvote) {
                    downvote();
                }
            }

            private void upvote() {
                if (!Utilities.loggedIn()) {
                    Toast.makeText(SiftApplication.getContext(), SiftApplication.getContext().getString(R.string.must_log_in), Toast.LENGTH_LONG).show();
                    return;
                }

                if (mVote == SiftContract.Posts.UPVOTE) {
                    mVote = SiftContract.Posts.NO_VOTE;
                    mUpvote.setImageDrawable(SiftApplication.getContext().getResources().getDrawable(R.drawable.ic_up_arrow_white_24dp));
                    mDownvote.setImageDrawable(SiftApplication.getContext().getResources().getDrawable(R.drawable.ic_down_arrow_white_24dp));
                    mPoints.setTextColor(Color.WHITE);
                    mPoints.setText(String.valueOf(Integer.valueOf(mPoints.getText().toString()) - 1));
                } else if(mVote == SiftContract.Posts.DOWNVOTE) {
                    mVote = SiftContract.Posts.UPVOTE;
                    mUpvote.setImageDrawable(SiftApplication.getContext().getResources().getDrawable(R.drawable.ic_up_arrow_filled_24dp));
                    mDownvote.setImageDrawable(SiftApplication.getContext().getResources().getDrawable(R.drawable.ic_down_arrow_white_24dp));
                    mPoints.setTextColor(SiftApplication.getContext().getResources().getColor(R.color.upvote));
                    mPoints.setText(String.valueOf(Integer.valueOf(mPoints.getText().toString()) + 2));
                } else {
                    mVote = SiftContract.Posts.UPVOTE;
                    mUpvote.setImageDrawable(SiftApplication.getContext().getResources().getDrawable(R.drawable.ic_up_arrow_filled_24dp));
                    mDownvote.setImageDrawable(SiftApplication.getContext().getResources().getDrawable(R.drawable.ic_down_arrow_white_24dp));
                    mPoints.setTextColor(SiftApplication.getContext().getResources().getColor(R.color.upvote));
                    mPoints.setText(String.valueOf(Integer.valueOf(mPoints.getText().toString()) + 1));
                }

                mContribution.mVote = mVote;

                if (mContributionType == ContributionInfo.CONTRIBUTION_POST) {
                    Reddit.votePost(mServerId, mVote);
                } else if(mContributionType == ContributionInfo.CONTRIBUTION_COMMENT) {
                    Reddit.voteComment(mJrawComment, mVote);
                }
            }

            private void downvote() {
                if (!Utilities.loggedIn()) {
                    Toast.makeText(SiftApplication.getContext(), SiftApplication.getContext().getString(R.string.must_log_in), Toast.LENGTH_LONG).show();
                    return;
                }

                if (mVote == SiftContract.Posts.DOWNVOTE) {
                    mVote = SiftContract.Posts.NO_VOTE;
                    mDownvote.setImageDrawable(SiftApplication.getContext().getResources().getDrawable(R.drawable.ic_down_arrow_white_24dp));
                    mUpvote.setImageDrawable(SiftApplication.getContext().getResources().getDrawable(R.drawable.ic_up_arrow_white_24dp));
                    mPoints.setTextColor(Color.WHITE);
                    mPoints.setText(String.valueOf(Integer.valueOf(mPoints.getText().toString()) + 1));
                } else if (mVote == SiftContract.Posts.UPVOTE) {
                    mVote = SiftContract.Posts.DOWNVOTE;
                    mDownvote.setImageDrawable(SiftApplication.getContext().getResources().getDrawable(R.drawable.ic_down_arrow_filled_24dp));
                    mUpvote.setImageDrawable(SiftApplication.getContext().getResources().getDrawable(R.drawable.ic_up_arrow_white_24dp));
                    mPoints.setTextColor(SiftApplication.getContext().getResources().getColor(R.color.downvote));
                    mPoints.setText(String.valueOf(Integer.valueOf(mPoints.getText().toString()) - 2));
                } else {
                    mVote = SiftContract.Posts.DOWNVOTE;
                    mDownvote.setImageDrawable(SiftApplication.getContext().getResources().getDrawable(R.drawable.ic_down_arrow_filled_24dp));
                    mUpvote.setImageDrawable(SiftApplication.getContext().getResources().getDrawable(R.drawable.ic_up_arrow_white_24dp));
                    mPoints.setTextColor(SiftApplication.getContext().getResources().getColor(R.color.downvote));
                    mPoints.setText(String.valueOf(Integer.valueOf(mPoints.getText().toString()) - 1));
                }

                mContribution.mVote = mVote;

                if (mContributionType == ContributionInfo.CONTRIBUTION_POST) {
                    Reddit.votePost(mServerId, mVote);
                } else if(mContributionType == ContributionInfo.CONTRIBUTION_COMMENT) {
                    Reddit.voteComment(mJrawComment, mVote);
                }
            }

            private void goToPostDetail(View v) {
                if (mContributionType == ContributionInfo.CONTRIBUTION_COMMENT) {
                    new GetPostTask(mPostServerId).execute();
                } else {
                    String title = mTitle.getText().toString();
                    String username = mUsername.getText().toString();
                    String subreddit = mSubreddit.getText().toString();
                    String points = mPoints.getText().toString();
                    String comments = mComments.getText().toString();
                    String age = mAge.getText().toString();
                    String domain = mDomain.getText().toString();

                    Intent intent = new Intent(SiftApplication.getContext(), PostDetailActivity.class);
                    intent.putExtra(SiftApplication.getContext().getString(R.string.title), title);
                    intent.putExtra(SiftApplication.getContext().getString(R.string.username), username);
                    intent.putExtra(SiftApplication.getContext().getString(R.string.subreddit), subreddit);
                    intent.putExtra(SiftApplication.getContext().getString(R.string.points), points);
                    intent.putExtra(SiftApplication.getContext().getString(R.string.comments), comments);
                    intent.putExtra(SiftApplication.getContext().getString(R.string.url), mUrl);
                    intent.putExtra(SiftApplication.getContext().getString(R.string.age), age);
                    intent.putExtra(SiftApplication.getContext().getString(R.string.image_url), mImageUrl);
                    intent.putExtra(SiftApplication.getContext().getString(R.string.post_id), mPostId);
                    intent.putExtra(SiftApplication.getContext().getString(R.string.server_id), mServerId);
                    intent.putExtra(SiftApplication.getContext().getString(R.string.body), mBody);
                    intent.putExtra(SiftApplication.getContext().getString(R.string.domain), domain);
                    intent.putExtra(SiftApplication.getContext().getString(R.string.vote), mVote);

                    mCallback.onItemSelected(intent);
                }
            }

            private void goToUserInfo(View v) {
                String username = mUsername.getText().toString();
                Intent intent = new Intent(SiftApplication.getContext(), UserInfoActivity.class);
                intent.putExtra(SiftApplication.getContext().getString(R.string.username), username);
                mCallback.onItemSelected(intent);
            }

            private void goToSubreddit(View v) {
                String subreddit = mSubreddit.getText().toString();
                Intent intent = new Intent(SiftApplication.getContext(), SubredditActivity.class);
                intent.putExtra(SiftApplication.getContext().getString(R.string.subreddit_name), subreddit);
                mCallback.onItemSelected(intent);
            }
        });

        private final class GetPostTask extends AsyncTask<String, Void, Submission> {
            String mSubmissionServerId;

            public GetPostTask(String submissionServerId) {
                mSubmissionServerId = submissionServerId;
            }

            @Override
            protected Submission doInBackground(String... params) {
                try {
                    Reddit reddit = Reddit.getInstance();
                    reddit.mRateLimiter.acquire();
                    return reddit.mRedditClient.getSubmission(mSubmissionServerId);
                } catch (RuntimeException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPreExecute() {

            }

            @Override
            protected void onPostExecute(Submission sub) {
                if (sub != null) {
                    Intent intent = new Intent(SiftApplication.getContext(), PostDetailActivity.class);
                    intent.putExtra(SiftApplication.getContext().getString(R.string.title), sub.getTitle());
                    intent.putExtra(SiftApplication.getContext().getString(R.string.username), sub.getAuthor());
                    intent.putExtra(SiftApplication.getContext().getString(R.string.subreddit), sub.getSubredditName());
                    intent.putExtra(SiftApplication.getContext().getString(R.string.points), sub.getScore());
                    intent.putExtra(SiftApplication.getContext().getString(R.string.comments), sub.getCommentCount());
                    intent.putExtra(SiftApplication.getContext().getString(R.string.url), sub.getUrl());
                    intent.putExtra(SiftApplication.getContext().getString(R.string.age), Utilities.getAgeString(sub.getCreated().getTime()));
                    intent.putExtra(SiftApplication.getContext().getString(R.string.post_id), mPostId);
                    intent.putExtra(SiftApplication.getContext().getString(R.string.server_id), mPostServerId);
                    intent.putExtra(SiftApplication.getContext().getString(R.string.body), sub.getSelftext());
                    intent.putExtra(SiftApplication.getContext().getString(R.string.domain), sub.getDomain());
                    intent.putExtra(SiftApplication.getContext().getString(R.string.vote), sub.getVote());

                    mCallback.onItemSelected(intent);
                }
            }
        }
    }

    public interface Callback {
        void onItemSelected(Intent intent);
    }
}
