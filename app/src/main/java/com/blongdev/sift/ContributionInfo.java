package com.blongdev.sift;

/**
 * Created by Brian on 2/24/2016.
 */
public class ContributionInfo {
    public String mUsername;
    public long mUserId;
    public long mPost;
    public String mBody;
    public int mPoints;
    public int mComments;
    public long mAge;
    public String mServerId;
    public int mContributionType;
    public int mVote;

    public static final int CONTRIBUTION_POST = 0;
    public static final int CONTRIBUTION_COMMENT = 1;
}
