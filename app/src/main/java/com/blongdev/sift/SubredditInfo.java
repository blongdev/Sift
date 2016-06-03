package com.blongdev.sift;

/**
 * Created by Brian on 2/24/2016.
 */
public class SubredditInfo {
    public String mName;
    public long mId;
    public int mPoints;
    public int mAge;
    public String mServerId;
    public String mDescription;
    public long mSubscribers;

    public static final int SUBREDDIT_PAGINATOR = 0;
    public static final int SUBMISSION_SEARCH_PAGINATOR = 1;
    public static final int USER_CONTRIBUTION_PAGINATOR = 2;

    public static final String CATEGORY_SUBMITTED = "submitted";
    public static final String CATEGORY_COMMENTS = "comments";
    public static final String CATEGORY_OVERVIEW = "overview";
    public static final String CATEGORY_SAVED = "saved";

}
