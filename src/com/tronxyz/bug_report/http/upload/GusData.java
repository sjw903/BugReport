package com.tronxyz.bug_report.http.upload;

import android.os.Parcel;
import android.os.Parcelable;

public class GusData implements Parcelable {

    public static final String URI_KEY = "com.tronxyz.bug_report.upload.uri";
    public static final String BYTES_SENT_KEY = "com.tronxyz.bug_report.upload.bytesSent";
    public static final String UPLOAD_ID_KEY = "com.tronxyz.bug_report.upload.id";

    public static final int MIN_PROGRESS_BYTES = 8192;

    public final String mFilePath;
    public final String mProgressAction;
    public final String mComponentName;
    public final int mProgressBytes;
    public final long mId;

    public volatile byte mMaxRetries; // OPTIONAL - How many times to retry
    // uploading this sucker (default is 0)
    public volatile boolean mWifiOnly; // OPTIONAL - This will only be uploaded
    // on WiFi, otherwise we'll wait (default
    // is false)
    public String mCategory; // OPTIONAL - This is the category that will be

    // added to every intent related to this upload
    // (default is empty)

    public GusData(String filePath, String progressAction, String componentName, int progressBytes) {
        mFilePath = filePath;
        mId = System.currentTimeMillis() + mFilePath.hashCode();
        mProgressAction = progressAction;
        mComponentName = componentName;
        mProgressBytes = progressBytes;
    }

    public static final Parcelable.Creator<GusData> CREATOR = new Parcelable.Creator<GusData>() {

        public GusData createFromParcel(Parcel in) {
            return new GusData(in);
        }

        public GusData[] newArray(int size) {
            return new GusData[size];
        }
    };

    private GusData(Parcel in) {
        mFilePath = in.readString();
        mId = in.readLong();
        mProgressAction = in.readString();
        mComponentName = in.readString();
        mProgressBytes = in.readInt();
        mMaxRetries = in.readByte();
        mCategory = in.readString();
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeString(mFilePath);
        out.writeLong(mId);
        out.writeString(mProgressAction);
        out.writeString(mComponentName);
        out.writeInt(mProgressBytes);
        out.writeByte(mMaxRetries);
        out.writeString(mCategory);
    }
}
