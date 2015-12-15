
package com.qiku.bug_report.upload;

import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.util.Log;

import com.qiku.bug_report.BugReportApplication;
import com.qiku.bug_report.Constants;
import com.qiku.bug_report.R;
import com.qiku.bug_report.TaskMaster;
import com.qiku.bug_report.helper.DeviceID;
import com.qiku.bug_report.helper.JSONHelper;
import com.qiku.bug_report.helper.Notifications;
import com.qiku.bug_report.helper.Util;
import com.qiku.bug_report.http.upload.GUS;
import com.qiku.bug_report.http.upload.GusData;
import com.qiku.bug_report.http.upload.GusJob;
import com.qiku.bug_report.http.upload.IGusCallback;
import com.qiku.bug_report.http.upload.GUS.ReturnCode;
import com.qiku.bug_report.model.ComplainReport;
import com.qiku.bug_report.model.UserSettings;
import com.qiku.bug_report.model.ComplainReport.State;
import com.qiku.bug_report.model.ComplainReport.Type;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * This is the actual report uploader that will upload file to the BugReport
 * serve. It can upload only one file at a time.
 * 
 * @author gkp374
 */
public class UploadWorker implements IGusCallback {
    private static final String tag = "BugReportUploadWorker";
    private static final int PROGRESS_UPLOAD_SIZE = 20480;
    private static final int MAX_DIR_NEST_LEVEL = 20;
    private ReliableUploader mUploader;
    private GUS mGus;
    private TaskMaster mTaskMaster;
    private PowerManager.WakeLock mWakeLock;
    private ComplainReport mCurrentReport;
    private long mCurrentJobId;
    public static String createDate;
    private JSONObject reportInfo;

    enum Result {
        INVALID, CANCELLED, FAILED, SUCCESSFUL;
    }

    enum CompressSubstate {
        PREPARE, COMPRESS, CHANGE_STATE, REMOVE_FILES, CHANGE_LOGPATH, START_TO_UPLOAD
    }

