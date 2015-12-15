package com.qiku.bug_report.upload;

import static com.qiku.bug_report.upload.UploadWorker.showNotification;

import java.io.File;
import java.util.List;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.qiku.bug_report.BugReportApplication;
import com.qiku.bug_report.Constants;
import com.qiku.bug_report.R;
import com.qiku.bug_report.TaskMaster;
import com.qiku.bug_report.helper.Notifications;
import com.qiku.bug_report.helper.Util;
import com.qiku.bug_report.http.upload.GUS.ReturnCode;
import com.qiku.bug_report.model.ComplainReport;
import com.qiku.bug_report.model.ComplainReport.State;
import com.qiku.bug_report.upload.UploadWorker.Result;

public class ReliableUploader extends Service{
    public static final String tag = "BugReportReliableUploader";
    private static final int MAX_CONTINUOUS_FAILURE = 2;
    private Result mLastUploadResult = Result.SUCCESSFUL;
    private int mContinuousFailuresCount = 0;
    private TaskMaster mTaskMaster;
    private UploaderReceiver mUploaderReceiver = null;
    private UploadWorker mUploadWorker;
    private boolean mStopped = false;

    public void onCreate() {
        Log.i(tag, "onCreate()");
        super.onCreate();
        mTaskMaster = ((BugReportApplication) getApplicationContext()).getTaskMaster();

        mUploadWorker = new UploadWorker(this);

        mUploaderReceiver = new UploaderReceiver(this);
        mUploaderReceiver.start();
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(tag, "onStartCommand()");
        //The intent may be null if this service is re-started after it is killed due to issues
        if(intent!=null){
            ComplainReport report = (ComplainReport) intent.getParcelableExtra(Constants.BUGREPORT_INTENT_EXTRA_REPORT);
            if(isValidReport(report)){
                //Add the report to upload queue
                addReportToUploadQueue(report);
            }
        }
        //trigger the upload worker to start next upload
        startNextUpload();
        //The service should be recreated if it is killed by the server due to issues
        return START_STICKY;
    }

    private boolean isValidReport(ComplainReport report){
        if(report == null){
            return false;
        }
        File file = new File(report.getLogPath());
        if (!file.exists()
                || (file.isDirectory() && file.list().length == 0) //empty directory
                || (file.isFile() && file.length() == 0) //empty file
                ) {
            Log.i(tag, String.format("Invalid log file: %s",  report.getLogPath()));
            if(showNotification(mTaskMaster, report))
                Notifications.showUploadErrorNotification(this,
                        this.getString(R.string.bug_prompt) + report.getTitle(),
                        R.string.upload_msg_invalid_log, (int)report.getId());
            return false;
        }
        return true;
    }

    private boolean isUploadAllowed(ComplainReport report){
        Boolean isMobileUploadAllowed = !mTaskMaster.getConfigurationManager().getUserSettings().getWlanUploadAllowed().getValue();
        Log.d(tag, "isNetworkAvailable: " + Util.isNetworkAvailable(this)
                + " isWIFIConnected: " + Util.isWIFIConnected(this)
                + " is2GConnected: " + Util.is2GConnected(this)
                + " is3GConnected: " + Util.is3GConnected(this)
                + " getMobileUploadAllowed: " + isMobileUploadAllowed
                );

        if (!Util.isNetworkAvailable(this)) {
            Log.i(tag, "Network unavailable. upload is pending : " + report);
            if(showNotification(mTaskMaster, report))
                Notifications.showUploadErrorNotification(this,
                        this.getString(R.string.bug_prompt) + report.getTitle(),
                        R.string.upload_pending, (int)report.getId());
            return false;
        }

        if(Util.isWIFIConnected(this)){
            Log.i(tag, "WIFI is available, don't need to check 3G/2G configuration");
        } else {
            if (!isMobileUploadAllowed &&
                    (Util.is3GConnected(this) || Util.is2GConnected(this)) ) {
                Log.i(tag, "3G/2G is connected but upload is not allowed. upload cancelled");
                if(showNotification(mTaskMaster, report))
                    Notifications.showUploadErrorNotification(this,
                            this.getString(R.string.bug_prompt) + report.getTitle(),
                            R.string.upload_not_allowed, (int)report.getId());
                return false;
            }
        }

        int userBatteryThreshold  = mTaskMaster.getConfigurationManager()
            .getUserSettings().getBatteryPercent().getValue();
        if(userBatteryThreshold > mUploaderReceiver.getCurrentBatteryLevel() && !mUploaderReceiver.isPowerConnected())
        {
            Log.i(tag, "Uploads suspended for low battery; level=" + mUploaderReceiver.getCurrentBatteryLevel() + ", threshold =" + userBatteryThreshold);
            if(showNotification(mTaskMaster, report))
                Notifications.showUploadErrorNotification(this,
                        this.getString(R.string.bug_prompt) + report.getTitle(),
                        R.string.notification_message_battery_suspended, (int)report.getId());
            return false;
        }
        return true;
    }

