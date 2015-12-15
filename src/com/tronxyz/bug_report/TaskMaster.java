
package com.tronxyz.bug_report;

import android.content.Context;
import android.content.Intent;
import android.os.DropBoxManager;
import android.os.Handler;
import android.os.Message;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.tronxyz.bug_report.conf.ConfigurationManager;
import com.tronxyz.bug_report.db.BugReportDAO;
import com.tronxyz.bug_report.helper.Util;
import com.tronxyz.bug_report.log.DropBoxEventHandler;
import com.tronxyz.bug_report.log.ShellScriptLogCollector;
import com.tronxyz.bug_report.model.ComplainReport;
import com.tronxyz.bug_report.model.UserSettings;
import com.tronxyz.bug_report.upload.ReliableUploader;

import java.util.ArrayList;
import java.util.List;

public class TaskMaster extends Handler {
    static final String tag = "BugReportTaskMaster";
    public static final int TRONXYZ_BUG_REPORT_DEVICE_START = 0;
    public static final int TRONXYZ_BUG_REPORT_SEND_LOG = 4;
    public static final int TRONXYZ_BUG_REPORT_CHECK_UNSENT_LOG = 6;
    public static final int TRONXYZ_BUG_REPORT_NETWORK_AVAILABLE = 8;
    public static final int TRONXYZ_BUG_REPORT_POWER_CONNECTED = 9;
    public static final int TRONXYZ_BUG_REPORT_BATTERY_THRESHOLD_CHANGED = 10;
    public static final int TRONXYZ_BUG_REPORT_STARTED = 11;
    public static final String BUGREPORT_INTENT_EXTRA_DROPBOX_BACKLOG_TRIGGER = "tronxyz.intent.extra.BUGREPORT.DROPBOX.BACKLOG.TRIGGER";

    private ShellScriptLogCollector mLogCollector = null;
    private BugReportDAO mDao = null;
    private BugReportApplication mContext = null;
    private ConfigurationManager cm = null;

    public TaskMaster(BugReportApplication context) {
        mContext = context;
        mLogCollector = new ShellScriptLogCollector(this);
        mDao = new BugReportDAO(context);
        cm = new ConfigurationManager(context);
    }

    public void handleMessage(Message msg) {
        switch (msg.what) {
            case TRONXYZ_BUG_REPORT_DEVICE_START:
                onSystemStart();
            case TRONXYZ_BUG_REPORT_STARTED: {
                checkUncompletedReports();
                uploadReport(null);
                checkUnarchivedReports();
                break;
            }
            case TRONXYZ_BUG_REPORT_SEND_LOG:
                Log.i(tag, "Sending report");
                if (msg.obj != null && msg.obj instanceof ComplainReport) {
                    ComplainReport report = (ComplainReport) msg.obj;
                    mDao.updateReport(report);
                    uploadReport(report);
                }
                break;
            case TRONXYZ_BUG_REPORT_NETWORK_AVAILABLE:
                Log.i(tag, "Network became available, retry upload now...");
                reuploadReports();
                break;
            case TRONXYZ_BUG_REPORT_POWER_CONNECTED:
                Log.i(tag, "Power connected, retry upload now...");
                reuploadReports();
                break;
            case TRONXYZ_BUG_REPORT_BATTERY_THRESHOLD_CHANGED:
                Log.i(tag, "Battery threshold changed, retry upload now...");
                reuploadReports();
                break;
            default:
                super.handleMessage(msg);
                break;
        }
    }

