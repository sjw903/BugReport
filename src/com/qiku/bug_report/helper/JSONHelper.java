package com.qiku.bug_report.helper;

import org.json.JSONException;
import org.json.JSONObject;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.content.Context;
import android.os.Build;
import android.text.TextUtils;

import com.qiku.bug_report.BugReportApplication;
import com.qiku.bug_report.Constants;
import com.qiku.bug_report.TaskMaster;
import com.qiku.bug_report.model.ComplainReport;
import com.qiku.bug_report.model.UserSettings;
import com.qiku.bug_report.model.ComplainReport.Type;
public class JSONHelper {

    public final static String REQUEST="request";
    public final static String DEV="devInfo";
    public final static String DEV_UID="uid";
    public final static String DEV_VERSION_SDK_INT="VERSION_SDK_INT";
    public final static String DEV_MODEL="MODEL";
    public final static String DEV_BOARD="BOARD";
    public final static String DEV_BRAND="BRAND";
    public final static String DEV_SERIAL="SERIAL";
    public final static String DEV_TELEPHONY_DEVICEID="TELEPHONY_DEVICE_ID";
    public final static String DEV_WIFI_MAC="WIFI_MAC";
    public final static String DEV_MEM="totalMem";
    public final static String DEV_BLUR="blurdevice_flag";
    public final static String DEV_SWV="softwareVersion";
    public final static String DEV_PRODUCT="PRODUCT";
    public final static String DEV_BUILD_ID="BUILD_ID";
    public final static String DEV_TYPE="TYPE";
    public final static String DEV_BP_VERSOIN="bpVersion";
    public final static String DEV_CLOUD="cloud";

    public final static String USER="userInfo";
    public final static String USER_FNAME="name";
    public final static String USER_EMAIL="email";
    public final static String USER_PHONE="phone";
    public final static String USER_ID="coreid";

    public final static String ISSUE_CATEGORY="category";
    public final static String ISSUE_SUMMARY="summary";
    public final static String ISSUE_PROCESS="process";
    public final static String ISSUE_DESC="description";
    public final static String ISSUE_CREATE_TIME="uploadDate";
    public final static String ISSUE_UPLOAD_ID="upload_id";

    public final static String CLIENT_CRT_VERSION="bugreport_ver_creation";
    public final static String CLIENT_UPLOAD_VERSION="bugreport_ver_upload";

    public final static String RESP="response";
    public final static String RESP_CODE="code";
    public final static String RESP_MSG="message";
    public final static String RESP_CR_URL="CR_URL";
    public final static int RESP_CODE_OK=200;
    public final static int RESP_CODE_BAD_REQUEST=400;
    public final static int RESP_CODE_DATA_NOT_FOUND=404;
    public final static int RESP_CODE_SERVER_ERROR=500;

