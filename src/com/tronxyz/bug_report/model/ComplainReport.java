package com.tronxyz.bug_report.model;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.tronxyz.bug_report.R;

@SuppressLint("SimpleDateFormat")
public class ComplainReport implements Parcelable {

    public static final Parcelable.Creator<ComplainReport> CREATOR = new Parcelable.Creator<ComplainReport>() {
        public ComplainReport createFromParcel(Parcel in) {
            return new ComplainReport(in);
        }

        public ComplainReport[] newArray(int size) {
            return new ComplainReport[size];
        }
    };

    private long id;
    private String logPath;

    public enum State {
        BUILDING,
        WAIT_USER_INPUT,

        READY_TO_UPLOAD,
        READY_TO_COMPRESS,
        COMPRESSING,
        COMPRESSION_PAUSED,
        READY_TO_TRANSMIT,
        TRANSMITTING,
        READY_TO_COMPLETE,
        COMPLETING,

        READY_TO_ARCHIVE,
        ARCHIVED_FULL,
        ARCHIVED_PARTIAL,

        BUILD_FAILED,
        COMPRESS_FAILED,
        TRANSMIT_FAILED,
        COMPLETE_FAILED,
        USER_DELETED_OUTBOX,
        USER_DELETED_DRAFT;

        public static boolean isUploadingState(State state){
            return READY_TO_UPLOAD.equals(state) ||
                   COMPRESSING.equals(state) ||
                   READY_TO_TRANSMIT.equals(state) ||
                   TRANSMITTING.equals(state) ||
                   READY_TO_COMPLETE.equals(state);
        }

        public static String stateToString(Context ctx, State state) {
            switch(state){
            case BUILDING:
                return ctx.getString(R.string.building_report);
            case WAIT_USER_INPUT:
                return ctx.getString(R.string.waiting_user_input_report);
            case READY_TO_UPLOAD:
                return ctx.getString(R.string.ready_to_upload_report);
            case READY_TO_COMPRESS:
                return ctx.getString(R.string.ready_to_compress_report);
            case COMPRESSING:
                return ctx.getString(R.string.compressing_report);
            case COMPRESSION_PAUSED:
                return ctx.getString(R.string.compressing_paused_report);
            case READY_TO_TRANSMIT:
                return ctx.getString(R.string.ready_to_transmit_report);
            case TRANSMITTING:
                return ctx.getString(R.string.transmitting_report);
            case READY_TO_COMPLETE:
                return ctx.getString(R.string.ready_to_complete_report);
            case COMPLETING:
                return ctx.getString(R.string.completing_report);
            case READY_TO_ARCHIVE:
                return ctx.getString(R.string.ready_to_archive_report);
            case ARCHIVED_FULL:
                return ctx.getString(R.string.archived_full_report);
            case ARCHIVED_PARTIAL:
                return ctx.getString(R.string.archived_partial_report);
            case BUILD_FAILED:
                return ctx.getString(R.string.build_failed_report);
            case COMPRESS_FAILED:
                return ctx.getString(R.string.compress_failed_report);
            case TRANSMIT_FAILED:
                return ctx.getString(R.string.transmit_failed_report);
            case COMPLETE_FAILED:
                return ctx.getString(R.string.complete_failed_report);
            case USER_DELETED_OUTBOX:
                return ctx.getString(R.string.user_deleted_outbox_report);
            case USER_DELETED_DRAFT:
                return ctx.getString(R.string.user_deleted_draft_report);
            default:
                return state.name();
            }

        }

        public static String categoryToString(Context ctx, State state){
            switch(state){
            case READY_TO_UPLOAD:
                return ctx.getString(R.string.ready_to_upload_report);
            case READY_TO_COMPRESS:
            case READY_TO_TRANSMIT:
                return ctx.getString(R.string.ready_to_transmit_report);
            case READY_TO_COMPLETE:
                return ctx.getString(R.string.queued_for_upload);
            case COMPRESSING:
                return ctx.getString(R.string.compressing_report);
            case TRANSMITTING:
            case COMPLETING:
                return ctx.getString(R.string.transmitting_report);
            case READY_TO_ARCHIVE:
            case ARCHIVED_FULL:
            case ARCHIVED_PARTIAL:

            case BUILD_FAILED:
            case COMPRESS_FAILED:
            case TRANSMIT_FAILED:
            case COMPLETE_FAILED:
            case USER_DELETED_OUTBOX:
            case USER_DELETED_DRAFT:

            default:
                return stateToString(ctx, state);
            }
        }
    }

    public enum Type{
        AUTO,
        USER;

        public static String typeToString(Context ctx, Type type) {
            switch(type){
            case AUTO:
                return ctx.getString(R.string.type_auto);
            case USER:
                return ctx.getString(R.string.type_user);
            default:
                return type.name();
            }
        }
    }

