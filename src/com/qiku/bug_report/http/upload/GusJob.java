package com.qiku.bug_report.http.upload;

public class GusJob {

	public final GusData mUploadData;
	public final IGusCallback mCallback;
	public final long mGusId;

	public volatile State mState = State.Idle;
	public volatile String Cookie = null;
	public volatile Integer mUploadId;
	public volatile long mUploadOffset;
	public volatile GusWS.GusRequest mRequest;
	public volatile long mUploadSize;
	public volatile byte mRetryCount = 0;
	public volatile byte mMaxRetries = 3;
	public volatile String mSessionId = null;
	public volatile int mUserId = 0;
	public volatile int mRootId = 0;
	public volatile int mBugReportFolderId = 0;
	public volatile int mRange = 0;
	public volatile boolean mNeedUpload = false;
	public volatile String mSarUrl = null;
	public volatile String mUploadIp = null;
	public volatile String mCallbackUri = null;
	public volatile String mSalAccessKeyId = null;
	public volatile String mRequestId = null;
	public volatile String mBucketName = null;
	public volatile String mSha1 = null;
	public volatile boolean isZip = false;

	public GusJob(GusData uploadData, IGusCallback callback) {
		mUploadData = uploadData;
		mCallback = callback;
		mGusId = mUploadData.mId;
		if (uploadData.mFilePath.contains(".zip")) {
			isZip = true;
		}

		// System.currentTimeMillis() +
		// mUploadData.mMediaURI.hashCode() +
		// mUploadData.mProgressAction.hashCode();
	}

	public boolean shouldRetry() {
		// return (mMaxRetries < 0 || mRetryCount < mMaxRetries) &&
		// !mRequest.mCancelled.get();
		return false;
	}

	public void upRetryCount() {
		mRetryCount++;
	}

	public enum State {
		Idle, GettingId, Uploading, GettingOffset, Logon, Logoff, FileList, GetSarUrl, FileUpload, FolderCreate
	}

	@Override
	public String toString() {
		return new StringBuilder().append(mGusId).append(":").append(mState)
				.append(":").append(mUploadData.mFilePath).toString();
	}
}
