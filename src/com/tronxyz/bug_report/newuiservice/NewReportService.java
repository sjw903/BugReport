
package com.tronxyz.bug_report.newuiservice;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

import com.tronxyz.bug_report.BugReportApplication;
import com.tronxyz.bug_report.BugReportException;
import com.tronxyz.bug_report.Constants;
import com.tronxyz.bug_report.R;
import com.tronxyz.bug_report.TaskMaster;
import com.tronxyz.bug_report.conf.bean.Deam;
import com.tronxyz.bug_report.helper.Notifications;
import com.tronxyz.bug_report.helper.Util;
import com.tronxyz.bug_report.log.DeamEventHandler;
import com.tronxyz.bug_report.model.ComplainReport;
import com.tronxyz.bug_report.model.UserDeamEvent;

import java.io.File;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

public class NewReportService extends Service {
    public static final String tag = "NewReportService";
    private State mState = State.idle;
    private BugreportTimer mScriptTimer;
    private TaskMaster mTaskMaster;
    private Map<String, Integer> mReportEditorCounter;
    private Map<String, ComplainReport> mReportsInCollecting;
    private volatile int mNumberOfRunningTasks = 0;
    private Intent mUserExitIntent;
    public static NewReportService mBugS = null;
    public long reportID = 0;

    public void onCreate() {
        Log.i(tag, "onCreate()");
        mTaskMaster = ((BugReportApplication) getApplicationContext())
                .getTaskMaster();
        mScriptTimer = new BugreportTimer();
        mReportEditorCounter = new Hashtable<String, Integer>();
        mReportsInCollecting = new HashMap<String, ComplainReport>();
        mBugS = this;
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        Log.i(tag, "onStartCommand() : " + action);
        if (Constants.BUGREPORT_INTENT_BUGREPORT_START.equalsIgnoreCase(action)) {
            onCollectorStart(intent);
        } else if (Constants.BUGREPORT_INTENT_BUGREPORT_ERROR
                .equalsIgnoreCase(action)) {
            onCollectorError(intent);
        } else if (Constants.BUGREPORT_INTENT_BUGREPORT_END
                .equalsIgnoreCase(action)) {
            onCollectorEnd(intent);
        } else if (Constants.BUGREPORT_INTENT_COLLECT_CATEGORY_LOG
                .equalsIgnoreCase(action)) {
            collectLog(intent);
        } else if (Constants.BUGREPORT_INTENT_DISCARD_REPORT
                .equalsIgnoreCase(action)) {
            discardReport(intent);
        } else if (Constants.BUGREPORT_INTENT_SAVE_REPORT
                .equalsIgnoreCase(action)) {
            updateReportWithUserInput(intent);
        }
        return START_NOT_STICKY;
    }

    private void onCollectorStart(final Intent intent) {
        Log.i(tag, "collectorStart");
        if (!mState.equals(State.idle)) {
            Log.i(tag, "mState is idle");
            return;
        }
        Log.i(tag, "after idle");
        mState = State.collecting;
        // start a timer to prevent the shell script from timeout
        mScriptTimer.start(intent);
        createReport(intent);
        Intent newIntent = new Intent();
        newIntent.setAction(Constants.BUGREPORT_INTENT_BUGREPORT_START);
        newIntent.putExtra(Constants.REPORT_LOG_PATH,
                intent.getStringExtra(Constants.REPORT_LOG_PATH));
        newIntent.putExtra(Constants.BUGREPORT_INTENT_PARA_ID,
                intent.getStringExtra(Constants.BUGREPORT_INTENT_PARA_ID));
        newIntent.putExtra("reportid", reportID);
        sendBroadcast(newIntent);
        Log.i(tag, "broadcast intent");

    }

    private void onCollectorError(Intent intent) {
        mState = State.idle;
        // cancel the timer once the shell script returns
        mScriptTimer.stop(intent);
        // show message in the notification bar
        String notificationType = intent
                .getStringExtra(Constants.BUGREPORT_INTENT_EXTRA_NOTIFICATION_TYPE);
        if (Constants.BUGREPORT_INTENT_EXTRA_NOTIFICATION_BAR
                .equals(notificationType)) {
            int titleResId = intent.getIntExtra(
                    Constants.BUGREPORT_INTENT_PARA_ERROR_TITLE, 0);
            int msgResId = intent.getIntExtra(
                    Constants.BUGREPORT_INTENT_PARA_ERROR_MSG, 0);
            Notifications
                    .showNotification(this, titleResId, msgResId, msgResId);
        }
        broadcastUpdates(intent);
        // stop service if no more task
        stopSelfIfNoMoreTask();
    }

