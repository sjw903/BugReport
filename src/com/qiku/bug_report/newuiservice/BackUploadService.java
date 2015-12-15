
package com.qiku.bug_report.newuiservice;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import com.qiku.bug_report.BugReportApplication;
import com.qiku.bug_report.Constants;
import com.qiku.bug_report.TaskMaster;
import com.qiku.bug_report.model.ComplainReport;

public class BackUploadService extends Service {
    public static final String tag = "BackUploadService";
    protected Intent mStartItent;
    protected ComplainReport mCurrentReport = null;
    private TaskMaster mTaskMaster;

    private BroadcastReceiver mBugreportServiceReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            Log.i(tag, "received broadcast");
            String action = intent.getAction();
            if (Constants.BUGREPORT_INTENT_BUGREPORT_END
                    .equalsIgnoreCase(action)) {
                onCollectorEnd(intent);
            }
        }
    };

    public void onCreate() {
        mTaskMaster = ((BugReportApplication) getApplicationContext())
                .getTaskMaster();
        IntentFilter bugreportFilter = new IntentFilter();
        bugreportFilter.addAction(Constants.BUGREPORT_INTENT_BUGREPORT_END);
        registerReceiver(mBugreportServiceReceiver, bugreportFilter);
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null
                && intent
                        .getParcelableExtra(Constants.BUGREPORT_INTENT_EXTRA_REPORT) != null) {
            mCurrentReport = intent
                    .getParcelableExtra(Constants.BUGREPORT_INTENT_EXTRA_REPORT);
            Log.i(tag, mCurrentReport.getLogPath());
        }
        return START_NOT_STICKY;
    }

    private void onCollectorEnd(Intent intent) {
        if (intent != null) {
            new Thread() {
                public void run() {
                    if (mCurrentReport != null) {
                        Message msg = Message.obtain();
                        msg.what = TaskMaster.TRONXYZ_BUG_REPORT_SEND_LOG;
                        msg.obj = mCurrentReport;
                        mTaskMaster.sendMessage(msg);
                    }
                }
            }.start();
            // Toast.makeText(BackUploadService.this, "正在努力为您提交报告",
            // Toast.LENGTH_SHORT)
            // .show();
            stopSelf();
        }
    }

    public void onDestroy() {
        Log.i(tag, "onDestroy()");
        unregisterReceiver(mBugreportServiceReceiver);
        super.onDestroy();
    }

    public IBinder onBind(Intent paramIntent) {
        return null;
    }

}