    private State state = null;
    private Type type = null;
    private Date createTime = null;
    private String category = null;
    private String summary = null;
    private String freeText = null;
    private String uploadId = null;
    private int mUploadPaused;
    private int mPriority;
    private int mUploadedBytes;
    private String screenshotPath = null;
    private String attachment = null;
    private String apVersion = null;
    private String bpVersion = null;
    private String deleteAfterUploading = "false";
    private String mApkVersion = null;
    private int mShowNotification = 1;

    public ComplainReport() {
        state = State.WAIT_USER_INPUT;
        type = Type.USER;
    }

    public ComplainReport(Parcel in) {
        this();
        id = in.readLong();
        logPath = in.readString();
        state = State.valueOf(in.readString());
        type = Type.valueOf(in.readString());
        createTime = new Date(in.readLong());
        category = in.readString();
        summary = in.readString();
        freeText = in.readString();
        uploadId = in.readString();
        mUploadPaused = in.readInt();
        mPriority = in.readInt();
        mUploadedBytes = in.readInt();
        screenshotPath = in.readString();
        attachment = in.readString();
        apVersion = in.readString();
        bpVersion = in.readString();
        deleteAfterUploading = in.readString();
        mApkVersion = in.readString();
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeLong(id);
        out.writeString(logPath);
        out.writeString(state.name());
        out.writeString(type.name());
        out.writeLong(createTime.getTime());
        out.writeString(category);
        out.writeString(summary);
        out.writeString(freeText);
        out.writeString(uploadId);
        out.writeInt(mUploadPaused);
        out.writeInt(mPriority);
        out.writeInt(mUploadedBytes);
        out.writeString(screenshotPath);
        out.writeString(attachment);
        out.writeString(apVersion);
        out.writeString(bpVersion);
        out.writeString(deleteAfterUploading);
        out.writeString(mApkVersion);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getLogPath() {
        return logPath;
    }

    public void setLogPath(String logPath) {
        this.logPath = logPath;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public Type getType(){
        return type;
    }

    public void setType(Type type){
        this.type = type;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getTitle() {
        if(Type.AUTO == type){
            //For auto-report, the category is the DEAM scenario, summary is the DropBox tag
            //We should use the scenario as the title of the auto-report if it presents
            return TextUtils.isEmpty(category) ? summary : category;
        }else{
            return TextUtils.isEmpty(summary) ? category : summary;
        }
    }

    public String getFreeText() {
        return freeText;
    }

    public void setFreeText(String freeText) {
        this.freeText = freeText;
    }

    public String getUploadId() {
        return uploadId;
    }

    public void setUploadId(String uploadId) {
        this.uploadId = uploadId;
    }

    public int getUploadPaused() {
        return mUploadPaused;
    }

    public void setUploadPaused(int uploadPaused) {
        this.mUploadPaused = uploadPaused;
    }

    public boolean isUploadPaused(){
        return mUploadPaused == 1;
    }

    public int getPriority() {
        return mPriority;
    }

    public void setPriority(int priority) {
        this.mPriority = priority;
    }

    public void setUploadedBytes(int bytes){
        this.mUploadedBytes = bytes;
    }

    public int getUploadedBytes(){
        return mUploadedBytes;
    }

    public String getScreenshotPath() {
        return screenshotPath;
    }

    public void setScreenshotPath(String screenshotPath) {
        this.screenshotPath = screenshotPath;
    }

    public String getAttachment() {
        return attachment;
    }

    public List<String> getAttachments() {
        if (TextUtils.isEmpty(getAttachment()))
            return Collections.emptyList();
        else
            return Arrays.asList(getAttachment().split("\\^\\|\\^"));
    }

    public void setAttachment(String attachment) {
        this.attachment = attachment;
    }

    public String getApVersion() {
        return apVersion;
    }

    public void setApVersion(String apVersion) {
        this.apVersion = apVersion;
    }

    public String getBpVersion() {
        return bpVersion;
    }

    public void setBpVersion(String bpVersion) {
        this.bpVersion = bpVersion;
    }

    public String getApkVersion(){
        return mApkVersion;
    }

    public void setApkVersion(String version){
        this.mApkVersion = version;
    }

    public boolean isEditable(){
        return State.WAIT_USER_INPUT.equals(state) && Type.USER.equals(type);
    }

    public int getShowNotification(){
        return mShowNotification;
    }

    public boolean isNotificationEnabled(){
        return mShowNotification == 1;
    }

    public void setShowNotification(int showNotification){
        mShowNotification = showNotification;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public String toString() {
        return category == null ? "None" : category + " : " + new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss.SSSZ").format(
                getCreateTime()) + ", " + state.name();
    }

    public int hashCode(){
        return createTime.hashCode();
    }
}