    private void onCollectorEnd(final Intent intent) {
        // cancel the timer once the shell script returns
        mScriptTimer.stop(intent);
        new Thread() {
            public void run() {
                mNumberOfRunningTasks++;
                updateReport(intent);
                mState = NewReportService.State.idle;
                mNumberOfRunningTasks--;
                // stop service if no more task
                stopSelfIfNoMoreTask();
            }
        }.start();
    }

    private void collectLog(final Intent intent) {
        new Thread() {
            public void run() {
                mNumberOfRunningTasks++;
                collectLogsForCategory(intent);
                mNumberOfRunningTasks--;
                // stop service if no more task
                stopSelfIfNoMoreTask();
            }
        }.start();
    }

    private void discardReport(final Intent intent) {
        mState = NewReportService.State.idle;
        mUserExitIntent = intent;
        new Thread() {
            public void run() {
                try {
                    mNumberOfRunningTasks++;
                    forceStopBugReportbureport(intent);
                } finally {
                    mNumberOfRunningTasks--;
                    // stop service if no more task
                    stopSelfIfNoMoreTask();
                }
            }
        }.start();
    }

    private boolean forceStopBugReportbureport(Intent intent) {
        try {
            String logId = intent
                    .getStringExtra(Constants.BUGREPORT_INTENT_PARA_ID);
            ComplainReport report = mReportsInCollecting.get(logId);
            if (report != null) {
                report.setState(ComplainReport.State.USER_DELETED_DRAFT);
                mTaskMaster.getBugReportDAO().updateReportState(report);
                mReportsInCollecting.remove(logId);
            }
            // stop the service
            Util.setSystemProperty("ctl.stop", Constants.BUGREPORT_SERVICE);
            int times = 0;
            // wait for the state of the service for 5 seconds
            while (times++ < 5) {
                Thread.sleep(1000);
                String state = Util.getSystemProperty("init.svc."
                        + Constants.BUGREPORT_SERVICE, null);
                // remove the log related files if the service is stopped
                if ("stopped".equalsIgnoreCase(state)) {
                    String logPath = intent
                            .getStringExtra(Constants.REPORT_LOG_PATH);
                    if (!TextUtils.isEmpty(logPath)) {
                        String infoFile = logPath.replace(logId, "bugreport_"
                                + logId + ".info");
                        Util.removeFile(infoFile);
                    }
                    mTaskMaster.deleteComplainReport(report);
                    Log.d(tag, "Discarded report : " + logId);
                    return true;
                }
            }
        } catch (InterruptedException e) {
            Log.e(tag, "Failed to discard report", e);
        }
        return false;
    }

    private void createReport(Intent intent) {
        String bugreportId = intent
                .getStringExtra(Constants.BUGREPORT_INTENT_PARA_ID);
        try {
            Properties reportInfo = new Properties();
            reportInfo.put(Constants.TIMESTAMP_LABEL, bugreportId);
            reportInfo.put(Constants.LOG_FILES_LABEL,
                    intent.getStringExtra(Constants.REPORT_LOG_PATH));
            ComplainReport report = mTaskMaster.getLogCollector().createReport(
                    reportInfo);
            if (report != null) {
                report.setType(ComplainReport.Type.USER);
                report.setState(ComplainReport.State.BUILDING);
                reportID = mTaskMaster.getBugReportDAO().saveReport(report);

                increaseEditorNum(bugreportId, report);
            }
        } catch (BugReportException e) {
            Log.e(tag, "Failed to create report for " + bugreportId, e);
        }
    }

    private void updateReport(Intent intent) {
        try {
            String bugreportId = intent
                    .getStringExtra(Constants.BUGREPORT_INTENT_PARA_ID);
            ComplainReport report = mReportsInCollecting.get(bugreportId);
            if (report != null) {
                String reportInfoFile = intent
                        .getStringExtra(Constants.REPORT_INFO_LABEL);
                Properties reportInfo = Util
                        .readPropertiesFromFile(reportInfoFile);

                String screenshotPath = reportInfo
                        .getProperty(Constants.LOG_SCREENSHOT_LABEL);
                if (screenshotPath != null && new File(screenshotPath).exists())
                    report.setScreenshotPath(screenshotPath);

                decreaseEditorNum(bugreportId, report);

                // update report to database
                mTaskMaster.getBugReportDAO().updateReport(report);
                // Remove the temporary log files and report info file for
                // they have been correctly processed and saved to database
                String files = reportInfo
                        .getProperty(Constants.LOG_FILES_REMOVE_LABEL);
                if (!TextUtils.isEmpty(files))
                    Util.removeFiles(files.replaceAll("[ ]+", " ").split(" "));
                // update the main activity with the new report object
                intent.putExtra(Constants.BUGREPORT_INTENT_EXTRA_REPORT, report);
                broadcastUpdates(intent);
            } else {
                throw new BugReportException("Failed to update report for : "
                        + bugreportId);
            }
        } catch (BugReportException e) {
            Log.e(tag, "Error found in the collected result.", e);
            intent.setAction(Constants.BUGREPORT_INTENT_BUGREPORT_ERROR);
            intent.putExtra(Constants.BUGREPORT_INTENT_PARA_ERROR_MSG,
                    "Failed to create report due to " + e.getMessage());
            onCollectorError(intent);
        }
    }

