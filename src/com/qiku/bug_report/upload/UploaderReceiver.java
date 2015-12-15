
package com.qiku.bug_report.upload;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.BatteryManager;
import android.util.Log;

import com.qiku.bug_report.BugReportApplication;
import com.qiku.bug_report.Constants;
import com.qiku.bug_report.TaskMaster;
import com.qiku.bug_report.helper.Util;
import com.qiku.bug_report.http.upload.GusData;
import com.qiku.bug_report.http.upload.GUS.ReturnCode;
import com.qiku.bug_report.model.ComplainReport;
import com.qiku.bug_report.model.ComplainReport.State;

public class UploaderReceiver extends BroadcastReceiver {
    public static final String tag = "BugReportUploaderReceiver";
    private TaskMaster mTaskMaster;
    private ReliableUploader mUploader;
    private boolean mPowerConnected = false;
    // Initialize to MAX_VALUE, so that we always upload in the case where we
    // never receive the
    // ACTION_BATTERY_CHANGED Intent.
    private int mCurrentBatteryLevel = Integer.MAX_VALUE;

    public UploaderReceiver(ReliableUploader uploader) {
        super();
        mUploader = uploader;
        mTaskMaster = ((BugReportApplication) uploader.getApplicationContext())
                .getTaskMaster();
    }