    private void onSystemStart() {
        // start the DropBoxEventHandler to handle DropBox events in the backlog
        Intent dropboxEventHandler = new Intent(mContext,
                DropBoxEventHandler.class);
        // The BUGREPORT_INTENT_EXTRA_DROPBOX_BACKLOG_TRIGGER is a non-existent
        // tag in the DropBox.
        // So the DropBoxEventHandler won't actually process it but will process
        // the events put
        // to the DropBox before System.currentTimeMillis()
        dropboxEventHandler.putExtra(DropBoxManager.EXTRA_TAG,
                BUGREPORT_INTENT_EXTRA_DROPBOX_BACKLOG_TRIGGER);
        dropboxEventHandler.putExtra(DropBoxManager.EXTRA_TIME,
                System.currentTimeMillis());
        mContext.startService(dropboxEventHandler);
        Log.i("DropBoxEventHandler: ", "Started");
        // setup user contact info is missing
        if (!cm.getUserSettings().isContactInfoComplete()) {
            UserSettings settings = cm.getUserSettings();
            settings.setCoreID("unknown");
            settings.setEmail("");
            settings.setFirstName("unknown");
            TelephonyManager mTm = (TelephonyManager) mContext
                    .getSystemService(Context.TELEPHONY_SERVICE);
            String num = mTm.getLine1Number();
            if (num == null || num.isEmpty()) {
                settings.setPhone("");
            } else {
                settings.setPhone(num);
            }
            cm.saveUserSettings(settings);
        }
    }

    private void checkUncompletedReports() {
        new Thread(new Runnable() {
            public void run() {
                try {
                    Log.d(tag, "Checking untracked and unsent reports");
                    mLogCollector.collectUntrackedReport();
                    int pendingReportCount = mDao.getReportsByState(
                            ComplainReport.State.BUILDING,
                            ComplainReport.State.WAIT_USER_INPUT).size();
                    if (pendingReportCount != 0) {
                        List<ComplainReport> mReports = getAllReports();
                        List<ComplainReport> delList = new ArrayList<ComplainReport>();
                        for (ComplainReport report : mReports) {
                            if (report.getState().equals(ComplainReport.State.BUILDING)
                                    || report.getState().equals(
                                            ComplainReport.State.WAIT_USER_INPUT)) {
                                delList.add(report);
                                deleteComplainReport(report);
                            }
                        }
                        mReports.removeAll(delList);
                    }
                } catch (BugReportException e) {
                    Log.e(tag, "Error occured in checkUncompletedReports()", e);
                }
            }
        }).start();
    }

    private void checkUnarchivedReports() {
        new Thread(new Runnable() {
            public void run() {
                Log.d(tag, "Checking unarchived reports");
                List<ComplainReport> reports = mDao
                        .getReportsByState(ComplainReport.State.READY_TO_ARCHIVE);
                for (ComplainReport report : reports) {
                    Util.removeFiles(new String[] {
                            report.getLogPath(),
                            report.getScreenshotPath()
                    });
                    report.setState(ComplainReport.State.ARCHIVED_FULL);
                    mDao.updateReportUploadInfo(report, true);
                }
            }
        }).start();
    }

    public ConfigurationManager getConfigurationManager() {
        return cm;
    }

    // upload the reports that already got upload id
    private void reuploadReports() {
        Log.d(tag, "reuploadReports()");
        if (!Util.isWIFIConnected(mContext) || // wifi 没连接 或者 （不允许移动上传 &&
                                               // （2g或3g可用））
                (cm.getUserSettings().getWlanUploadAllowed().getValue() &&
                (Util.is2GConnected(mContext) || Util.is3GConnected(mContext))))
            return;
        uploadReport(null);
    }

    /**
     * @param report the report to be uploaded, if it is null, then it will just
     *            start the upload service to upload reports already in the
     *            upload queue
     */
    private void uploadReport(ComplainReport report) {
        Log.d(tag, "uploadReport " + report);
        // start the upload service
        Intent intent = new Intent(mContext, ReliableUploader.class);
        intent.putExtra(Constants.BUGREPORT_INTENT_EXTRA_REPORT, report);
        mContext.startService(intent);
    }

    public List<ComplainReport> getAllReports() {
        return mDao.getAllReports();
    }

    public boolean deleteComplainReport(ComplainReport report) {
        if (report == null)
            return false;
        Log.i(tag, "Deleting Complain Report " + report.getCategory() + ", "
                + report.getLogPath());
        if (mDao.deleteReport(report)) {
            Util.removeFiles(new String[] {
                    report.getLogPath(),
                    report.getScreenshotPath()
            });
            return true;
        }
        return false;
    }

    public ShellScriptLogCollector getLogCollector() {
        return mLogCollector;
    }

    public void onDestory() {
        mDao.close();
    }

    public BugReportDAO getBugReportDAO() {
        return mDao;
    }

    public BugReportApplication getApplicationContext() {
        return mContext;
    }
}