    /**
     * update report info with user input in the ReportEditor activity
     * 
     * @param intent
     */
    private void updateReportWithUserInput(Intent intent) {
        mUserExitIntent = intent;
        String bugreportId = mUserExitIntent
                .getStringExtra(Constants.BUGREPORT_INTENT_PARA_ID);
        ComplainReport report = mReportsInCollecting.get(bugreportId);
        if (report != null) {
            Log.i(tag, "Updating report :" + bugreportId);
            report.setCategory(mUserExitIntent
                    .getStringExtra(Constants.BUGREPORT_INTENT_PARA_CATEGORY));
            report.setSummary(mUserExitIntent
                    .getStringExtra(Constants.BUGREPORT_INTENT_PARA_SUMMARY));
            report.setFreeText(mUserExitIntent
                    .getStringExtra(Constants.BUGREPORT_INTENT_PARA_DETAIL));
            mTaskMaster.getBugReportDAO().updateReport(report);
        }
    }

    private void broadcastUpdates(Intent intent) {
        // check whether user exited the main activity
        if (mUserExitIntent != null) {
            String exitIntentId = mUserExitIntent
                    .getStringExtra(Constants.BUGREPORT_INTENT_PARA_ID);
            String thisIntentId = intent
                    .getStringExtra(Constants.BUGREPORT_INTENT_PARA_ID);
            if (exitIntentId != null && exitIntentId.equals(thisIntentId)) {
                ComplainReport report = intent
                        .getParcelableExtra(Constants.BUGREPORT_INTENT_EXTRA_REPORT);
                if (report != null
                        && Constants.BUGREPORT_INTENT_DISCARD_REPORT
                                .equals(mUserExitIntent.getAction())) {
                    // delete the report if user selects discard
                    Log.i(tag, "Discarding report :" + thisIntentId);
                    mTaskMaster.deleteComplainReport(report);
                }
                mUserExitIntent = null;
            }
        }
        // create an intent for the updates and broadcast it.
        Intent updates = new Intent(intent.getAction());
        updates.putExtras(intent.getExtras());
        sendBroadcast(updates);
    }

    @SuppressWarnings("deprecation")
    private synchronized void collectLogsForCategory(Intent intent) {
        ComplainReport report = (ComplainReport) intent
                .getParcelableExtra(Constants.BUGREPORT_INTENT_EXTRA_REPORT);
        String bugreportId = null;
        String logPath = null;
        String category = null;
        try {
            // report != null, means user changed category for an existing
            // report
            // so we can get the category and logpath from the report object
            if (report != null) {
                logPath = report.getLogPath();
                category = report.getCategory();
                bugreportId = report.getCreateTime().toGMTString();
            } else {
                // user selects a category before the log collection completes
                // we can find the logPath, logId and issue category in the
                // intent
                logPath = intent.getStringExtra(Constants.REPORT_LOG_PATH);
                category = intent.getStringExtra(Constants.REPORT_LOG_CATEGORY);
                bugreportId = intent
                        .getStringExtra(Constants.BUGREPORT_INTENT_PARA_ID);
                report = mReportsInCollecting.get(bugreportId);
            }

            // update the report state to COLLECTING so that it can not be
            // uploaded at this time
            increaseEditorNum(bugreportId, report);

            // remove the logs collected for the previous issue category
            String categoryLogPath = logPath + File.separator + "category_logs";
            Util.removeFile(categoryLogPath);

            // create a new folder in the root log directory
            Util.mkdirs(categoryLogPath);

            // Use DEAM handler to getLogs
            Deam deam = mTaskMaster.getConfigurationManager()
                    .getDeamConfiguration().get();
            if (deam != null) {
                DeamEventHandler deamHandler = new DeamEventHandler();
                UserDeamEvent event = null;
                try {
                    event = new UserDeamEvent(category, report.getCreateTime()
                            .getTime());
                    deamHandler.handle(event, deam, new File(categoryLogPath),
                            false, this);
                } finally {
                    event.cleanup();
                }
            }
        } catch (BugReportException e) {
            Log.e(tag, "Failed collecting logs for category " + category, e);
        } finally {
            // update the report state to NEW so that user can upload it
            decreaseEditorNum(bugreportId, report);
        }
    }