    public void start() {
        Log.i(tag, "started");
        IntentFilter filter = new IntentFilter();
        // put the ACTION_BATTERY_CHANGED action as the first filter so that
        // we get the sticky intent while registering it
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        filter.addAction(Constants.BUGREPORT_INTENT_UPLOAD_PROGRESS);
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        filter.addAction(Constants.BUGREPORT_INTENT_BATTERY_THRESHOLD_CHANGED);
        filter.addAction(Constants.BUGREPORT_INTENT_PAUSE_UPLOAD);
        filter.addAction(Constants.BUGREPORT_INTENT_REPORT_REMOVED);
        filter.addAction(Constants.BUGREPORT_INTENT_UPLOAD_PRIORITY_CHANGED);
        filter.addAction(Constants.BUGREPORT_INTENT_UPLOAD_PAUSED);
        Intent intent = mUploader.registerReceiver(this, filter);
        if (null != intent
                && Intent.ACTION_BATTERY_CHANGED.equals(intent.getAction())) {
            updatePowerState(intent);
        } else {
            Log.w(tag,
                    "No sticky ACTION_BATTERY_CHANGED available; using defaults");
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        // Log.i(tag, "onReceive : " + action);
        if (Constants.BUGREPORT_INTENT_UPLOAD_PROGRESS.equals(action)) { // uploading
                                                                         // progress
                                                                         // changed
            String fileName = intent.getStringExtra(GusData.URI_KEY);
            if (fileName != null) {
                ComplainReport report = mUploader.getCurrentUpload();
                if (report == null)
                    return;
                if (report.getState() != State.TRANSMITTING) // Upload has been
                                                             // canceled, but
                                                             // the upload
                                                             // thread may be
                                                             // still running
                                                             // in the
                                                             // background.
                    return;
                int sentLength = intent.getIntExtra(GusData.BYTES_SENT_KEY, 0);
                report.setUploadedBytes(sentLength);
                mTaskMaster.getBugReportDAO().updateReportUploadInfo(report,
                        false);
                // if (showNotification(mTaskMaster, report))
                // Notifications.showProgressNotification(
                // context,
                // context.getString(R.string.bug_prompt)
                // + report.getTitle(), (int) length,
                // sentLength, (int) report.getId(), false);
            }
        } else if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) { // data
                                                                             // conectivity
                                                                             // changed
            // if network is unavailable, cancel all uploading action
            if (!Util.isNetworkAvailable(context)) {
                Log.i(tag, "Network unavailable, cancelling all uploads");
                mUploader.cancelAll(ReturnCode.NETWORK_DISCONNECTED);
            }
        } else if (Constants.BUGREPORT_INTENT_BATTERY_THRESHOLD_CHANGED
                .equals(action)) { // battery threshold changed
            int userBatteryThreshold = intent.getIntExtra(
                    Constants.BUGREPORT_INTENT_EXTRA_BATTERY_THRESHOLD,
                    Constants.DEFAULT_BATTERY_PERCENT);
            // if the battery level is below the threshold and no external power
            // connected, cancel uploads
            if (userBatteryThreshold > mCurrentBatteryLevel && !mPowerConnected) {
                Log.i(tag,
                        "User battery threshold changed, cancel all uploads. level="
                                + mCurrentBatteryLevel + ", threshold ="
                                + userBatteryThreshold);
//                mUploader.cancelAll(ReturnCode.BATTERY_LOW);
            }
        } else if (Intent.ACTION_BATTERY_CHANGED.equals(action)) { // battery
                                                                   // state
                                                                   // changed
            // save the current battery info for later use
            updatePowerState(intent);

            int userBatteryThreshold = mTaskMaster.getConfigurationManager()
                    .getUserSettings().getBatteryPercent().getValue();
            // if the battery level is below the threshold and no external power
            // connected, cancel uploads
            if (userBatteryThreshold > mCurrentBatteryLevel && !mPowerConnected) {
                Log.i(tag, "Power disconnected, cancel all uploads. level="
                        + mCurrentBatteryLevel + ", threshold ="
                        + userBatteryThreshold);
//                mUploader.cancelAll(ReturnCode.BATTERY_LOW);
            }
        } else if (Constants.BUGREPORT_INTENT_PAUSE_UPLOAD.equals(action)) { // user
                                                                             // pauses
                                                                             // the
                                                                             // uploader
            mUploader.cancelAll(ReturnCode.UPLOAD_PAUSED);
        } else if (Constants.BUGREPORT_INTENT_REPORT_REMOVED.equals(action)) { // user
                                                                               // removes
                                                                               // the
                                                                               // report
            ComplainReport report = (ComplainReport) intent
                    .getParcelableExtra(Constants.BUGREPORT_INTENT_EXTRA_REPORT);
            if (report != null)
                mUploader.cancel(report, ReturnCode.UPLOAD_DELETED);
        } else if (Constants.BUGREPORT_INTENT_UPLOAD_PRIORITY_CHANGED
                .equals(action)) { // upload priority changed
            ComplainReport report = mUploader.getCurrentUpload();
            if (report == null)
                return;
            int fromPriority = intent.getIntExtra(
                    Constants.BUGREPORT_INTENT_EXTRA_PRIORITY_FROM, 0);
            int toPriority = intent.getIntExtra(
                    Constants.BUGREPORT_INTENT_EXTRA_PRIORITY_TO, 0);
            int currentUploadPriority = report.getPriority();
            if (currentUploadPriority <= Math.max(fromPriority, toPriority)
                    && currentUploadPriority >= Math.min(fromPriority,
                            toPriority)) {
                mUploader.cancel(report, ReturnCode.UPLOAD_PAUSED);
            }
        } else if (Constants.BUGREPORT_INTENT_UPLOAD_PAUSED.equals(action)) {
            ComplainReport report = mUploader.getCurrentUpload();
            if (report == null)
                return;
            long[] ids = intent
                    .getLongArrayExtra(Constants.BUGREPORT_INTENT_EXTRA_REPORT_IDS);
            if (ids == null)
                return;
            for (int i = 0; i < ids.length; i++) {
                if (ids[i] == report.getId()) {
                    mUploader.cancel(report, ReturnCode.UPLOAD_PAUSED);
                    break;
                }
            }
        }
    }

    private void updatePowerState(Intent intent) {
        // 0 means it is on battery, others are on different power sources
        mPowerConnected = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0;
        // Using Math.max() below should not be needed, and is only there in
        // case the system is
        // behaving badly. Since we're a debug app, it's really important that
        // we be able to
        // upload reports on unstable devices.
        mCurrentBatteryLevel = Math.max(
                intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0), 0);
        Log.i(tag, "updatePowerState, power connected : " + mPowerConnected
                + ", level : " + mCurrentBatteryLevel);
    }

    public boolean isPowerConnected() {
        return mPowerConnected;
    }

    public int getCurrentBatteryLevel() {
        return mCurrentBatteryLevel;
    }

    public void stop() {
        Log.i(tag, "stopped");
        mUploader.unregisterReceiver(this);
    }
}