    public UploadWorker(ReliableUploader uploader) {
        mUploader = uploader;
        mGus = new GUS(uploader, Constants.BUGREPORT_URL_GETUPLOADID,
                Constants.BUGREPORT_URL_UPLOAD, Constants.BUGREPORT_URL_RESUME,
                Constants.BUGREPORT_URL_LOGON, Constants.BUGREPORT_URL_LOGOFF,
                Constants.BUGREPORT_URL_FILELIST,
                Constants.BUGREPORT_URL_FILEUPLOAD,
                Constants.BUGREPORT_URL_FOLDERCREATE);
        mWakeLock = ((PowerManager) uploader
                .getSystemService(Context.POWER_SERVICE)).newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK, tag);
        mTaskMaster = ((BugReportApplication) uploader.getApplicationContext())
                .getTaskMaster();
    }

    public void startUpload(ComplainReport task) {
        if (isBusy()) {
            throw new IllegalStateException(
                    "UploadWorker can only upload one at a time.");
        }
        if (task == null)
            throw new IllegalArgumentException("Upload can not be null.");
        Log.i(tag, "startUpload " + task);
        mCurrentReport = task;
        mTaskMaster.getBugReportDAO().updateReportUploadInfo(mCurrentReport,
                false);
        if (!mWakeLock.isHeld()) {
            mWakeLock.acquire();
            Log.d(tag, "Acquired wakelock");
        }
        new Thread() {
            public void run() {
                compressLogs();
            }
        }.start();
        // show notification : waiting for uploading in the queue
        // if (showNotification(mTaskMaster, mCurrentReport))
        // Notifications.showOnGoingNotification(
        // mUploader,
        // mUploader.getString(R.string.bug_prompt)
        // + mCurrentReport.getTitle(),
        // mUploader.getString(R.string.queued_for_upload),
        // (int) task.getId());
    }

    private void compressLogs() {
        Log.d(tag, "compressLogs : " + mCurrentReport);
        // compress in following steps:
        // 1. change state to COMPRESSING
        // 2. compress the folder into zip file
        // 3. change state to READY_TO_TRANSMIT
        // 4. remove the folder
        // 5. change the logpath to the zip file
        // 6. start the GUS job
        try {
            CompressSubstate subState = CompressSubstate.PREPARE;
            boolean ifCancelled = false;

            File file = new File(mCurrentReport.getLogPath());
            if (!file.exists()) {
                // the process is interrupted at step 5
                subState = CompressSubstate.CHANGE_LOGPATH;
            } else if (file.isFile()) {
                subState = CompressSubstate.START_TO_UPLOAD;
            } else if (mCurrentReport.getState() == ComplainReport.State.READY_TO_TRANSMIT) {
                // the process is interrupted at step 4
                subState = CompressSubstate.REMOVE_FILES;
            }
            // construct the zip file name
            String logPath = getZipLogPath();

            switch (subState) {
                case PREPARE:
                    mCurrentReport.setState(ComplainReport.State.COMPRESSING);
                    mTaskMaster.getBugReportDAO().updateReportUploadInfo(
                            mCurrentReport, false);

                case COMPRESS:
                    Log.i(tag,
                            "Compressing directory : "
                                    + mCurrentReport.getLogPath());
                    // show compressing notification
                    // if (showNotification(mTaskMaster, mCurrentReport))
                    // Notifications.showOnGoingNotification(mUploader,
                    // mUploader.getString(R.string.bug_prompt)
                    // + mCurrentReport.getTitle(),
                    // mUploader.getString(R.string.compressing_report),
                    // (int) mCurrentReport.getId());

                    // add the report information to a file before compressing

                    reportInfo = JSONHelper.toCreateReportRequest(
                            mUploader, mCurrentReport);
                    Util.saveDataToFile(reportInfo.toString(4).getBytes(),
                            mCurrentReport.getLogPath() + File.separator
                                    + "report_info.txt");
                    // compress the log files into a single zip
                    compressFolderToZip(file.getAbsolutePath(), logPath);

                case CHANGE_STATE:
                    // it's status may be updated during the compression because
                    // the
                    // upload may be canceled for some reason.
                    synchronized (this) {
                        ComplainReport.State state = mCurrentReport.getState();
                        if (state == ComplainReport.State.USER_DELETED_OUTBOX) {// it
                                                                                // is
                                                                                // canceled
                                                                                // by
                                                                                // user
                            Util.removeFile(logPath);
                            if (showNotification(mTaskMaster, mCurrentReport)) {
                                Notifications.showUploadErrorNotification(
                                        mUploader,
                                        mUploader.getString(R.string.bug_prompt)
                                                + mCurrentReport.getTitle(),
                                        R.string.upload_msg_canceled,
                                        (int) mCurrentReport.getId());
                            }
                            ifCancelled = true;
                        } else {
                            mCurrentReport
                                    .setState(ComplainReport.State.READY_TO_TRANSMIT);
                            mTaskMaster.getBugReportDAO().updateReportUploadInfo(
                                    mCurrentReport, false);
                            if (state != ComplainReport.State.COMPRESSING) {
                                // canceled by the system, such as battery is
                                // getting low or network disconnects
                                if (showNotification(mTaskMaster, mCurrentReport)) {
                                    Notifications.showUploadErrorNotification(
                                            mUploader,
                                            mUploader
                                                    .getString(R.string.bug_prompt)
                                                    + mCurrentReport.getTitle(),
                                            R.string.upload_msg_failed,
                                            (int) mCurrentReport.getId());
                                }
                                ifCancelled = true;
                            }
                        }
                    }

                case REMOVE_FILES:
                    Util.removeFile(file.getAbsolutePath());

                case CHANGE_LOGPATH:
                    mCurrentReport.setLogPath(logPath);
                    mTaskMaster.getBugReportDAO().updateReportUploadInfo(
                            mCurrentReport, false);

                case START_TO_UPLOAD:
                    if (ifCancelled) {
                        onUploadFinished(Result.CANCELLED);
                    } else {
                        startUploadJob();
                    }
            }
        } catch (JSONException e) {
            Log.e(tag, "Failed to compress " + mCurrentReport, e);
            failwithException();
        } catch (IOException e) {
            Log.e(tag, "Failed to compress " + mCurrentReport, e);
            failwithException();
        }
    }

    private void failwithException() {
        // show failed notification
        if (showNotification(mTaskMaster, mCurrentReport))
            Notifications.showUploadErrorNotification(mUploader,
                    mUploader.getString(R.string.bug_prompt)
                            + mCurrentReport.getTitle(),
                    R.string.upload_msg_conpression_failed,
                    (int) mCurrentReport.getId());
        mCurrentReport.setState(State.COMPRESS_FAILED);
        mCurrentReport.setPriority(0);
        mTaskMaster.getBugReportDAO().updateReportUploadInfo(
                mCurrentReport, true);
        onUploadFinished(Result.FAILED);
    }

    private String getZipLogPath() {
        // createDate = Util.formatDate("yyyy-MM-dd_HH-mm-ss.SSSZ",
        //         mCurrentReport.getCreateTime());
        createDate = Util.formatDate("yyyy-MM-dd_HH-mm-ss", // time zone info is not useful for tronxyz.
                mCurrentReport.getCreateTime());
        File parentFile = new File(mCurrentReport.getLogPath()).getParentFile();
        String parentPath = parentFile == null ? File.separator : parentFile
                .getAbsolutePath() + File.separator;
        String apVersion_full = mCurrentReport.getApVersion();
        String[] apVersion = apVersion_full.split(" ");
        String versionText;
        if (apVersion.length >= 3) {
            versionText = apVersion[2];
        } else {
            versionText = apVersion[0];
        }

        String logPath;
        // if (Type.AUTO.equals(mCurrentReport.getType())) {
        //     logPath = String.format("%s%s-%s-%s@%s.zip", parentPath,
        //             versionText, mCurrentReport.getSummary(), DeviceID
        //                     .getInstance().getId(mUploader), createDate);
        // } else {
        //     logPath = String.format("%s%s-%s-%s@%s.zip", parentPath,
        //             versionText, mCurrentReport.getSummary(), DeviceID
        //                     .getInstance().getId(mUploader), createDate);
        // }
        // remove versionText from file name, otherwise, file name too long to server, more than 100 chars.
        if (Type.AUTO.equals(mCurrentReport.getType())) {
            logPath = String.format("%s%s-%s@%s.zip", parentPath,
                mCurrentReport.getSummary().split(":", 2)[0], DeviceID
                .getInstance().getId(mUploader), createDate);
        } else {
            logPath = String.format("%s%s-%s@%s.zip", parentPath,
                mCurrentReport.getSummary().split(":", 2)[0], DeviceID
                .getInstance().getId(mUploader), createDate);
        }
        return logPath;
    }

    private void compressFolderToZip(String srcPath, String destPath)
            throws FileNotFoundException, IOException {
        byte inBuf[] = new byte[8192];
        ZipOutputStream out = null;
        try {
            out = new ZipOutputStream(new BufferedOutputStream(
                    new FileOutputStream(destPath)));
            addFileToZip(new File(srcPath), out, 0, inBuf);
        } finally {
            try {
                if (out != null)
                    out.close();
            } catch (IOException e) {
                // Nothing we can do here
            }
        }
    }

    private void addFileToZip(File file, ZipOutputStream out, int nestLevel,
            byte[] inBuf) throws FileNotFoundException, IOException {
        if (file.isDirectory()) {
            // Recursively add files, but guard against infinite recursion.
            if (nestLevel >= MAX_DIR_NEST_LEVEL) {
                Log.e(tag, "Max nest level of " + MAX_DIR_NEST_LEVEL
                        + " reached at " + file.getAbsolutePath()
                        + "; aborting branch");
                return;
            }
            final File[] childFiles = file.listFiles();
            if (childFiles != null) {
                for (File childFile : childFiles) {
                    addFileToZip(childFile, out, nestLevel + 1, inBuf);
                }
            }
        } else {
            BufferedInputStream in = null;
            try {
                in = new BufferedInputStream(new FileInputStream(file),
                        inBuf.length);
                // TODO Preserve relative paths of files in the zip
                ZipEntry entry = new ZipEntry(file.getName());
                if (isCompressed(file)) {
                    // Don't bother compressing files that are already
                    // compressed.
                    out.setLevel(Deflater.NO_COMPRESSION);
                } else {
                    // We are typically compressing text, which responds well to
                    // default
                    // compression. Don't waste resources attempting max
                    // compression.
                    out.setLevel(Deflater.DEFAULT_COMPRESSION);
                }
                out.putNextEntry(entry);
                int count;
                while ((count = in.read(inBuf, 0, inBuf.length)) != -1) {
                    out.write(inBuf, 0, count);
                }
                out.closeEntry();
            } finally {
                try {
                    if (in != null)
                        in.close();
                } catch (IOException e) {
                    // Nothing we can do here
                }
            }
        }
    }

    private boolean isCompressed(File file) {
        final String name = file.getName();
        final String ext = name.substring(name.lastIndexOf('.') + 1,
                name.length());
        final String[] zipExts = {
                "gz", "zip", "rar", "7z", "tgz", "png"
        };
        for (String zipExt : zipExts) {
            if (zipExt.equals(ext))
                return true;
        }
        return false;
    }

    private void startUploadJob() {
        Log.d(tag, "startUploadJob : " + mCurrentReport);

        File file = new File(mCurrentReport.getLogPath());
        if (!file.exists() || file.length() == 0) {
            Log.i(tag, "Empty zip file: " + mCurrentReport.getLogPath());
            if (showNotification(mTaskMaster, mCurrentReport))
                Notifications.showUploadErrorNotification(mUploader,
                        mUploader.getString(R.string.bug_prompt)
                                + mCurrentReport.getTitle(),
                        R.string.upload_msg_invalid_log,
                        (int) mCurrentReport.getId());
            // Move back to drafts list, user can retry later or delete it.
            mCurrentReport.setState(State.WAIT_USER_INPUT);
            mCurrentReport.setPriority(0);
            mTaskMaster.getBugReportDAO().updateReportUploadInfo(
                    mCurrentReport, true);
            onUploadFinished(Result.INVALID);
            return;
        }

        // the state may already be TRANSMITTING in cases like user forces the
        // upload while it is TRANSMITTING
        if (mCurrentReport.getState() != State.TRANSMITTING) {
            mCurrentReport.setState(State.TRANSMITTING);
            mTaskMaster.getBugReportDAO().updateReportUploadInfo(
                    mCurrentReport, false);
        }

        int progressSize = Math.max((int) file.length() / 100,
                PROGRESS_UPLOAD_SIZE);
        GusData uploadData = new GusData(mCurrentReport.getLogPath(),
                Constants.BUGREPORT_INTENT_UPLOAD_PROGRESS, null, progressSize);
        GusJob job = new GusJob(uploadData, this);
        if (mCurrentReport.getUploadId() != null) { // The report has been tried
                                                    // to upload once before and
                                                    // got an uploadId, but
                                                    // failed.
            job.mUploadId = Integer.parseInt(mCurrentReport.getUploadId()); // use
                                                                            // the
                                                                            // original
                                                                            // upload
                                                                            // id
                                                                            // if
                                                                            // (showNotification(mTaskMaster,
                                                                            // mCurrentReport))
            // Notifications.showOnGoingNotification(mUploader,
            // mUploader.getString(R.string.bug_prompt)
            // + mCurrentReport.getTitle(),
            // mUploader.getString(R.string.resuming_upload),
            // (int) mCurrentReport.getId());
        } else {
            // show notification for contacting server to get an upload id
            // if (showNotification(mTaskMaster, mCurrentReport))
            // Notifications.showOnGoingNotification(mUploader,
            // mUploader.getString(R.string.bug_prompt)
            // + mCurrentReport.getTitle(),
            // mUploader.getString(R.string.contacting_server),
            // (int) mCurrentReport.getId());
        }
        // The GUS will upload the report asynchronously in a new thread
        try{
            if(reportInfo==null){
                reportInfo = JSONHelper.toCreateReportRequest(mUploader, mCurrentReport);
            }
        }catch (JSONException e) {
            Log.e(tag, "Failed to create reportInfo " + mCurrentReport, e);
            failwithException();
        }

        mCurrentJobId = mGus.start(job, reportInfo);
    }

    /**
     * This is a call back method called when the GUS login the server before
     * actual uploading
     */
    public void onLogonReturned(GusJob job, String session) {
        Log.d(tag, "onLogonReturned : " + session + ", " + mCurrentReport);
        if (mCurrentReport == null) {
            Log.d(tag, "No report is current uploading");
            return;
        }
    }

    /**
     * This is a call back method called when the upload fails or completes
     */
    public void done(GusJob job, ReturnCode code) {
        Log.d(tag, "done " + code.name() + ", " + mCurrentReport);
        try {
            if (job.mUploadId != null) {
                mCurrentReport.setUploadId(job.mUploadId.toString());
                mCurrentReport.setState(ComplainReport.State.TRANSMITTING);
            }
            switch (code) {
                case SUCCESS: // The log file has been successfully uploaded,
                              // send
                              // request to create a report record on the server
                    mCurrentReport.setState(State.READY_TO_COMPLETE);
                    /*
                     * Notifications.showUploadStopNotification(mUploader,
                     * mUploader.
                     * getString(R.string.bug_prompt)+mCurrentReport.getTitle(),
                     * mUploader.getString(R.string.complete_upload), (int)
                     * mCurrentReport.getId());
                     */
                    break;
                case UPLOAD_DELETED: // the upload job stops because user
                                     // removed it
                    mCurrentReport.setState(State.USER_DELETED_OUTBOX);
                    Notifications.cancel(mUploader, (int) mCurrentReport.getId());
                    break;
                case UPLOAD_PAUSED:
                    mCurrentReport.setState(State.READY_TO_TRANSMIT);
                    File file = new File(mCurrentReport.getLogPath());
                    if (showNotification(mTaskMaster, mCurrentReport))
                        Notifications.showUploadManualStopNotification(mUploader,
                                mUploader.getString(R.string.bug_prompt)
                                        + mCurrentReport.getTitle(),
                                mUploader.getString(R.string.transmit_paused),
                                (int) file.length(),
                                mCurrentReport.getUploadedBytes(),
                                mCurrentReport.getId());
                    break;
                case SERVER_ERROR:
                    mUploader.getApplicationContext().sendBroadcast(new Intent(Constants.BUGREPORT_INTENT_UPLOAD_PAUSED));
                    break;
                case BATTERY_LOW:
                case NETWORK_DISCONNECTED:
                case UNAUTHORIZED:
                case BAD_REQUEST:
                default:
                    mCurrentReport.setState(State.READY_TO_TRANSMIT);
                    // show failed notification
                    if (showNotification(mTaskMaster, mCurrentReport))
                        Notifications.showUploadErrorNotification(mUploader,
                                mUploader.getString(R.string.bug_prompt)
                                        + mCurrentReport.getTitle(),
                                R.string.upload_msg_failed,
                                (int) mCurrentReport.getId());
                    break;
            }
        } finally {
            mTaskMaster.getBugReportDAO().updateReportUploadInfo(
                    mCurrentReport, false);
            if (State.READY_TO_COMPLETE == mCurrentReport.getState()) {
                mCurrentReport.setState(State.READY_TO_ARCHIVE);
                mCurrentReport.setPriority(0);
                mTaskMaster.getBugReportDAO().updateReportUploadInfo(
                        mCurrentReport, true);
                Util.removeFiles(new String[] {
                        mCurrentReport.getLogPath(),
                        mCurrentReport.getScreenshotPath()
                });
                mCurrentReport.setState(State.ARCHIVED_FULL);
                mTaskMaster.getBugReportDAO().updateReportUploadInfo(
                        mCurrentReport, true);

                if (showNotification(mTaskMaster, mCurrentReport)) {
                    Notifications.showUploadManualStopNotification(
                            mUploader,
                            mUploader.getString(R.string.bug_prompt)
                                    + mCurrentReport.getTitle(),
                            mUploader.getString(R.string.complete_upload),
                            100, 100, mCurrentReport.getId());
                }
                onUploadFinished(Result.SUCCESSFUL);

            } else {
                onUploadFinished(Result.FAILED);
            }
        }
    }

    public synchronized void cancel(ReturnCode reason) {
        if (mCurrentReport == null) {
            Log.d(tag, "No uploading report can be canceled");
            return;
        }
        Log.d(tag, "cancel " + reason.name() + ", " + mCurrentReport);
        // If the report is being compressed, we need to wait for the completion
        // of the compression.
        if (State.COMPRESSING == mCurrentReport.getState()) {
            // if (showNotification(mTaskMaster, mCurrentReport))
            // Notifications.showOnGoingNotification(mUploader,
            // mUploader.getString(R.string.bug_prompt)
            // + mCurrentReport.getTitle(),
            // mUploader.getString(R.string.cancelling_upload),
            // (int) mCurrentReport.getId());
            mCurrentReport
                    .setState(reason == ReturnCode.UPLOAD_DELETED ? State.USER_DELETED_OUTBOX
                            : State.COMPRESSION_PAUSED);
            mTaskMaster.getBugReportDAO().updateReportUploadInfo(
                    mCurrentReport, false);
        } else if (mCurrentJobId != 0)
            mGus.stop(mCurrentJobId, reason);
    }

    public ComplainReport getCurrentUpload() {
        return mCurrentReport;
    }

    public boolean isBusy() {
        return mWakeLock.isHeld();
    }

    private void onUploadFinished(Result result) {
        if (mWakeLock.isHeld()) {
            mWakeLock.release();
            Log.d(tag, "Released wakelock");
        }
        mCurrentJobId = 0;
        mTaskMaster.getBugReportDAO().updateReportUploadInfo(mCurrentReport,
                false);
        mUploader.onUploadFinished(mCurrentReport, result);
    }

    public static boolean showNotification(TaskMaster taskMaster,
            ComplainReport report) {
        UserSettings settings = taskMaster.getConfigurationManager()
                .getUserSettings();
        boolean notificationSettings = settings.getDeamNotify().getValue();
        return report.isNotificationEnabled() && notificationSettings;
    }
}