    /**
     * call this method before collecting logs for a report, this method will
     * change the state of the report to COLLECTING to prevent user from sending
     * the report. So, this method works like a state lock must call
     * decreaseEditorNum to release the state lock.
     * 
     * @param bugreportId
     * @param report
     */
    private void increaseEditorNum(String bugreportId, ComplainReport report) {
        Log.v(tag, "increaseEditorNum() " + bugreportId);
        if (bugreportId != null) {
            Integer numberOfEditors = mReportEditorCounter.get(bugreportId);
            if (numberOfEditors == null) {
                mReportEditorCounter.put(bugreportId, 1);
            } else {
                mReportEditorCounter.put(bugreportId, numberOfEditors + 1);
            }
            if (report != null) {
                mReportsInCollecting.put(bugreportId, report);
                // update the report state to COLLECTING so it can not be
                // uploaded at this time
                report.setState(ComplainReport.State.BUILDING);
                mTaskMaster.getBugReportDAO().updateReportState(report);
            }
        }
    }

    /**
     * call this method after collecting logs for a report, this method will
     * change the state of the report to NEW so that user can send the report.
     */
    private void decreaseEditorNum(String bugreportId, ComplainReport report) {
        Log.v(tag, "decreaseEditorNum() " + bugreportId);
        if (bugreportId != null) {
            Integer numberOfEditors = mReportEditorCounter.get(bugreportId);
            if (numberOfEditors != null) {
                if (numberOfEditors == 1) {// if no others is editing this
                                           // report, set the state to NEW
                    mReportEditorCounter.remove(bugreportId);
                    mReportsInCollecting.remove(bugreportId);
                    // update the report state to new so it is ready to upload
                    if (report != null) {
                        report.setState(ComplainReport.State.WAIT_USER_INPUT);
                        mTaskMaster.getBugReportDAO().updateReportState(report);
                    }
                } else {
                    mReportEditorCounter.put(bugreportId, numberOfEditors - 1);
                }
            }
        }
    }

    public void onDestroy() {
        Log.i(tag, "onDestroy()");
        mBugS = null;
        super.onDestroy();
    }

    private void stopSelfIfNoMoreTask() {
        if (mNumberOfRunningTasks == 0 && mState == State.idle)
            stopSelf();
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    public State getState() {
        return mState;
    }

    public static enum State {
        idle, collecting
    }

    /**
     * A timer that stops waiting for the bugtogo.sh if it timeouts
     */
    class BugreportTimer {
        private Timer mTimer = new Timer();
        private Map<String, TimerTask> mTimerTasks = new Hashtable<String, TimerTask>();

        public void start(final Intent intent) {
            // start the timer and save to a local map
            TimerTask task = new TimerTask() {
                public void run() {

                    try {
                        Thread.sleep(Constants.COLLECTOR_TIMEOUT);
                        // The Shell script log collector timeouts
                        if (mState.equals(NewReportService.State.collecting)) {
                            Log.i(tag, "bugreport timed out");
                            intent.putExtra(
                                    Constants.BUGREPORT_INTENT_PARA_ERROR_TITLE,
                                    R.string.notification_bugreport_failed);
                            intent.putExtra(
                                    Constants.BUGREPORT_INTENT_PARA_ERROR_MSG,
                                    R.string.notification_bugreport_timeout);
                            intent.setAction(Constants.BUGREPORT_INTENT_BUGREPORT_ERROR);
                            intent.putExtra(
                                    Constants.BUGREPORT_INTENT_EXTRA_NOTIFICATION_TYPE,
                                    Constants.BUGREPORT_INTENT_EXTRA_NOTIFICATION_BAR);
                            // force stop the bugreport service and remove
                            // everything relevant
                            forceStopBugReportbureport(intent);
                            onCollectorError(intent);
                        }
                    } catch (InterruptedException e) {
                        Thread.interrupted();
                    }
                }
            };
            String id = intent
                    .getStringExtra(Constants.BUGREPORT_INTENT_PARA_ID);
            if (!TextUtils.isEmpty(id))
                mTimerTasks.put(id, task);
            mTimer.schedule(task, 0);
        }

        public void stop(final Intent intent) {
            String id = intent
                    .getStringExtra(Constants.BUGREPORT_INTENT_PARA_ID);
            if (!TextUtils.isEmpty(id)) {
                TimerTask task = mTimerTasks.get(id);
                if (task != null) {
                    task.cancel();
                    mTimerTasks.remove(id);
                }
            }
        }
    }
}