    public static JSONObject toCreateReportRequest(Context context, ComplainReport report) throws JSONException {
        TaskMaster taskMaster =
            ((BugReportApplication) context.getApplicationContext()).getTaskMaster();
        ActivityManager activityManager =
            (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        MemoryInfo mi = new MemoryInfo();
        activityManager.getMemoryInfo(mi);

        JSONObject deviceInfo = new JSONObject();
        deviceInfo.put(DEV_MEM, (int) (mi.availMem / 1048576L));
        deviceInfo.put(DEV_VERSION_SDK_INT, String.valueOf(Build.VERSION.SDK_INT));

        String uid = DeviceID.getInstance().getId(context);
        if (!TextUtils.isEmpty(uid))
            deviceInfo.put(DEV_UID, uid);
        if (!TextUtils.isEmpty(report.getApVersion()))
            deviceInfo.put(DEV_SWV, report.getApVersion());
        if (!TextUtils.isEmpty(report.getBpVersion()))
            deviceInfo.put(DEV_BP_VERSOIN, report.getBpVersion());
        if(!TextUtils.isEmpty(Build.ID))
            //deviceInfo.put(DEV_BUILD_ID, Build.ID);
            deviceInfo.put(DEV_BUILD_ID, Build.VERSION.INCREMENTAL);

        if (!TextUtils.isEmpty(Build.PRODUCT))
            deviceInfo.put(DEV_PRODUCT, Build.PRODUCT);

        if (!TextUtils.isEmpty(Build.MODEL))
            deviceInfo.put(DEV_MODEL, Build.MODEL);

        if (!TextUtils.isEmpty(Build.BOARD))
            deviceInfo.put(DEV_BOARD, Build.BOARD);

        if (!TextUtils.isEmpty(Build.BRAND))
            deviceInfo.put(DEV_BRAND, Build.BRAND);

        if (!TextUtils.isEmpty(Build.SERIAL))
            deviceInfo.put(DEV_SERIAL, Build.SERIAL);

        String deviceId = DeviceID.getInstance().getTelephonyDeviceId(context);
        if (!TextUtils.isEmpty(deviceId))
            deviceInfo.put(DEV_TELEPHONY_DEVICEID, deviceId);

        String macAddress = DeviceID.getInstance().getMacAddress(context);
        if (!TextUtils.isEmpty(macAddress))
            deviceInfo.put(DEV_WIFI_MAC, macAddress);

        if (!TextUtils.isEmpty(Build.TYPE))
            deviceInfo.put(DEV_TYPE, Build.TYPE);

        // add user settings;
        JSONObject userInfo = new JSONObject();
        UserSettings us = taskMaster.getConfigurationManager().getUserSettings();
        if( us != null ){
            if (!TextUtils.isEmpty(us.getEmail()))
                userInfo.put(USER_EMAIL, us.getEmail());
            if (!TextUtils.isEmpty(us.getFirstName()))
                userInfo.put(USER_FNAME, us.getFirstName());
            if (!TextUtils.isEmpty(us.getPhone()))
                userInfo.put(USER_PHONE, us.getPhone());
            if (!TextUtils.isEmpty(us.getCoreID()))
                userInfo.put(USER_ID, us.getCoreID());
            if (TextUtils.isEmpty(us.getEmail()) && !TextUtils.isEmpty(us.getCoreID())) {
                userInfo.put(USER_EMAIL, us.getCoreID().trim() + Constants.TRONXYZ_EMAIL_DOMAIN);
            }
        }

        JSONObject request = new JSONObject();
        request.put(DEV, deviceInfo);
        request.put(USER, userInfo);

        if (!TextUtils.isEmpty(report.getCategory()))
            request.put(ISSUE_CATEGORY,
                            (Type.AUTO == report.getType() ? "A:" : "U:") + report.getCategory());

        if (!TextUtils.isEmpty(report.getSummary())) {
            String[] a = report.getSummary().split(":", 2);
            if(a.length == 2) {
                request.put(ISSUE_SUMMARY, a[0]);
                request.put(ISSUE_PROCESS, TextUtils.isEmpty(a[1]) ? a[0] : a[1]);
            }
            if(a.length == 1) {
                request.put(ISSUE_SUMMARY, a[0]);
                request.put(ISSUE_PROCESS, a[0]);
            }
        }

        if (!TextUtils.isEmpty(report.getFreeText())) {
            request.put(ISSUE_DESC, report.getFreeText());
        }

        int uploadId = report.getUploadId() != null ? Integer.parseInt(report.getUploadId()) : -1;
        if (uploadId > 0) {
            request.put(ISSUE_UPLOAD_ID, uploadId);
        }

        if(!TextUtils.isEmpty(report.getApkVersion()))
            request.put(CLIENT_CRT_VERSION, report.getApkVersion());

        String currentApkVer = Util.getAppVersionName(context);
        if(!TextUtils.isEmpty(currentApkVer))
            request.put(CLIENT_UPLOAD_VERSION, currentApkVer);

        request.put(ISSUE_CREATE_TIME, report.getCreateTime().getTime());
        JSONObject result = new JSONObject();
        result.put(REQUEST, request);
        return result;
    }
}