    private void addReportToUploadQueue(ComplainReport task){
        Log.v(tag, "addUploadTask : " + task);
        //update report status to database
        task.setState(State.READY_TO_UPLOAD);
        task.setUploadPaused(
                mTaskMaster.getConfigurationManager()
                .getUserSettings().isAutoUploadEnabled().getValue() ? 0 : 1);
        //set priority if it is not set yet
        if(task.getPriority() == 0){
            List<ComplainReport> pendingUploads = mTaskMaster.getBugReportDAO().getReportsByState(
                    State.READY_TO_UPLOAD, State.READY_TO_COMPRESS, State.COMPRESSING,
                    State.READY_TO_TRANSMIT,State.TRANSMITTING, State.READY_TO_COMPLETE,
                    State.COMPLETING);
            ComplainReport lastTask = pendingUploads.isEmpty() ?
                    null : pendingUploads.get(pendingUploads.size() -1 );
            task.setPriority(lastTask != null ? lastTask.getPriority() + 1 : 1);
            mTaskMaster.getBugReportDAO().updateReportUploadInfo(task, true);
        }else{
            mTaskMaster.getBugReportDAO().updateReportUploadInfo(task, false);
        }
    }

    private synchronized void startNextUpload(){
        Log.d(tag, "startNextUpload");
        if(mUploadWorker.isBusy()){
            Log.d(tag, "The uploader worker is busy, please wait ... ");
            return;
        }
        if(mStopped){
            stopSelf();
            return;
        }

        if(!mTaskMaster.getConfigurationManager().getUserSettings()
                        .isAutoUploadEnabled().getValue()){
            stopSelf();
            return;
        }

        List<ComplainReport> pendingUploads = mTaskMaster.getBugReportDAO().getReportsByState(false,
                State.READY_TO_UPLOAD, State.READY_TO_COMPRESS, State.COMPRESSING,
                State.READY_TO_TRANSMIT,State.TRANSMITTING, State.READY_TO_COMPLETE,
                State.COMPLETING);
        if(pendingUploads.isEmpty()){
            stopSelf();
            return;
        }

        ComplainReport report = pendingUploads.get(0);
        if(isUploadAllowed(report)){
            mUploadWorker.startUpload(pendingUploads.get(0));
        }else{
            stopSelf();
        }
    }

    protected void onUploadFinished(ComplainReport task, Result result){
        Log.v(tag, "onUploadFinished : " + task);
        //increase the failures count if both last upload and current upload fail
        if(Result.FAILED == result && mLastUploadResult == Result.FAILED){
            mContinuousFailuresCount ++;
            //stop uploader if number of continuous failures exceeds the MAX_CONTINUOUS_FAILURE
            if(mContinuousFailuresCount > MAX_CONTINUOUS_FAILURE){
                mStopped = true;
            }
        }else{//otherwise reset the failure count
            mContinuousFailuresCount = 0;
        }
        mLastUploadResult = result;
        startNextUpload();
    }

    public synchronized void cancel(ComplainReport report, ReturnCode reason){
        Log.d(tag, "cancel : " + report + ", code : " + reason.name());
        if(report.equals(mUploadWorker.getCurrentUpload())){//if the report is current being uploaded
            mUploadWorker.cancel(reason);
        }else{//the report is current pending in the upload queue or the compression queue
            //remove the report from either of the two queues
            if(ReturnCode.UPLOAD_DELETED == reason){
                report.setState(State.USER_DELETED_OUTBOX);
                //remove notification
                Notifications.cancel(this, (int)report.getId());
            }else{
                if(showNotification(mTaskMaster, report))
                    Notifications.showUploadErrorNotification(this,
                            this.getString(R.string.bug_prompt) + report.getTitle(),
                            R.string.upload_msg_failed, (int)report.getId());
                report.setState(State.READY_TO_UPLOAD);
            }
            mTaskMaster.getBugReportDAO().updateReportUploadInfo(report, false);
        }
    }

    public synchronized void cancelAll(ReturnCode reason) {
        mStopped = true;
        mUploadWorker.cancel(reason);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void onDestroy() {
        mUploaderReceiver.stop();
        Log.d(tag, "Reliable Upload onDestroy().");
    }

    public synchronized ComplainReport getCurrentUpload(){
        return mUploadWorker.getCurrentUpload();
    }
}
