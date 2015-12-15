
package com.tronxyz.bug_report.log;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.DropBoxManager;
import android.os.IBinder;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import com.tronxyz.bug_report.BugReportApplication;
import com.tronxyz.bug_report.BugReportException;
import com.tronxyz.bug_report.Constants;
import com.tronxyz.bug_report.TaskMaster;
import com.tronxyz.bug_report.conf.bean.Deam;
import com.tronxyz.bug_report.helper.Util;
import com.tronxyz.bug_report.log.DeamEventHandler.Results;
import com.tronxyz.bug_report.model.ComplainReport;
import com.tronxyz.bug_report.model.ComplainReport.State;
import com.tronxyz.bug_report.model.ComplainReport.Type;
import com.tronxyz.bug_report.model.DropBoxDeamEvent;
import com.tronxyz.bug_report.model.UserSettings;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DropBoxEventHandler extends Service {

    static String TAG = "BugReportDropBoxEventHandler";
    private Deam mDeam;
    private DropBoxManager mDbm;
    private TaskMaster mTaskMaster;
    private LastEventUpdater mLastEventUpdater;
    private static final long MAX_BACKWARDS_EVENT_TIME = 5 * 60 * 1000;
    // Maximum threads allowed
    private static final int THREAD_POOL_SIZE = 3;
    // A thread pool executor
    private ExecutorService mExecutor;
    // counts the number of worker thread running
    private int mNumWorkers;
    // Dropbox Event validator
    private DropBoxEventValidators mValidator;

    public void onCreate() {
        mLastEventUpdater = LastEventUpdater
                .getInstance(getApplicationContext());
        mTaskMaster = ((BugReportApplication) getApplicationContext())
                .getTaskMaster();
        mDbm = (DropBoxManager) getSystemService(DROPBOX_SERVICE);
        mExecutor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        mValidator = new DropBoxEventValidators(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        UserSettings userSetting = mTaskMaster.getConfigurationManager()
                .getUserSettings();
        if (userSetting != null && userSetting.isAutoReportEnabled().getValue()) {
            // Always use the latest DEAM configuration
            mDeam = mTaskMaster.getConfigurationManager()
                    .getDeamConfiguration().get();

            String tagName = intent.getStringExtra(DropBoxManager.EXTRA_TAG);
            long time = intent.getLongExtra(DropBoxManager.EXTRA_TIME,
                    System.currentTimeMillis());
            // handle those events that BugReport might have no enough time
            // to process before the last reset of Android Runtime
            handleUnprocessedEvent(time);
            // handle the current event
            handleDropBoxEvent(tagName, time);
        }
        stopSelfIfNoMoreTask();
        return START_NOT_STICKY;
    }

    private void handleUnprocessedEvent(long newEventTime) {
        long lastProccessedEventTime = mLastEventUpdater
                .getLastProcessedEventTime();
        if (lastProccessedEventTime != 0) {

            long currentTime = System.currentTimeMillis();
            // just need to look for events occurred within the most recent
            // MAX_BACKWARDS_TIME
            // if the last processed time is too long
            if (currentTime - MAX_BACKWARDS_EVENT_TIME > lastProccessedEventTime) {
                lastProccessedEventTime = currentTime
                        - MAX_BACKWARDS_EVENT_TIME;
            }

            DropBoxManager.Entry entry = mDbm.getNextEntry(null,
                    lastProccessedEventTime);
            // we need to handle all events that happened between the last
            // processed event and the new event
            while (entry != null && entry.getTimeMillis() < newEventTime) {
                handleDropBoxEvent(entry.getTag(), entry.getTimeMillis());
                entry.close();
                entry = mDbm.getNextEntry(null, entry.getTimeMillis());
            }
            if (entry != null)
                entry.close();
        }
    }

    private void handleDropBoxEvent(String tag, long timeMillis) {
        if (mLastEventUpdater.eventIsProcessed(timeMillis)) {
            Log.w(TAG, "The entry has already been processed : " + tag + ", "
                    + timeMillis);
            return;
        }
        // update the latest processed event time here so that other threads
        // can always
        // get the most recent processed event even if the event is still
        // being processed.
        mLastEventUpdater.processEvent(timeMillis);

        if (null == mDeam)
            return;

        if (!mDeam.hasTag(tag, Deam.Tag.Type.DROPBOX))
            return;

        if (!mValidator.isValid(mDeam, tag, timeMillis))
            return;
        // process the entry in a worker thread
        handleEventAsyncly(tag, timeMillis);

    }

    private void handleEventAsyncly(final String tag, final long timeMillis) {
        mNumWorkers++;
        new AsyncTask<Void, Void, Void>() {
            public Void doInBackground(Void... params) {
                DropBoxDeamEvent event = null;
                try {
                    event = new DropBoxDeamEvent(
                            (DropBoxManager) getSystemService(DROPBOX_SERVICE),
                            tag, timeMillis);
                    DeamEventHandler handler = new DeamEventHandler();
                    boolean notificationEnabled = mTaskMaster
                            .getConfigurationManager().getUserSettings()
                            .getDeamNotify().getValue().booleanValue();
                    DeamEventHandler.Results results;
                    results = handler.handle(event,
                            mDeam, null, notificationEnabled,
                            DropBoxEventHandler.this);
                    if (results.isHandled)
                        try {
                            uploadReport(event, results);
                        } catch (BugReportException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                } catch (IOException e) {
                    Log.e(TAG, "Error processing " + tag + "@" + timeMillis, e);
                } catch (BugReportException e) {
                    Log.e(TAG, "Error processing " + tag + "@" + timeMillis, e);
                } finally {
                    if (event != null)
                        event.cleanup();
                }
                return null;
            }

            protected void onPostExecute(Void result) {
                mNumWorkers--;
                stopSelfIfNoMoreTask();
            }
        }.executeOnExecutor(mExecutor, new Void[0]);
    }

    private void uploadReport(DropBoxDeamEvent event, Results result)
            throws BugReportException {
        cleanReport();
        LocationService.getInstance(mTaskMaster).saveLocationInfo(
                result.logDir + File.separator + "device_location.txt");
        ComplainReport report = new ComplainReport();
        report.setCreateTime(new Date(event.getTimeMillis()));
        report.setState(State.WAIT_USER_INPUT);
        report.setCategory(result.scenarioName);
        report.setType(Type.AUTO);
        report.setLogPath(result.logDir);
        report.setSummary(event.getTag() + ":" + result.processName);
        report.setShowNotification(result.showNotification ? 1 : 0);
        if (TextUtils.isEmpty(result.description))
            report.setFreeText(Long.toString(event.getTimeMillis()));
        else
            report.setFreeText(result.description);
        report.setApVersion(Util.getSystemProperty(
                Constants.DEVICE_PROPERTY_APVERSION, ""));
        report.setBpVersion(Util.getSystemProperty(
                Constants.DEVICE_PROPERTY_BPVERSION, ""));
        report.setApkVersion(Util.getAppVersionName(this));
        mTaskMaster.getBugReportDAO().saveReport(report);
        // post to upload queue immediately
        Message msg = Message.obtain();
        msg.what = TaskMaster.TRONXYZ_BUG_REPORT_SEND_LOG;
        msg.obj = report;
        mTaskMaster.sendMessage(msg);
    }

    private void cleanReport() {
        List<ComplainReport> mReports = mTaskMaster.getAllReports();
        List<ComplainReport> delList = new ArrayList<ComplainReport>();
        for (ComplainReport report : mReports) {
            /*
             * if (report.getState().equals(ComplainReport.State.BUILDING) ||
             * report.getState().equals( ComplainReport.State.WAIT_USER_INPUT))
             * { delList.add(report); mTaskMaster.deleteComplainReport(report);
             * continue; }
             */
            long createTime = report.getCreateTime().getTime();
            Calendar c = Calendar.getInstance();
            c.add(Calendar.DAY_OF_MONTH, -3);
            long timeMillis = c.getTimeInMillis();
            if (timeMillis >= createTime) {
                delList.add(report);
                mTaskMaster.deleteComplainReport(report);
            }
        }
        mReports.removeAll(delList);
    }

    public static class LastEventUpdater {
        private static long lastProcessedEventTime;
        // Don't have a good idea on when to remove the events stored in
        // processedEvents,
        // So just keep 100 events for now.
        private static final int MAX_EVENTS_KEPT = 100;
        // Use this to store the events that are being processed in the current
        // Java process.
        // In order to avoid duplicate of reports, if multiple instances of this
        // service
        // are created, they should do nothing for the events stored in this
        // vector.
        private static Vector<Long> processedEvents = new Vector<Long>();
        private static LastEventUpdater instance;
        private static final String LAST_EVENT_KEY = "last_event";
        private static final String PROCESSED_EVENTS = "processed_events";
        private String mLastEventFile;

        public static synchronized LastEventUpdater getInstance(Context context) {
            if (instance == null) {
                instance = new LastEventUpdater(context);
            }
            return instance;
        }

        private LastEventUpdater(Context context) {
            mLastEventFile = context.getFilesDir() + File.separator
                    + "lastEvent.properties";

            lastProcessedEventTime = 0;
            String time = Util.readPropertiesFromFile(mLastEventFile)
                    .getProperty(LAST_EVENT_KEY, "0");
            try {
                lastProcessedEventTime = Long.parseLong(time);
            } catch (NumberFormatException e) {
            }
            String eventTimes = Util.readPropertiesFromFile(mLastEventFile)
                    .getProperty(PROCESSED_EVENTS, "");
            JSONArray events = null;
            try {
                events = new JSONArray(eventTimes);
            } catch (JSONException e) {
            }
            if (events != null) {
                int length = events.length();
                int start = length - MAX_EVENTS_KEPT;
                if (start <= 0) {
                    start = 0;
                } else {
                    length = MAX_EVENTS_KEPT;
                }
                for (int i = start; i < length; i++) {
                    try {
                        processedEvents.add(events.getLong(i));
                    } catch (JSONException e) {
                    }
                }
            }
        }

        public void processEvent(long time) {
            synchronized (this) { // lock the object while writing info to file
                lastProcessedEventTime = time;
                processedEvents.add(Long.valueOf(time));
                while (processedEvents.size() > MAX_EVENTS_KEPT)
                    processedEvents.remove(0);
                // update the time to file
                Properties properties = new Properties();
                JSONArray events = new JSONArray(processedEvents);
                properties.setProperty(PROCESSED_EVENTS, events.toString());
                properties.setProperty(LAST_EVENT_KEY, String.valueOf(time));
                Util.savePropertiesToFile(properties, mLastEventFile);
            }
        }

        public long getLastProcessedEventTime() {
            return lastProcessedEventTime;
        }

        public void setLastEvent(long time) {
            synchronized (this) { // lock the object while writing info to file
                // update the time to file
                Properties properties = new Properties();
                JSONArray events = new JSONArray(processedEvents);
                properties.setProperty(PROCESSED_EVENTS, events.toString());
                properties.setProperty(LAST_EVENT_KEY, String.valueOf(time));
                Util.savePropertiesToFile(properties, mLastEventFile);
            }
        }

        public boolean eventIsProcessed(long time) {
            return processedEvents.contains(time);
        }
    }

    public void stopSelfIfNoMoreTask() {
        if (mNumWorkers == 0) {
            Log.d(TAG, "No more task to run, stopSelf");
            stopSelf();
        }
    }

    @Override
    public void onDestroy() {
        mExecutor.shutdown();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
